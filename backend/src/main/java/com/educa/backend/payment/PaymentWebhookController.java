package com.educa.backend.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Callbacks serveur-à-serveur des prestataires — public (voir SecurityConfig : permitAll),
 * authentifiés par signature (Stripe) ou par revérification côté serveur (Orange Money),
 * jamais par JWT.
 */
@RestController
@RequestMapping("/api/v1/payments/webhooks")
public class PaymentWebhookController {

    private final SubscriptionService subscriptionService;

    public PaymentWebhookController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<Void> stripe(@RequestBody String payload,
                                       @RequestHeader("Stripe-Signature") String signature) {
        subscriptionService.handleStripeWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/orange-money")
    public ResponseEntity<Void> orangeMoney(@RequestParam("order_id") String orderId,
                                            @RequestParam("amount") long amount,
                                            @RequestParam("pay_token") String payToken) {
        subscriptionService.handleOrangeMoneyWebhook(orderId, amount, payToken);
        return ResponseEntity.ok().build();
    }
}
