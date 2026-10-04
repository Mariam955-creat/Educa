import { Component, ElementRef, HostListener, computed, inject, signal } from '@angular/core';
import { NavigationStart, Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { filter } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { AppLang, LanguageService } from '../i18n/language.service';

/** Avatar de l'en-tête et son menu déroulant : compte, cours, achats, langue, déconnexion. */
@Component({
  selector: 'app-user-menu',
  imports: [RouterLink, TranslatePipe],
  templateUrl: './user-menu.component.html',
  styleUrl: './user-menu.component.scss',
})
export class UserMenuComponent {
  readonly auth = inject(AuthService);
  readonly lang = inject(LanguageService);
  private readonly host = inject(ElementRef<HTMLElement>);

  readonly open = signal(false);

  /** Initiales du nom (« Amina Bah » → « AB »), à défaut la première lettre de l'email. */
  readonly initials = computed(() => {
    const user = this.auth.user();
    if (!user) return '';
    const words = user.fullName.trim().split(/\s+/).filter(Boolean);
    const letters = words.length > 1 ? words[0][0] + words[words.length - 1][0] : (words[0]?.[0] ?? user.email[0]);
    return letters.toUpperCase();
  });

  /** Rôle le plus élevé, affiché sous le nom. */
  readonly roleKey = computed(() => {
    if (this.auth.hasRole('ADMIN')) return 'userMenu.roleAdmin';
    if (this.auth.hasRole('INSTRUCTOR')) return 'userMenu.roleInstructor';
    return 'userMenu.roleLearner';
  });

  constructor() {
    // Une navigation (clic sur un lien du menu, bouton précédent…) referme le menu
    inject(Router)
      .events.pipe(filter((e) => e instanceof NavigationStart))
      .subscribe(() => this.open.set(false));
  }

  toggle(): void {
    this.open.update((v) => !v);
  }

  changeLang(code: AppLang): void {
    this.lang.set(code);
  }

  logout(): void {
    this.open.set(false);
    this.auth.logout();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.open() && !this.host.nativeElement.contains(event.target as Node)) {
      this.open.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.open.set(false);
  }
}
