/**
 * Module <b>payment</b> — achat individuel d'un cours (pas d'abonnement plateforme), deux
 * prestataires : {@code StripePaymentGateway} (Europe) et
 * {@code OrangeMoneyPaymentGateway} (Afrique, intégration directe). Interface
 * {@code PaymentGateway}, repli "désactivé" si clés absentes (voir module {@code ai}).
 */
package com.educa.backend.payment;
