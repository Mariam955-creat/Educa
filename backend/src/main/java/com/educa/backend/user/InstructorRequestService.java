package com.educa.backend.user;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.educa.backend.common.error.ApiException;
import com.educa.backend.common.error.ConflictException;
import com.educa.backend.common.error.ResourceNotFoundException;
import com.educa.backend.config.EducaProperties;
import com.educa.backend.notification.EmailService;
import com.educa.backend.notification.NotificationService;
import com.educa.backend.notification.NotificationType;
import com.educa.backend.security.CurrentUser;
import com.educa.backend.storage.FileTypeDetector;
import com.educa.backend.storage.StorageService;
import com.educa.backend.user.InstructorRequest.Status;
import com.educa.backend.user.InstructorRequestDocument.Kind;
import com.educa.backend.user.dto.InstructorRequestDto;

/**
 * Demandes de rôle formateur : déposées avec une motivation et trois pièces (dernier diplôme, CV, pièce
 * d'identité), signalées aux administrateurs par une notification, puis acceptées ou refusées (avec un motif).
 * Le candidat est prévenu de la décision par une notification et par email.
 */
@Service
public class InstructorRequestService {

    /** Taille maximale de chaque pièce jointe. */
    static final long MAX_DOCUMENT_BYTES = 10L * 1024 * 1024;

    private static final Set<String> PDF_OR_IMAGE = Set.of("application/pdf", "image/png", "image/jpeg", "image/webp");
    private static final Set<String> CV_TYPES = Set.of("application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private static final Map<Kind, String> LABELS = Map.of(
            Kind.DIPLOMA, "dernier diplôme", Kind.CV, "CV", Kind.ID_DOCUMENT, "pièce d'identité");

    private final InstructorRequestRepository repository;
    private final InstructorRequestDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final StorageService storageService;
    private final FileTypeDetector fileTypeDetector;
    private final String publicBaseUrl;

    public InstructorRequestService(InstructorRequestRepository repository,
            InstructorRequestDocumentRepository documentRepository, UserRepository userRepository,
            RoleRepository roleRepository, NotificationService notificationService, EmailService emailService,
            StorageService storageService, FileTypeDetector fileTypeDetector, EducaProperties properties) {
        this.repository = repository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.storageService = storageService;
        this.fileTypeDetector = fileTypeDetector;
        this.publicBaseUrl = properties.publicBaseUrl();
    }

    /**
     * Dépose une demande. Refusée (409) si le compte est déjà formateur ou a une demande en attente ; (400) si la
     * motivation ou une pièce manque ; (413) si une pièce dépasse 10 Mo ; (415) si son type réel n'est pas accepté.
     * Après un refus, le candidat corrige et renvoie : une pièce non renvoyée reprend celle de la demande refusée.
     */
    @Transactional
    public InstructorRequestDto submit(Long userId, String motivation, MultipartFile diploma, MultipartFile cv,
            MultipartFile idDocument) {
        User user = loadUser(userId);
        if (user.getRoles().stream().anyMatch(r -> r.getName() != RoleName.LEARNER)) {
            throw new ConflictException("Ce compte a déjà le rôle de formateur");
        }
        if (repository.existsByUserIdAndStatus(userId, Status.PENDING)) {
            throw new ConflictException("Une demande est déjà en attente");
        }
        if (!StringUtils.hasText(motivation)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La motivation est obligatoire");
        }
        if (motivation.trim().length() > 2000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La motivation dépasse 2000 caractères");
        }
        // Toutes les pièces sont vérifiées avant d'enregistrer quoi que ce soit
        Map<Kind, InstructorRequestDocument> previous = previousDocuments(userId);
        Map<Kind, MultipartFile> files = new EnumMap<>(Kind.class);
        files.put(Kind.DIPLOMA, diploma);
        files.put(Kind.CV, cv);
        files.put(Kind.ID_DOCUMENT, idDocument);
        files.values().removeIf(file -> file == null || file.isEmpty());
        for (Kind kind : Kind.values()) {
            if (!files.containsKey(kind) && !previous.containsKey(kind)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Pièce manquante : " + LABELS.get(kind));
            }
        }
        Map<Kind, String> detectedTypes = new EnumMap<>(Kind.class);
        files.forEach((kind, file) -> detectedTypes.put(kind, validate(kind, file)));

        InstructorRequest request = new InstructorRequest();
        request.setUserId(userId);
        request.setMotivation(motivation.trim());
        repository.save(request);

        files.forEach((kind, file) -> {
            InstructorRequestDocument document = new InstructorRequestDocument();
            document.setRequestId(request.getId());
            document.setKind(kind);
            document.setStorageKey(storageService.store(file, "instructor-requests/" + request.getId()));
            document.setOriginalName(originalName(file));
            document.setMimeType(detectedTypes.get(kind));
            document.setSizeBytes(file.getSize());
            documentRepository.save(document);
        });
        // Pièces reprises de la demande refusée : même fichier stocké, nouvelle ligne pour cette demande
        previous.forEach((kind, old) -> {
            if (files.containsKey(kind)) {
                return;
            }
            InstructorRequestDocument document = new InstructorRequestDocument();
            document.setRequestId(request.getId());
            document.setKind(kind);
            document.setStorageKey(old.getStorageKey());
            document.setOriginalName(old.getOriginalName());
            document.setMimeType(old.getMimeType());
            document.setSizeBytes(old.getSizeBytes());
            documentRepository.save(document);
        });

        for (User admin : userRepository.findByRoles_NameAndEnabledTrueAndDeletedAtIsNull(RoleName.ADMIN)) {
            notificationService.notify(admin.getId(), NotificationType.INSTRUCTOR_REQUEST_SUBMITTED,
                    user.getFullName(), "/admin/instructor-requests");
        }
        return toDtos(List.of(request)).get(0);
    }

