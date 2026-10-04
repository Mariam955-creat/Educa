import { Component, HostListener, computed, inject, input, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { filter } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';

export interface DrawerLink {
  /** Chemin relatif à la route de l'espace (ex. `overview`) ou absolu (ex. `/admin`). */
  path: string;
  icon: string;
  labelKey: string;
}

/**
 * Mise en page « espace » (Mon compte, espace formateur) : tiroir de navigation à gauche — fixe sur grand
 * écran, escamotable sur mobile — et contenu projeté à droite.
 */
@Component({
  selector: 'app-drawer-layout',
  imports: [RouterLink, RouterLinkActive, TranslatePipe],
  templateUrl: './drawer-layout.component.html',
  styleUrl: './drawer-layout.component.scss',
})
export class DrawerLayoutComponent {
  readonly auth = inject(AuthService);

  /** Clé de traduction du nom de l'espace (bouton d'ouverture sur mobile, libellé du tiroir). */
  readonly titleKey = input.required<string>();
  /** Sous-titre affiché sous le nom de l'utilisateur (ex. « Espace formateur »). */
  readonly subtitleKey = input<string | null>(null);
  readonly links = input.required<DrawerLink[]>();
  readonly footerLinks = input<DrawerLink[]>([]);
  /** Bouton d'action principal en tête du tiroir (ex. « Nouveau cours »). */
  readonly cta = input<DrawerLink | null>(null);

  readonly open = signal(false);

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
      .subscribe(() => this.open.set(false));
  }

  @HostListener('document:keydown.escape')
  close(): void {
    this.open.set(false);
  }
}
