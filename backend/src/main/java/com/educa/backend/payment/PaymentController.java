package com.educa.backend.payment;

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

import com.educa.backend.payment.dto.InvoiceDto;
import com.educa.backend.security.CurrentUser;

/** Factures de l'apprenant connecté (un paiement réussi = une facture). */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/me")
    public List<InvoiceDto> myInvoices() {
        return paymentService.myInvoices(CurrentUser.id());
    }

    /** {@code lang} : langue du PDF (celle de l'interface) ; absente ou non supportée → langue du titulaire. */
    @GetMapping("/{id}/invoice/download")
    public ResponseEntity<Resource> downloadInvoice(@PathVariable Long id, @RequestParam(required = false) String lang) {
        Resource pdf = paymentService.downloadInvoice(id, CurrentUser.id(), CurrentUser.isAdmin(), lang);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"facture-" + id + ".pdf\"")
                .body(pdf);
    }
}