    /** Pièces de la dernière demande du compte si elle a été refusée (réutilisables au renvoi), vide sinon. */
    private Map<Kind, InstructorRequestDocument> previousDocuments(Long userId) {
        Map<Kind, InstructorRequestDocument> previous = new EnumMap<>(Kind.class);
        repository.findFirstByUserIdOrderByCreatedAtDescIdDesc(userId)
                .filter(last -> last.getStatus() == Status.REJECTED)
                .ifPresent(last -> documentRepository.findByRequestIdInOrderByIdAsc(List.of(last.getId()))
                        .forEach(d -> previous.put(d.getKind(), d)));
        return previous;
    }

    /** Dernière demande du compte (pour suivre son état), vide s'il n'en a jamais fait. */
    @Transactional(readOnly = true)
    public Optional<InstructorRequestDto> latest(Long userId) {
        return repository.findFirstByUserIdOrderByCreatedAtDescIdDesc(userId).map(r -> toDtos(List.of(r)).get(0));
    }

    @Transactional(readOnly = true)
    public Page<InstructorRequestDto> list(Status status, Pageable pageable) {
        Page<InstructorRequest> page = repository.findByStatus(status, pageable);
        List<InstructorRequestDto> dtos = toDtos(page.getContent());
        return page.map(r -> dtos.get(page.getContent().indexOf(r)));
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return repository.countByStatus(Status.PENDING);
    }

