import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import {
  PaymentApiService,
  PaymentProvider,
  Subscription,
  SubscriptionPlan,
} from '../../core/payment/payment-api.service';

@Component({
  selector: 'app-billing',
  imports: [DatePipe, TranslatePipe],
  templateUrl: './billing.component.html',
  styleUrl: './billing.component.scss',
})
export class BillingComponent implements OnInit {
  private readonly api = inject(PaymentApiService);
  private readonly translate = inject(TranslateService);

  readonly subscription = signal<Subscription | null>(null);
  readonly loading = signal(true);
  readonly starting = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  startCheckout(plan: SubscriptionPlan, provider: PaymentProvider): void {
    const key = plan + provider;
    this.starting.set(key);
    this.error.set(null);
    this.api.checkout(plan, provider).subscribe({
      next: (res) => {
        window.location.href = res.checkoutUrl;
      },
      error: (err: HttpErrorResponse) => {
        this.starting.set(null);
        this.error.set(err.error?.message ?? this.translate.instant('billing.error'));
      },
    });
  }

  cancel(): void {
    if (!confirm(this.translate.instant('billing.confirmCancel'))) return;
    this.api.cancel().subscribe({
      next: (sub) => this.subscription.set(sub),
      error: (err: HttpErrorResponse) => this.error.set(err.error?.message ?? this.translate.instant('billing.error')),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.api.mySubscription().subscribe({
      next: (sub) => {
        this.subscription.set(sub);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
