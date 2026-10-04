package com.educa.backend.payment;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.payment.dto.InstructorSaleDto;
import com.educa.backend.security.CurrentUser;

/** Ventes des cours du formateur connecté (espace formateur : revenus). */
@RestController
@RequestMapping("/api/v1/instructor/sales")
@PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
public class InstructorSalesController {

    private final PaymentService paymentService;

    public InstructorSalesController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<InstructorSaleDto> mySales() {
        return paymentService.instructorSales(CurrentUser.id());
    }
}