    /** Pièce jointe d'une demande, pour l'administration. */
    @Transactional(readOnly = true)
    public DocumentFile document(Long requestId, Long documentId) {
        InstructorRequestDocument document = documentRepository.findByIdAndRequestId(documentId, requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Pièce jointe introuvable"));
        return new DocumentFile(storageService.loadAsResource(document.getStorageKey()), document.getOriginalName(),
                document.getMimeType());
    }

    public record DocumentFile(Resource resource, String fileName, String mimeType) {
    }

    /** Accepte la demande : ajoute le rôle formateur (le rôle apprenant est conservé), notifie et écrit au candidat. */
    @Transactional
    public InstructorRequestDto approve(Long requestId) {
        InstructorRequest request = loadPending(requestId);
        User user = loadUser(request.getUserId());
        user.addRole(roleRepository.findByName(RoleName.INSTRUCTOR)
                .orElseThrow(() -> new IllegalStateException("Rôle INSTRUCTOR absent — vérifier les migrations")));
        decide(request, Status.APPROVED, null);
        notificationService.notify(user.getId(), NotificationType.INSTRUCTOR_REQUEST_APPROVED, null, "/instructor");
        afterCommit(() -> emailService.send(user.getEmail(), user.getPreferredLanguage(), "instructorApproved",
                user.getFullName(), publicBaseUrl + "/instructor"));
        return toDtos(List.of(request)).get(0);
    }

    /** Refuse la demande : le compte reste apprenant ; le motif est transmis par notification et par email. */
    @Transactional
    public InstructorRequestDto reject(Long requestId, String reason) {
        InstructorRequest request = loadPending(requestId);
        User user = loadUser(request.getUserId());
        String motive = reason.trim();
        decide(request, Status.REJECTED, motive);
        notificationService.notify(user.getId(), NotificationType.INSTRUCTOR_REQUEST_REJECTED, motive,
                "/account/profile");
        afterCommit(() -> emailService.send(user.getEmail(), user.getPreferredLanguage(), "instructorRejected",
                user.getFullName(), motive, publicBaseUrl + "/account/profile"));
        return toDtos(List.of(request)).get(0);
    }

    /** Rôle formateur (ou admin) accordé à la main : une demande en attente est close comme acceptée. */
    @Transactional
    public void closePendingAsApproved(Long userId) {
        repository.findByUserIdAndStatus(userId, Status.PENDING)
                .ifPresent(request -> decide(request, Status.APPROVED, null));
    }

    private String validate(Kind kind, MultipartFile file) {
        if (file.getSize() > MAX_DOCUMENT_BYTES) {
            throw new ApiException(HttpStatus.CONTENT_TOO_LARGE,
                    "Fichier trop volumineux (" + LABELS.get(kind) + ", 10 Mo maximum)");
        }
        String detected = fileTypeDetector.detect(file);
        Set<String> allowed = kind == Kind.CV ? CV_TYPES : PDF_OR_IMAGE;
        if (!allowed.contains(detected)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Type de fichier refusé pour " + LABELS.get(kind) + " : " + detected);
        }
        return detected;
    }

    private static String originalName(MultipartFile file) {
        String name = StringUtils.getFilename(file.getOriginalFilename());
        if (!StringUtils.hasText(name)) {
            return "document";
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    /** L'email ne part qu'une fois la décision enregistrée (jamais pour une transaction annulée). */
    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private void decide(InstructorRequest request, Status status, String reason) {
        request.setStatus(status);
        request.setRejectionReason(reason);
        request.setDecidedAt(Instant.now());
        request.setDecidedBy(CurrentUser.optionalId());
    }

    private InstructorRequest loadPending(Long requestId) {
        InstructorRequest request = repository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));
        if (request.getStatus() != Status.PENDING) {
            throw new ConflictException("Cette demande a déjà été traitée");
        }
        return request;
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    /** Convertit des demandes en DTO, avec leurs auteurs et leurs pièces chargés en deux requêtes. */
    private List<InstructorRequestDto> toDtos(List<InstructorRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }
        Map<Long, User> users = userRepository.findAllById(
                requests.stream().map(InstructorRequest::getUserId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, List<InstructorRequestDto.Document>> documents = documentRepository
                .findByRequestIdInOrderByIdAsc(requests.stream().map(InstructorRequest::getId).toList()).stream()
                .collect(Collectors.groupingBy(InstructorRequestDocument::getRequestId,
                        Collectors.mapping(d -> new InstructorRequestDto.Document(d.getId(), d.getKind().name(),
                                d.getOriginalName(), d.getMimeType(), d.getSizeBytes()), Collectors.toList())));
        return requests.stream().map(r -> {
            User user = users.get(r.getUserId());
            return new InstructorRequestDto(r.getId(), r.getUserId(),
                    user != null ? user.getFullName() : null,
                    user != null ? user.getEmail() : null,
                    user != null ? user.getHeadline() : null,
                    r.getMotivation(), r.getStatus().name(), r.getRejectionReason(),
                    r.getCreatedAt(), r.getDecidedAt(), documents.getOrDefault(r.getId(), List.of()));
        }).toList();
    }
}
