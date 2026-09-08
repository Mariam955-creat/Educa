import { Component, inject } from '@angular/core';

import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-learner-dashboard',
  template: `
    <section class="page">
      <h1>Bonjour {{ auth.user()?.fullName }} 👋</h1>
      <p class="role">Espace apprenant</p>
      <div class="placeholder">
        <p>Mes formations en cours et mes certificats apparaîtront ici (Phase 2 &amp; 3).</p>
      </div>
    </section>
  `,
  styleUrl: './dashboard.scss',
})
export class LearnerDashboardComponent {
  readonly auth = inject(AuthService);
}
