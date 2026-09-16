import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-billing-success',
  imports: [TranslatePipe, RouterLink],
  template: `
    <section class="page">
      <div class="ok">
        <p class="badge">{{ 'billing.successTitle' | translate }}</p>
        <p>{{ 'billing.successBody' | translate }}</p>
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
      .ok {
        border: 1px solid #bbf7d0;
        background: #f0fdf4;
        border-radius: 12px;
        padding: 1.5rem;
        text-align: center;
      }
      .badge {
        font-weight: 700;
        color: #166534;
        margin-top: 0;
      }
      a {
        color: var(--accent);
      }
    `,
  ],
})
export class BillingSuccessComponent {}
