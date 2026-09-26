import { AppLang } from './language.service';

/** Locale `Intl` par langue d'interface. Chiffres latins aussi en arabe : usage courant au Maghreb et en Afrique de l'Ouest. */
const LOCALES: Record<AppLang, string> = { fr: 'fr-FR', en: 'en-GB', ar: 'ar-u-nu-latn' };

export function intlLocale(lang: AppLang): string {
  return LOCALES[lang] ?? 'fr-FR';
}
