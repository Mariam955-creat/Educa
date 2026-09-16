package com.educa.backend.payment;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.payment.dto.CheckoutRequest;
import com.educa.backend.payment.dto.CheckoutResponse;
import com.educa.backend.payment.dto.SubscriptionDto;
import com.educa.backend.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        return subscriptionService.startCheckout(CurrentUser.id(), request);
    }

    @GetMapping("/me")
    public SubscriptionDto me() {
        return subscriptionService.mySubscription(CurrentUser.id());
    }

    @PostMapping("/cancel")
    public SubscriptionDto cancel() {
        subscriptionService.cancel(CurrentUser.id());
        return subscriptionService.mySubscription(CurrentUser.id());
    }
}
