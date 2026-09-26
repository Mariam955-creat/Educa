package com.educa.backend.certificate;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.certificate.dto.CertificateDto;
import com.educa.backend.certificate.dto.CertificateVerificationDto;
import com.educa.backend.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/certificates")
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping("/me")
    public List<CertificateDto> myCertificates() {
        return certificateService.myCertificates(CurrentUser.id());
    }

    /** {@code lang} : langue du PDF (celle de l'interface) ; absente ou non supportée → langue du titulaire. */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, @RequestParam(required = false) String lang) {
        Resource pdf = certificateService.download(id, CurrentUser.id(), CurrentUser.isAdmin(), lang);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"certificat-" + id + ".pdf\"")
                .body(pdf);
    }

    /** Vérification publique par code (voir SecurityConfig : permitAll). */
    @GetMapping("/verify/{code}")
    public CertificateVerificationDto verify(@PathVariable String code) {
        return certificateService.verify(code);
    }
}
