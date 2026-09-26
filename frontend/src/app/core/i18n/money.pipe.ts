import { Pipe, PipeTransform } from '@angular/core';

import { intlLocale } from './intl-locale';
import { AppLang } from './language.service';

/**
 * Montant dans la langue de l'interface : « 19,99 € » (fr), « €19.99 » (en).
 * La langue est passée en argument (`lang.current()`) pour que le pipe reste pur tout en suivant le sélecteur.
 */
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  transform(amount: number | null | undefined, currency: string, lang: AppLang): string {
    if (amount == null) {
      return '';
    }
    return new Intl.NumberFormat(intlLocale(lang), {
      style: 'currency',
      currency: currency.toUpperCase(),
    }).format(amount);
  }
}
