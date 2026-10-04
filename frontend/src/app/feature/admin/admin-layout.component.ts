import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { DrawerLayoutComponent, DrawerLink } from '../../shared/drawer-layout/drawer-layout.component';

/** Espace administration : tiroir (tableau de bord + une section par domaine) et section courante. */
@Component({
  selector: 'app-admin-layout',
  imports: [RouterOutlet, DrawerLayoutComponent],
  template: `
    <app-drawer-layout titleKey="admin.title" subtitleKey="userMenu.roleAdmin" [links]="links" [footerLinks]="footerLinks">
      <router-outlet />
    </app-drawer-layout>
  `,
})
export class AdminLayoutComponent {
  readonly links: DrawerLink[] = [
    { path: 'overview', icon: '📊', labelKey: 'admin.nav.overview' },
    { path: 'users', icon: '👥', labelKey: 'admin.nav.users' },
    { path: 'courses', icon: '📚', labelKey: 'admin.nav.courses' },
    { path: 'payments', icon: '💶', labelKey: 'admin.nav.payments' },
    { path: 'certificates', icon: '🎓', labelKey: 'admin.nav.certificates' },
    { path: 'reviews', icon: '⭐', labelKey: 'admin.nav.reviews' },
    { path: 'languages', icon: '🌐', labelKey: 'admin.nav.languages' },
    { path: 'trash', icon: '🗑', labelKey: 'trash.title' },
  ];

  readonly footerLinks: DrawerLink[] = [
    { path: '/instructor', icon: '🧑‍🏫', labelKey: 'account.nav.instructorSpace' },
    { path: '/account', icon: '👤', labelKey: 'userMenu.account' },
  ];
}
