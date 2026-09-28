import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { AuthService } from '../auth/auth.service';

export type AppLang = 'fr' | 'en' | 'de' | 'nl';
const SUPPORTED: AppLang[] = ['fr', 'en', 'de', 'nl'];
const STORAGE_KEY = 'educa.lang';

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly translate = inject(TranslateService);
  private readonly auth = inject(AuthService);
  private readonly document = inject(DOCUMENT);

  private readonly _current = signal<AppLang>('fr');
  readonly current = this._current.asReadonly();
  readonly supported = SUPPORTED;

  /** À appeler une fois au démarrage de l'application. */
  init(): void {
    const initial = this.resolveInitial();
    this.translate.addLangs(SUPPORTED);
    this.translate.setFallbackLang('fr');
    this.apply(initial, false);
  }

  set(lang: AppLang): void {
    this.apply(lang, true);
  }

  private apply(lang: AppLang, persistRemote: boolean): void {
    this._current.set(lang);
    this.translate.use(lang);

    const html = this.document.documentElement;
    html.setAttribute('lang', lang);

    try {
      localStorage.setItem(STORAGE_KEY, lang);
    } catch {
      /* stockage indisponible : on ignore */
    }

    if (persistRemote && this.auth.isAuthenticated() && this.auth.user()?.preferredLanguage !== lang) {
      this.auth.updatePreferredLanguage(lang).subscribe({ error: () => undefined });
    }
  }

  private resolveInitial(): AppLang {
    const fromUser = this.auth.user()?.preferredLanguage;
    if (this.isSupported(fromUser)) return fromUser;
    let stored: string | null = null;
    try {
      stored = localStorage.getItem(STORAGE_KEY);
    } catch {
      /* ignore */
    }
    return this.isSupported(stored) ? (stored as AppLang) : 'fr';
  }

  private isSupported(value: string | null | undefined): value is AppLang {
    return !!value && SUPPORTED.includes(value as AppLang);
  }
}
