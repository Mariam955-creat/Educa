import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { filter } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';

interface DrawerLink {
  path: string;
  icon: string;
  labelKey: string;
}

/**
 * Espace « Mon compte » : tiroir de navigation à gauche (fixe sur grand écran, escamotable sur mobile)
 * et section courante à droite (routes enfants).
 */
@Component({
  selector: 'app-account-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslatePipe],
  templateUrl: './account-layout.component.html',
  styleUrl: './account-layout.component.scss',
})
export class AccountLayoutComponent {
  readonly auth = inject(AuthService);

  readonly links: DrawerLink[] = [
    { path: 'overview', icon: '📊', labelKey: 'account.nav.overview' },
    { path: 'profile', icon: '👤', labelKey: 'account.nav.profile' },
    { path: 'courses', icon: '📚', labelKey: 'account.nav.courses' },
    { path: 'certificates', icon: '🎓', labelKey: 'account.nav.certificates' },
    { path: 'invoices', icon: '🧾', labelKey: 'account.nav.invoices' },
    { path: 'reviews', icon: '⭐', labelKey: 'account.nav.reviews' },
  ];

  /** Tiroir ouvert (mobile uniquement : sur grand écran il est toujours visible). */
  readonly drawerOpen = signal(false);

  readonly initials = computed(() => {
    const user = this.auth.user();
    if (!user) return '';
    const words = user.fullName.trim().split(/\s+/).filter(Boolean);
    const letters = words.length > 1 ? words[0][0] + words[words.length - 1][0] : (words[0]?.[0] ?? user.email[0]);
    return letters.toUpperCase();
  });

  constructor() {
    inject(Router)
      .events.pipe(filter((e) => e instanceof NavigationEnd))
      .subscribe(() => this.drawerOpen.set(false));
  }

  @HostListener('document:keydown.escape')
  closeDrawer(): void {
    this.drawerOpen.set(false);
  }
}
