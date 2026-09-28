import { MoneyPipe } from './money.pipe';

describe('MoneyPipe', () => {
  const pipe = new MoneyPipe();
  // Intl insère des espaces insécables (U+00A0 / U+202F) : on les normalise pour comparer.
  const plain = (s: string) => s.replace(/[  ]/g, ' ');

  it('formate à la française en fr', () => {
    expect(plain(pipe.transform(19.99, 'EUR', 'fr'))).toBe('19,99 €');
  });

  it('formate à l’anglaise en en', () => {
    expect(pipe.transform(19.99, 'eur', 'en')).toBe('€19.99');
  });

  it('formate à la néerlandaise en nl', () => {
    expect(pipe.transform(19.99, 'EUR', 'nl')).toContain('19,99');
  });

  it('renvoie une chaîne vide sans montant', () => {
    expect(pipe.transform(null, 'EUR', 'fr')).toBe('');
  });
});
