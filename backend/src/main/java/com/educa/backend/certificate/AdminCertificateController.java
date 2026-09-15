package com.educa.backend.certificate;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.certificate.dto.CertificateDto;
import com.educa.backend.common.web.PageResponse;

/** Registre de tous les certificats délivrés (administration). */
@RestController
@RequestMapping("/api/v1/admin/certificates")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCertificateController {

    private final CertificateService certificateService;

    public AdminCertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping
    public PageResponse<CertificateDto> registry(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageResponse.of(certificateService.registry(PageRequest.of(Math.max(page, 0), safeSize)));
    }
}
