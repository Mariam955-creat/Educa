package com.educa.backend.payment;

import java.io.ByteArrayOutputStream;
import java.time.Year;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.common.error.ApiException;
import com.educa.backend.common.error.ResourceNotFoundException;
import com.educa.backend.course.Course;
import com.educa.backend.course.CourseService;
import com.educa.backend.payment.dto.AdminPaymentDto;
import com.educa.backend.payment.dto.CheckoutRequest;
import com.educa.backend.payment.dto.CheckoutResponse;
import com.educa.backend.payment.dto.InvoiceDto;
import com.educa.backend.storage.StorageService;
import com.educa.backend.user.UserService;
import com.educa.backend.user.dto.UserDto;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final UserService userService;
    private final CourseService courseService;
    private final StorageService storageService;
    private final PaymentGateway stripeGateway;
    private final PaymentGateway orangeMoneyGateway;

    public PaymentService(PaymentRepository paymentRepository, UserService userService, CourseService courseService,
                          StorageService storageService,
                          @Qualifier("stripeGateway") PaymentGateway stripeGateway,
                          @Qualifier("orangeMoneyGateway") PaymentGateway orangeMoneyGateway) {
        this.paymentRepository = paymentRepository;
        this.userService = userService;
        this.courseService = courseService;
        this.storageService = storageService;
        this.stripeGateway = stripeGateway;
        this.orangeMoneyGateway = orangeMoneyGateway;
    }

    @Transactional
    public CheckoutResponse startCheckout(Long userId, Long courseId, CheckoutRequest request) {
        UserDto user = userService.getById(userId);
        Course course = courseService.requireCourse(courseId);
        if (!course.isPublished()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ce cours n'est pas publié");
        }
        if (course.getPrice().signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ce cours est gratuit, inscris-toi directement");
        }

        PaymentGateway.CheckoutResult result = gatewayFor(request.provider())
                .startCheckout(userId, user.email(), courseId, course.getSlug(), course.getTitle(), course.getPrice());

        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setCourseId(courseId);
        payment.setProvider(request.provider());
        payment.setProviderReference(result.providerReference());
        payment.setAmount(result.amount());
        payment.setCurrency(result.currency());
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        return new CheckoutResponse(result.checkoutUrl());
    }

    /** Apprenants : exige un paiement réussi pour ce cours précis. Formateurs/admins : toujours autorisés. */
    @Transactional(readOnly = true)
    public boolean hasSucceededPayment(Long userId, Long courseId) {
        UserDto user = userService.getById(userId);
        if (user.roles().contains("INSTRUCTOR") || user.roles().contains("ADMIN")) {
            return true;
        }
        return paymentRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, PaymentStatus.SUCCEEDED);
    }

    @Transactional(readOnly = true)
    public Page<AdminPaymentDto> registry(Pageable pageable) {
        return paymentRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(p -> new AdminPaymentDto(p.getId(), userService.displayNameById(p.getUserId()),
                        courseService.summary(p.getCourseId()).title(), p.getProvider(), p.getAmount(),
                        p.getCurrency(), p.getStatus(), p.getInvoiceNumber(), p.getCreatedAt()));
    }

    /** Factures de l'apprenant connecté : un paiement réussi = une facture. */
    @Transactional(readOnly = true)
    public List<InvoiceDto> myInvoices(Long userId) {
        return paymentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, PaymentStatus.SUCCEEDED).stream()
                .map(p -> new InvoiceDto(p.getId(), p.getInvoiceNumber(),
                        courseService.summary(p.getCourseId()).title(), p.getProvider(), p.getAmount(),
                        p.getCurrency(), p.getStatus(), p.getCreatedAt()))
                .toList();
    }

    /** PDF de facture, généré paresseusement au premier téléchargement (même patron que CertificateService). */
    @Transactional
    public Resource downloadInvoice(Long paymentId, Long requesterId, boolean isAdmin) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable"));
        if (!isAdmin && !payment.getUserId().equals(requesterId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cette facture ne vous appartient pas");
        }
        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Aucune facture pour un paiement non abouti");
        }
        if (payment.getPdfKey() == null) {
            String buyerName = userService.displayNameById(payment.getUserId());
            String courseTitle = courseService.summary(payment.getCourseId()).title();
            try {
                byte[] pdf = renderInvoicePdf(payment, buyerName, courseTitle);
                payment.setPdfKey(storageService.store(pdf, "invoices/" + payment.getId(), "pdf"));
                paymentRepository.save(payment);
            } catch (Exception e) {
                log.error("Génération du PDF de la facture {} échouée", payment.getId(), e);
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Facture indisponible");
            }
        }
        return storageService.loadAsResource(payment.getPdfKey());
    }

    // ---------- webhooks ----------

    @Transactional
    public void handleStripeWebhook(String payload, String sigHeader) {
        if (!(stripeGateway instanceof StripePaymentGateway stripe)) {
            log.warn("Webhook Stripe reçu mais Stripe n'est pas configuré, ignoré");
            return;
        }
        StripePaymentGateway.StripeEventResult result = stripe.parseWebhook(payload, sigHeader);
        if ("checkout.session.completed".equals(result.type())) {
            confirmCheckout(PaymentProvider.STRIPE, result.checkoutSessionId());
        } else {
            log.debug("Évènement Stripe ignoré : {}", result.type());
        }
    }

    private void confirmCheckout(PaymentProvider provider, String providerReference) {
        Payment payment = paymentRepository.findByProviderAndProviderReference(provider, providerReference)
                .orElseThrow(() -> {
                    log.warn("Webhook {} : référence {} introuvable localement, nouvelle tentative demandée",
                            provider, providerReference);
                    return new ApiException(HttpStatus.CONFLICT, "Paiement local pas encore visible, réessayer");
                });
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return;
        }
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setInvoiceNumber(nextInvoiceNumber());
        paymentRepository.save(payment);
    }

    /** Orange Money : paiement ponctuel, pas de prélèvement récurrent — un webhook confirmé suffit à débloquer le cours. */
    @Transactional
    public void handleOrangeMoneyWebhook(String orderId, long amount, String payToken) {
        if (!(orangeMoneyGateway instanceof OrangeMoneyPaymentGateway orangeMoney)) {
            log.warn("Webhook Orange Money reçu mais Orange Money n'est pas configuré, ignoré");
            return;
        }
        Payment payment = paymentRepository
                .findByProviderAndProviderReference(PaymentProvider.ORANGE_MONEY, orderId)
                .orElse(null);
        if (payment == null) {
            log.warn("Webhook Orange Money : transaction {} introuvable localement, ignoré", orderId);
            return;
        }
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return;
        }
        // Le montant du webhook doit correspondre à celui enregistré au moment du checkout — sinon on
        // ne fait jamais confiance à la seule confirmation de statut du prestataire pour ce montant.
        if (payment.getAmount().longValueExact() != amount) {
            log.warn("Webhook Orange Money : montant {} ne correspond pas au paiement {} attendu ({}), rejeté",
                    amount, orderId, payment.getAmount());
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            return;
        }
        if (!orangeMoney.isTransactionConfirmed(orderId, amount, payToken)) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            return;
        }
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setInvoiceNumber(nextInvoiceNumber());
        paymentRepository.save(payment);
    }

    // ---------- privé ----------

    private PaymentGateway gatewayFor(PaymentProvider provider) {
        return provider == PaymentProvider.STRIPE ? stripeGateway : orangeMoneyGateway;
    }

    /** Numérotation séquentielle par année, même patron que le n° de série des certificats. */
    private String nextInvoiceNumber() {
        long sequence = paymentRepository.countByInvoiceNumberIsNotNull() + 1;
        return "INV-" + Year.now().getValue() + "-" + String.format("%06d", sequence);
    }

    private byte[] renderInvoicePdf(Payment payment, String buyerName, String courseTitle) throws Exception {
        String html = """
                <html><head><meta charset="utf-8"/><style>
                  @page { size: A4; margin: 0; }
                  body { font-family: sans-serif; color: #1f2937; margin: 48px; }
                  h1 { font-size: 24px; color: #7e22ce; margin: 0 0 4px; }
                  .sub { color: #6b7280; margin: 0 0 32px; }
                  table { width: 100%%; border-collapse: collapse; margin-top: 16px; }
                  th, td { text-align: left; padding: 8px 0; border-bottom: 1px solid #e5e7eb; }
                  .total { font-weight: bold; font-size: 16px; }
                  .meta { margin-top: 32px; color: #6b7280; font-size: 12px; }
                </style></head><body>
                <h1>FACTURE</h1>
                <p class="sub">Plateforme e-learning educa</p>
                <p>N&#176; %s &#183; %s<br/>Facturé à : %s</p>
                <table>
                  <tr><th>Description</th><th>Prestataire</th><th>Montant</th></tr>
                  <tr><td>%s</td><td>%s</td><td class="total">%s %s</td></tr>
                </table>
                <p class="meta">Paiement %s &#183; référence %s</p>
                </body></html>
                """.formatted(payment.getInvoiceNumber(), payment.getCreatedAt(), escape(buyerName),
                escape(courseTitle), payment.getProvider(),
                payment.getAmount().stripTrailingZeros().toPlainString(), payment.getCurrency(),
                payment.getStatus(), payment.getProviderReference());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        }
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
