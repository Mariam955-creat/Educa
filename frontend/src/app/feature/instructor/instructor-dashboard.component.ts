import { Component, inject } from '@angular/core';

import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-instructor-dashboard',
  template: `
    <section class="page">
      <h1>Espace formateur</h1>
      <p class="role">Connecté : {{ auth.user()?.email }}</p>
      <div class="placeholder">
        <p>Création et gestion des cours, chapitres, contenus et quiz (Phase 2 &amp; 3).</p>
      </div>
    </section>
  `,
  styleUrl: '../dashboard/dashboard.scss',
})
export class InstructorDashboardComponent {
  readonly auth = inject(AuthService);
}
