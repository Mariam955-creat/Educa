package com.educa.backend.user;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.educa.backend.common.error.ConflictException;
import com.educa.backend.common.error.ResourceNotFoundException;
import com.educa.backend.notification.NotificationService;
import com.educa.backend.notification.NotificationType;
import com.educa.backend.security.CurrentUser;
import com.educa.backend.user.InstructorRequest.Status;
import com.educa.backend.user.dto.InstructorRequestDto;

/**
 * Demandes de rôle formateur : faites à l'inscription ou depuis l'espace compte, acceptées ou refusées (avec un
 * motif) par l'administration. Le candidat est prévenu par une notification.
 */
@Service
public class InstructorRequestService {

    private final InstructorRequestRepository repository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final NotificationService notificationService;

    public InstructorRequestService(InstructorRequestRepository repository, UserRepository userRepository,
            RoleRepository roleRepository, NotificationService notificationService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.notificationService = notificationService;
    }

    /** Dépose une demande ; refusée (409) si le compte est déjà formateur ou a déjà une demande en attente. */
    @Transactional
    public InstructorRequestDto submit(Long userId, String motivation) {
        User user = loadUser(userId);
        if (user.getRoles().stream().anyMatch(r -> r.getName() != RoleName.LEARNER)) {
            throw new ConflictException("Ce compte a déjà le rôle de formateur");
        }
        if (repository.existsByUserIdAndStatus(userId, Status.PENDING)) {
            throw new ConflictException("Une demande est déjà en attente");
        }
        InstructorRequest request = new InstructorRequest();
        request.setUserId(userId);
        request.setMotivation(StringUtils.hasText(motivation) ? motivation.trim() : null);
        return toDto(repository.save(request), user);
    }

    /** Dernière demande du compte (pour suivre son état), vide s'il n'en a jamais fait. */
    @Transactional(readOnly = true)
    public Optional<InstructorRequestDto> latest(Long userId) {
        return repository.findFirstByUserIdOrderByCreatedAtDescIdDesc(userId)
                .map(request -> toDto(request, loadUser(userId)));
    }

    @Transactional(readOnly = true)
    public Page<InstructorRequestDto> list(Status status, Pageable pageable) {
        return repository.findByStatus(status, pageable)
                .map(request -> toDto(request, userRepository.findById(request.getUserId()).orElse(null)));
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return repository.countByStatus(Status.PENDING);
    }

    /** Accepte la demande : ajoute le rôle formateur (le rôle apprenant est conservé) et prévient le candidat. */
    @Transactional
    public InstructorRequestDto approve(Long requestId) {
        InstructorRequest request = loadPending(requestId);
        User user = loadUser(request.getUserId());
        user.addRole(roleRepository.findByName(RoleName.INSTRUCTOR)
                .orElseThrow(() -> new IllegalStateException("Rôle INSTRUCTOR absent — vérifier les migrations")));
        decide(request, Status.APPROVED, null);
        notificationService.notify(user.getId(), NotificationType.INSTRUCTOR_REQUEST_APPROVED, null, "/instructor");
        return toDto(request, user);
    }

    /** Refuse la demande : le compte reste apprenant, le motif est transmis au candidat. */
    @Transactional
    public InstructorRequestDto reject(Long requestId, String reason) {
        InstructorRequest request = loadPending(requestId);
        User user = loadUser(request.getUserId());
        decide(request, Status.REJECTED, reason.trim());
        notificationService.notify(user.getId(), NotificationType.INSTRUCTOR_REQUEST_REJECTED, reason.trim(),
                "/account/overview");
        return toDto(request, user);
    }

    /** Rôle formateur (ou admin) accordé à la main : une demande en attente est close comme acceptée. */
    @Transactional
    public void closePendingAsApproved(Long userId) {
        repository.findByUserIdAndStatus(userId, Status.PENDING)
                .ifPresent(request -> decide(request, Status.APPROVED, null));
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

    private static InstructorRequestDto toDto(InstructorRequest request, User user) {
        return new InstructorRequestDto(request.getId(), request.getUserId(),
                user != null ? user.getFullName() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getHeadline() : null,
                request.getMotivation(), request.getStatus().name(), request.getRejectionReason(),
                request.getCreatedAt(), request.getDecidedAt());
    }
}
