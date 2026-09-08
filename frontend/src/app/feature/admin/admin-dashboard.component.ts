import { Component, inject } from '@angular/core';

import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-admin-dashboard',
  template: `
    <section class="page">
      <h1>Administration</h1>
      <p class="role">Connecté : {{ auth.user()?.email }}</p>
      <div class="placeholder">
        <p>Gestion des utilisateurs, des rôles, des langues et registre des certificats (Phase 3 &amp; 4).</p>
      </div>
    </section>
  `,
  styleUrl: '../dashboard/dashboard.scss',
})
export class AdminDashboardComponent {
  readonly auth = inject(AuthService);
}
