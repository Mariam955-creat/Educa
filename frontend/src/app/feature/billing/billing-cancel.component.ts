import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-billing-cancel',
  imports: [TranslatePipe, RouterLink],
  template: `
    <section class="page">
      <div class="ko">
        <p class="badge">{{ 'billing.cancelledTitle' | translate }}</p>
        <p>{{ 'billing.cancelledBody' | translate }}</p>
        <a routerLink="/billing">{{ 'billing.backToBilling' | translate }}</a>
      </div>
    </section>
  `,
  styles: [
    `
      .page {
        max-width: 560px;
        margin: 0 auto;
        padding: 3rem 1rem;
      }
      .ko {
        border: 1px solid var(--border);
        background: var(--surface);
        border-radius: 12px;
        padding: 1.5rem;
        text-align: center;
      }
      .badge {
        font-weight: 700;
        color: var(--muted);
        margin-top: 0;
      }
      a {
        color: var(--accent);
      }
    `,
  ],
})
export class BillingCancelComponent {}
