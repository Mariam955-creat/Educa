import { Component, HostListener, inject, input, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { filter } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';

export interface DrawerLink {
  /** Chemin relatif à la route de l'espace (ex. `overview`) ou absolu (ex. `/admin`). */
  path: string;
  icon: string;
  labelKey: string;
  /** Pastille de compteur à droite du lien (ex. demandes en attente) ; masquée si absente ou nulle. */
  badge?: number;
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
  host: { '[class.left]': "side() === 'left'" },
})
export class DrawerLayoutComponent {
  readonly auth = inject(AuthService);

  /** Clé de traduction du nom de l'espace (titre du tiroir, bouton d'ouverture sur mobile). */
  readonly titleKey = input.required<string>();
  readonly links = input.required<DrawerLink[]>();
  readonly footerLinks = input<DrawerLink[]>([]);
  /** Bouton d'action principal en tête du tiroir (ex. « Nouveau cours »). */
  readonly cta = input<DrawerLink | null>(null);
  /** Côté du tiroir : à droite par défaut (compte, formateur), à gauche pour l'administration. */
  readonly side = input<'left' | 'right'>('right');

  readonly open = signal(false);

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
