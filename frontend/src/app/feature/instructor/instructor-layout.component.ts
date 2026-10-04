import { Component, computed, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { DrawerLayoutComponent, DrawerLink } from '../../shared/drawer-layout/drawer-layout.component';

/**
 * Espace formateur (studio) : tiroir — tableau de bord, cours, revenus, avis reçus — et section courante.
 * L'édition d'un cours, d'un quiz et les résultats s'affichent aussi ici, le tiroir reste donc visible.
 */
@Component({
  selector: 'app-instructor-layout',
  imports: [RouterOutlet, DrawerLayoutComponent],
  template: `
    <app-drawer-layout
      titleKey="instructor.title"
      subtitleKey="instructor.title"
      [cta]="newCourse"
      [links]="links"
      [footerLinks]="footerLinks()"
    >
      <router-outlet />
    </app-drawer-layout>
  `,
})
export class InstructorLayoutComponent {
  private readonly auth = inject(AuthService);

  readonly newCourse: DrawerLink = { path: 'courses/new', icon: '＋', labelKey: 'instructor.newCourse' };

  readonly links: DrawerLink[] = [
    { path: 'overview', icon: '📊', labelKey: 'instructor.nav.overview' },
    { path: 'courses', icon: '📚', labelKey: 'instructor.nav.courses' },
    { path: 'sales', icon: '💶', labelKey: 'instructor.nav.sales' },
    { path: 'reviews', icon: '⭐', labelKey: 'instructor.nav.reviews' },
  ];

  readonly footerLinks = computed<DrawerLink[]>(() => [
    { path: '/account', icon: '👤', labelKey: 'userMenu.account' },
    ...(this.auth.hasRole('ADMIN') ? [{ path: '/admin', icon: '🛠️', labelKey: 'nav.admin' }] : []),
  ]);
}
