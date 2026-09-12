import { Component, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-admin-dashboard',
  imports: [TranslatePipe],
  template: `
    <section class="page">
      <h1>{{ 'admin.title' | translate }}</h1>
      <p class="role">{{ 'admin.connectedAs' | translate: { email: auth.user()?.email } }}</p>
      <div class="placeholder">
        <p>{{ 'admin.placeholder' | translate }}</p>
      </div>
    </section>
  `,
  styleUrl: '../dashboard/dashboard.scss',
})
export class AdminDashboardComponent {
  readonly auth = inject(AuthService);
}
