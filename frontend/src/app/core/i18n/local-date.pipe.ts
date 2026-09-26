import { Pipe, PipeTransform } from '@angular/core';

import { intlLocale } from './intl-locale';
import { AppLang } from './language.service';

const FORMATS: Record<'long' | 'short', Intl.DateTimeFormatOptions> = {
  long: { dateStyle: 'long' },
  short: { dateStyle: 'short', timeStyle: 'short' },
};

/**
 * Date dans la langue de l'interface : « 26 septembre 2026 » (fr), « 26 September 2026 » (en).
 * Remplace `DatePipe`, qui suit `LOCALE_ID` (en-US, aucune autre locale enregistrée) et non le sélecteur de langue.
 */
@Pipe({ name: 'localDate' })
export class LocalDatePipe implements PipeTransform {
  transform(value: string | Date | null | undefined, format: 'long' | 'short', lang: AppLang): string {
    if (value == null) {
      return '';
    }
    return new Intl.DateTimeFormat(intlLocale(lang), FORMATS[format]).format(new Date(value));
  }
}
