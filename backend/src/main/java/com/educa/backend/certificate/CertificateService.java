package com.educa.backend.certificate;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Year;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.certificate.dto.CertificateDto;
import com.educa.backend.certificate.dto.CertificateVerificationDto;
import com.educa.backend.common.error.ApiException;
import com.educa.backend.common.error.ResourceNotFoundException;
import com.educa.backend.common.pdf.PdfDocuments;
import com.educa.backend.config.EducaProperties;
import com.educa.backend.course.CourseService;
import com.educa.backend.user.UserService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

@Service
public class CertificateService {

    private static final Logger log = LoggerFactory.getLogger(CertificateService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CertificateRepository certificateRepository;
    private final UserService userService;
    private final CourseService courseService;
    private final PdfDocuments pdfDocuments;
    private final EducaProperties educaProperties;

    public CertificateService(CertificateRepository certificateRepository, UserService userService,
                              CourseService courseService, PdfDocuments pdfDocuments,
                              EducaProperties educaProperties) {
        this.certificateRepository = certificateRepository;
        this.userService = userService;
        this.courseService = courseService;
        this.pdfDocuments = pdfDocuments;
        this.educaProperties = educaProperties;
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

    /** Registre de tous les certificats délivrés (administration). */
    @Transactional(readOnly = true)
    public Page<CertificateDto> registry(Pageable pageable) {
        return certificateRepository.findAllByOrderByIssuedAtDesc(pageable).map(this::toDto);
    }

    /**
     * PDF généré à chaque téléchargement (quelques ko, rendu rapide) dans la langue demandée — celle de
     * l'interface du demandeur — ou, à défaut, la langue de préférence du titulaire. Pas de cache : un cache
     * unique figerait la langue du premier téléchargement.
     */
    @Transactional(readOnly = true)
    public Resource download(Long certificateId, Long requesterId, boolean isAdmin, String lang) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificat introuvable"));
        if (!isAdmin && !certificate.getUserId().equals(requesterId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Ce certificat ne vous appartient pas");
        }
        String resolvedLang = pdfDocuments.resolveLang(lang,
                userService.getById(certificate.getUserId()).preferredLanguage());
        String holderName = userService.displayNameById(certificate.getUserId());
        String courseTitle = courseService.summary(certificate.getCourseId()).title();
        try {
            return new ByteArrayResource(renderPdf(certificate, holderName, courseTitle, resolvedLang));
        } catch (RuntimeException e) {
            log.error("Génération du PDF du certificat {} échouée", certificate.getId(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF du certificat indisponible");
        }
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
        // Un cours sans contrôle (seulement un examen final) donne une moyenne des
        // contrôles nulle ; la colonne est NOT NULL, donc on la ramène à zéro.
        certificate.setControlsAverage(controlsAverage == null ? BigDecimal.ZERO : controlsAverage);
        certificate.setFinalExamScore(finalExamScore);
        certificate.setFinalGrade(finalGrade);
        certificate.setVerificationCode(randomHex());
        certificate.setSerialNumber("EDUCA-" + Year.now().getValue() + "-" + String.format("%06d", sequence));
        certificateRepository.save(certificate);
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

    private byte[] renderPdf(Certificate c, String holderName, String courseTitle, String lang) {
        String verificationUrl = educaProperties.publicBaseUrl() + "/verify/" + c.getVerificationCode();
        String qrCodeImg = qrCodeDataUri(verificationUrl)
                .map(dataUri -> "<img class=\"qr\" src=\"" + dataUri + "\"/>")
                .orElse("");

        String html = """
                <html lang="%s"><head><meta charset="utf-8"/><style>
                  @page { size: A4 landscape; margin: 0; }
                  body { font-family: '%s'; color: #1f2937; }
                  .frame { margin: 28px; border: 3px solid #7e22ce; border-radius: 10px;
                           padding: 60px 70px; text-align: center; position: relative; }
                  h1 { font-size: 34px; letter-spacing: 2px; color: #7e22ce; margin: 0 0 8px; }
                  .sub { color: #6b7280; margin: 0 0 40px; }
                  .name { font-size: 30px; font-weight: bold; margin: 24px 0 6px; }
                  .course { font-size: 20px; font-weight: bold; margin: 0 0 28px; }
                  .grade { font-size: 18px; }
                  .meta { margin-top: 40px; color: #6b7280; font-size: 12px; }
                  .qr { position: absolute; bottom: 24px; right: 32px; width: 84px; height: 84px; }
                </style></head><body>
                <div class="frame">
                  <h1>%s</h1>
                  <p class="sub">%s</p>
                  <p>%s</p>
                  <p class="name">%s</p>
                  <p>%s</p>
                  <p class="course">%s</p>
                  <p class="grade">%s</p>
                  <p class="meta">%s<br/>%s</p>
                  %s
                </div>
                </body></html>
                """.formatted(lang, PdfDocuments.FONT_FAMILY,
                pdfDocuments.text("cert.title", lang), pdfDocuments.text("cert.platform", lang),
                pdfDocuments.text("cert.attests", lang), PdfDocuments.escape(holderName),
                pdfDocuments.text("cert.completed", lang), PdfDocuments.escape(courseTitle),
                pdfDocuments.text("cert.grade", lang, plain(c.getFinalGrade()), plain(c.getControlsAverage()),
                        plain(c.getFinalExamScore())),
                pdfDocuments.text("cert.meta", lang, c.getSerialNumber(), pdfDocuments.date(c.getIssuedAt(), lang)),
                pdfDocuments.text("cert.verification", lang, c.getVerificationCode()), qrCodeImg);

        return pdfDocuments.render(html);
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    /** QR code pointant vers la page publique de vérification, encodé en data URI PNG pour l'embarquer dans le PDF. */
    private static Optional<String> qrCodeDataUri(String content) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 200, 200, hints);

            BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return Optional.of("data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray()));
        } catch (WriterException | IOException e) {
            log.warn("Génération du QR code de vérification échouée, certificat généré sans QR code", e);
            return Optional.empty();
        }
    }
}
