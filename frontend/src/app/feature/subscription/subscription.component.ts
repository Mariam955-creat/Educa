import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { AuthService } from '../../core/auth/auth.service';
import { LanguageService } from '../../core/i18n/language.service';
import { LocalDatePipe } from '../../core/i18n/local-date.pipe';
import { MoneyPipe } from '../../core/i18n/money.pipe';
import { PaymentApiService, PaymentProvider, Subscription } from '../../core/payment/payment-api.service';

/** Page publique de l'abonnement « accès à tous les cours » : offre, état et paiement d'un pass de 30 jours. */
@Component({
  selector: 'app-subscription',
  imports: [RouterLink, TranslatePipe, MoneyPipe, LocalDatePipe],
  templateUrl: './subscription.component.html',
  styleUrl: './subscription.component.scss',
})
export class SubscriptionComponent implements OnInit {
  private readonly api = inject(PaymentApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);
  readonly auth = inject(AuthService);
  readonly lang = inject(LanguageService);

  readonly offer = signal<Subscription | null>(null);
  readonly buying = signal<PaymentProvider | null>(null);
  readonly error = signal<string | null>(null);
  readonly notice = signal<'success' | 'cancelled' | null>(null);
  readonly benefits = ['allCourses', 'newCourses', 'certificates', 'noCommitment'] as const;

  ngOnInit(): void {
    const payment = this.route.snapshot.queryParamMap.get('payment');
    if (payment === 'success' || payment === 'cancelled') {
      this.notice.set(payment);
    }
    this.load();
  }

  subscribe(provider: PaymentProvider): void {
    if (!this.auth.isAuthenticated()) {
      void this.router.navigate(['/register']);
      return;
    }
    this.buying.set(provider);
    this.error.set(null);
    this.api.subscribe(provider).subscribe({
      next: (res) => {
        window.location.href = res.checkoutUrl;
      },
      error: (err: HttpErrorResponse) => {
        this.buying.set(null);
        this.error.set(err.error?.message ?? this.translate.instant('subscription.error'));
      },
    });
  }

  private load(): void {
    this.api.subscription().subscribe({
      next: (offer) => this.offer.set(offer),
      error: () => this.offer.set(null),
    });
  }
}
