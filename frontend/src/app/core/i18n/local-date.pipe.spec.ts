import { LocalDatePipe } from './local-date.pipe';

describe('LocalDatePipe', () => {
  const pipe = new LocalDatePipe();
  // Midi UTC : même jour quel que soit le fuseau de la machine de test.
  const date = '2026-09-26T12:00:00Z';

  it('écrit la date en français en fr', () => {
    expect(pipe.transform(date, 'long', 'fr')).toBe('26 septembre 2026');
  });

  it('écrit la date en anglais en en', () => {
    expect(pipe.transform(date, 'long', 'en')).toBe('26 September 2026');
  });

  it('garde les chiffres latins en ar', () => {
    expect(pipe.transform(date, 'long', 'ar')).toContain('2026');
  });

  it('format court jour/mois/année en fr', () => {
    expect(pipe.transform(date, 'short', 'fr')).toContain('26/09/2026');
  });

  it('renvoie une chaîne vide sans date', () => {
    expect(pipe.transform(null, 'long', 'fr')).toBe('');
  });
});
