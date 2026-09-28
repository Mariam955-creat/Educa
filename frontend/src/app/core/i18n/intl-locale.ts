import { AppLang } from './language.service';

/** Locale `Intl` par langue d'interface. */
const LOCALES: Record<AppLang, string> = {
  fr: 'fr-FR',
  en: 'en-GB',
  de: 'de-DE',
  nl: 'nl-NL',
};

export function intlLocale(lang: AppLang): string {
  return LOCALES[lang] ?? 'fr-FR';
}
