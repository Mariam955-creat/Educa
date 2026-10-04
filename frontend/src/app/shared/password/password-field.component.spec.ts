import { FormControl, FormGroup } from '@angular/forms';

import { PASSWORD_PATTERN, passwordScore, passwordsMatch } from './password-field.component';

describe('Règles de mot de passe', () => {
  it('exige au moins une lettre et un chiffre, comme le backend', () => {
    expect(PASSWORD_PATTERN.test('password123')).toBeTrue();
    expect(PASSWORD_PATTERN.test('motdepasse')).toBeFalse();
    expect(PASSWORD_PATTERN.test('12345678')).toBeFalse();
    expect(PASSWORD_PATTERN.test('élève2026')).toBeTrue();
  });

  it('note la force de 0 (trop court) à 4 (excellent)', () => {
    expect(passwordScore('')).toBe(0);
    expect(passwordScore('abc1')).toBe(0);
    expect(passwordScore('password123')).toBe(1);
    expect(passwordScore('Password1234')).toBe(3);
    expect(passwordScore('Password1234!')).toBe(4);
  });

  it('signale deux mots de passe différents', () => {
    const group = new FormGroup({
      password: new FormControl('password123'),
      confirmPassword: new FormControl('password124'),
    });
    expect(passwordsMatch(group)).toEqual({ passwordMismatch: true });
    group.controls.confirmPassword.setValue('password123');
    expect(passwordsMatch(group)).toBeNull();
  });
});
