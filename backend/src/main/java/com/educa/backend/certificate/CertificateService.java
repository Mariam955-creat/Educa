package com.educa.backend.certificate;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Year;
import java.util.HexFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.certificate.dto.CertificateDto;
import com.educa.backend.certificate.dto.CertificateVerificationDto;
import com.educa.backend.common.error.ApiException;
import com.educa.backend.common.error.ResourceNotFoundException;
import com.educa.backend.course.CourseService;
import com.educa.backend.storage.StorageService;
import com.educa.backend.user.UserService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

@Service
public class CertificateService {

    private static final Logger log = LoggerFactory.getLogger(CertificateService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CertificateRepository certificateRepository;
    private final UserService userService;
    private final CourseService courseService;
    private final StorageService storageService;

    public CertificateService(CertificateRepository certificateRepository, UserService userService,
                              CourseService courseService, StorageService storageService) {
        this.certificateRepository = certificateRepository;
        this.userService = userService;
        this.courseService = courseService;
        this.storageService = storageService;
    }

    /** Crée le certificat si absent pour ce couple (utilisateur, cours). Renvoie l'id du certificat. */
    @Transactional
    public Long issueIfAbsent(Long userId, Long courseId, Long finalExamAttemptId,
                              BigDecimal controlsAverage, BigDecimal finalExamScore, BigDecimal finalGrade) {
        return certificateRepository.findByUserIdAndCourseId(userId, courseId)
                .map(Certificate::getId)
                .orElseGet(() -> create(userId, courseId, finalExamAttemptId, controlsAverage, finalExamScore, finalGrade));
    }

    @Transactional(readOnly = true)
    public Long certificateIdFor(Long userId, Long courseId) {
        return certificateRepository.findByUserIdAndCourseId(userId, courseId)
                .map(Certificate::getId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<CertificateDto> myCertificates(Long userId) {
        return certificateRepository.findByUserIdOrderByIssuedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public Resource download(Long certificateId, Long requesterId, boolean isAdmin) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificat introuvable"));
        if (!isAdmin && !certificate.getUserId().equals(requesterId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Ce certificat ne vous appartient pas");
        }
        if (certificate.getPdfKey() == null) {
            // Génération paresseuse (ou re-tentative si l'émission avait échoué).
            String holderName = userService.displayNameById(certificate.getUserId());
            String courseTitle = courseService.summary(certificate.getCourseId()).title();
            try {
                byte[] pdf = renderPdf(certificate, holderName, courseTitle);
                certificate.setPdfKey(storageService.store(pdf, "certificates/" + certificate.getId(), "pdf"));
            } catch (Exception e) {
                log.error("Génération du PDF du certificat {} échouée", certificate.getId(), e);
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF du certificat indisponible");
            }
        }
        return storageService.loadAsResource(certificate.getPdfKey());
    }

    @Transactional(readOnly = true)
    public CertificateVerificationDto verify(String code) {
        return certificateRepository.findByVerificationCode(code)
                .map(c -> new CertificateVerificationDto(true, c.getSerialNumber(),
                        userService.displayNameById(c.getUserId()),
                        courseService.summary(c.getCourseId()).title(), c.getFinalGrade(), c.getIssuedAt()))
                .orElseGet(CertificateVerificationDto::invalid);
    }

    // ---------- privé ----------

    private Long create(Long userId, Long courseId, Long finalExamAttemptId,
                        BigDecimal controlsAverage, BigDecimal finalExamScore, BigDecimal finalGrade) {
        long sequence = certificateRepository.count() + 1;

        Certificate certificate = new Certificate();
        certificate.setUserId(userId);
        certificate.setCourseId(courseId);
        certificate.setFinalExamAttemptId(finalExamAttemptId);
        certificate.setControlsAverage(controlsAverage);
        certificate.setFinalExamScore(finalExamScore);
        certificate.setFinalGrade(finalGrade);
        certificate.setVerificationCode(randomHex());
        certificate.setSerialNumber("EDUCA-" + Year.now().getValue() + "-" + String.format("%06d", sequence));
        certificateRepository.save(certificate);

        String holderName = userService.displayNameById(userId);
        String courseTitle = courseService.summary(courseId).title();
        try {
            byte[] pdf = renderPdf(certificate, holderName, courseTitle);
            certificate.setPdfKey(storageService.store(pdf, "certificates/" + certificate.getId(), "pdf"));
        } catch (Exception e) {
            log.error("Génération du PDF du certificat {} échouée", certificate.getId(), e);
        }
        return certificate.getId();
    }

    private CertificateDto toDto(Certificate c) {
        return new CertificateDto(c.getId(), c.getSerialNumber(), c.getVerificationCode(), c.getCourseId(),
                courseService.summary(c.getCourseId()).title(), userService.displayNameById(c.getUserId()),
                c.getControlsAverage(), c.getFinalExamScore(), c.getFinalGrade(), c.getIssuedAt());
    }

    private static String randomHex() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private byte[] renderPdf(Certificate c, String holderName, String courseTitle) throws Exception {
        String html = """
                <html><head><meta charset="utf-8"/><style>
                  @page { size: A4 landscape; margin: 0; }
                  body { font-family: sans-serif; color: #1f2937; }
                  .frame { margin: 28px; border: 3px solid #2563eb; border-radius: 10px;
                           padding: 60px 70px; text-align: center; }
                  h1 { font-size: 34px; letter-spacing: 2px; color: #2563eb; margin: 0 0 8px; }
                  .sub { color: #6b7280; margin: 0 0 40px; }
                  .name { font-size: 30px; font-weight: bold; margin: 24px 0 6px; }
                  .course { font-size: 20px; margin: 0 0 28px; }
                  .grade { font-size: 18px; }
                  .meta { margin-top: 40px; color: #6b7280; font-size: 12px; }
                </style></head><body>
                <div class="frame">
                  <h1>CERTIFICAT DE R&#201;USSITE</h1>
                  <p class="sub">Plateforme e-learning educa</p>
                  <p>Ce certificat atteste que</p>
                  <p class="name">%s</p>
                  <p>a valid&#233; avec succ&#232;s la formation</p>
                  <p class="course">&#171; %s &#187;</p>
                  <p class="grade">Note finale : <b>%s / 100</b>
                     (contr&#244;les : %s &#183; examen final : %s)</p>
                  <p class="meta">N&#176; %s &#183; d&#233;livr&#233; le %s<br/>
                     V&#233;rification : code %s</p>
                </div>
                </body></html>
                """.formatted(escape(holderName), escape(courseTitle),
                c.getFinalGrade().stripTrailingZeros().toPlainString(),
                c.getControlsAverage().stripTrailingZeros().toPlainString(),
                c.getFinalExamScore().stripTrailingZeros().toPlainString(),
                c.getSerialNumber(), c.getIssuedAt(), c.getVerificationCode());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        }
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
