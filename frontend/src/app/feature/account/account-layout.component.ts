import { Component, computed, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { DrawerLayoutComponent, DrawerLink } from '../../shared/drawer-layout/drawer-layout.component';

/** Espace « Mon compte » : tiroir (sections de l'utilisateur) + section courante (routes enfants). */
@Component({
  selector: 'app-account-layout',
  imports: [RouterOutlet, DrawerLayoutComponent],
  template: `
    <app-drawer-layout titleKey="account.title" [links]="links" [footerLinks]="footerLinks()">
      <router-outlet />
    </app-drawer-layout>
  `,
})
export class AccountLayoutComponent {
  private readonly auth = inject(AuthService);

  readonly links: DrawerLink[] = [
    { path: 'overview', icon: '📊', labelKey: 'account.nav.overview' },
    { path: 'profile', icon: '👤', labelKey: 'account.nav.profile' },
    { path: 'courses', icon: '📚', labelKey: 'account.nav.courses' },
    { path: 'certificates', icon: '🎓', labelKey: 'account.nav.certificates' },
    { path: 'invoices', icon: '🧾', labelKey: 'account.nav.invoices' },
    { path: 'reviews', icon: '⭐', labelKey: 'account.nav.reviews' },
  ];

  readonly footerLinks = computed<DrawerLink[]>(() => [
    ...(this.auth.hasAnyRole(['INSTRUCTOR', 'ADMIN'])
      ? [{ path: '/instructor', icon: '🧑‍🏫', labelKey: 'account.nav.instructorSpace' }]
      : []),
    ...(this.auth.hasRole('ADMIN') ? [{ path: '/admin', icon: '🛠️', labelKey: 'nav.admin' }] : []),
  ]);
}
