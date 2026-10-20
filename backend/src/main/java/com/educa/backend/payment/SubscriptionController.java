package com.educa.backend.payment;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.payment.dto.CheckoutRequest;
import com.educa.backend.payment.dto.CheckoutResponse;
import com.educa.backend.payment.dto.SubscriptionDto;
import com.educa.backend.security.CurrentUser;

import jakarta.validation.Valid;

/** Abonnement mensuel « accès à tous les cours » (pass de 30 jours renouvelable). */
@RestController
@RequestMapping("/api/v1/subscription")
public class SubscriptionController {

    private final PaymentService paymentService;

    public SubscriptionController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** Public : prix et durée de l'offre, avec l'état de l'abonnement si l'utilisateur est connecté. */
    @GetMapping
    public SubscriptionDto subscription() {
        return paymentService.subscription(CurrentUser.optionalId());
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        return paymentService.startSubscriptionCheckout(CurrentUser.id(), request);
    }
}
