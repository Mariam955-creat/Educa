package com.educa.backend.payment;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.payment.dto.CheckoutRequest;
import com.educa.backend.payment.dto.CheckoutResponse;
import com.educa.backend.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/courses/{courseId}")
public class CourseCheckoutController {

    private final PaymentService paymentService;

    public CourseCheckoutController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@PathVariable Long courseId, @Valid @RequestBody CheckoutRequest request) {
        return paymentService.startCheckout(CurrentUser.id(), courseId, request);
    }
}
