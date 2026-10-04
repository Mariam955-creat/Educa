import { intlLocale } from './intl-locale';
import { AppLang } from './language.service';

/**
 * Pays proposés (ISO 3166-1 alpha-2) : Europe francophone et voisine, Amérique du Nord, Afrique francophone
 * (public visé par le paiement Orange Money). Les noms sont fournis par `Intl`, dans la langue de l'interface.
 */
const COUNTRY_CODES = [
  'FR', 'BE', 'CH', 'LU', 'MC', 'DE', 'NL', 'AT', 'GB', 'IE', 'ES', 'PT', 'IT', 'PL',
  'CA', 'US', 'HT',
  'MA', 'DZ', 'TN', 'EG', 'SN', 'CI', 'ML', 'BF', 'NE', 'GN', 'TG', 'BJ', 'CM', 'GA', 'CG', 'CD', 'CF', 'TD',
  'MR', 'MG', 'RW', 'BI', 'DJ', 'KM', 'MU', 'NG', 'GH', 'KE', 'ZA',
];

export interface CountryOption {
  code: string;
  name: string;
}

/** Liste triée par nom, dans la langue de l'interface. */
export function countryOptions(lang: AppLang): CountryOption[] {
  const locale = intlLocale(lang);
  const names = new Intl.DisplayNames([locale], { type: 'region' });
  return COUNTRY_CODES.map((code) => ({ code, name: names.of(code) ?? code })).sort((a, b) =>
    a.name.localeCompare(b.name, locale),
  );
}
