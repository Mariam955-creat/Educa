import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { RoleName } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { countryOptions } from '../../core/i18n/countries';
import { AppLang, LanguageService } from '../../core/i18n/language.service';
import { PASSWORD_PATTERN, PasswordFieldComponent, passwordsMatch } from '../../shared/password/password-field.component';

const ROLE_KEYS: Record<RoleName, string> = {
  LEARNER: 'userMenu.roleLearner',
  INSTRUCTOR: 'userMenu.roleInstructor',
  ADMIN: 'userMenu.roleAdmin',
};

const PHONE_PATTERN = /^[+0-9 ().-]{6,30}$/;

/**
 * Section « Mon profil » : identité (nom, titre, biographie), coordonnées (téléphone, pays), préférences
 * (langue) et sécurité (changement de mot de passe).
 */
@Component({
  selector: 'app-account-profile',
  imports: [ReactiveFormsModule, TranslatePipe, PasswordFieldComponent],
  templateUrl: './account-profile.component.html',
  styleUrl: './account-profile.component.scss',
})
export class AccountProfileComponent {
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);
  readonly auth = inject(AuthService);
  readonly lang = inject(LanguageService);

  private readonly user = this.auth.user();

  readonly form = this.fb.nonNullable.group({
    fullName: [this.user?.fullName ?? '', [Validators.required, Validators.maxLength(150)]],
    headline: [this.user?.headline ?? '', [Validators.maxLength(120)]],
    bio: [this.user?.bio ?? '', [Validators.maxLength(1000)]],
    phone: [this.user?.phone ?? '', [Validators.pattern(PHONE_PATTERN)]],
    country: [this.user?.country ?? ''],
  });

  readonly passwordForm = this.fb.nonNullable.group(
    {
      currentPassword: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100), Validators.pattern(PASSWORD_PATTERN)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordsMatch },
  );

  readonly saving = signal(false);
  readonly savingPassword = signal(false);
  readonly message = signal<string | null>(null);
  readonly messageIsError = signal(false);
  readonly passwordError = signal<string | null>(null);

  readonly roleKeys = computed(() => (this.auth.user()?.roles ?? []).map((r) => ROLE_KEYS[r]));
  readonly isInstructor = computed(() => this.auth.hasAnyRole(['INSTRUCTOR', 'ADMIN']));
  readonly countries = computed(() => countryOptions(this.lang.current()));

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.saving.set(true);
    // Chaînes vides envoyées telles quelles : le serveur efface alors le champ
    this.auth
      .updateProfile({
        fullName: raw.fullName.trim(),
        headline: raw.headline.trim(),
        bio: raw.bio.trim(),
        phone: raw.phone.trim(),
        country: raw.country,
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.form.markAsPristine();
          this.flash(this.translate.instant('account.saved'));
        },
        error: (err: HttpErrorResponse) => {
          this.saving.set(false);
          this.flash(err.error?.message ?? this.translate.instant('account.saveError'), true);
        },
      });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    const { currentPassword, password } = this.passwordForm.getRawValue();
    this.savingPassword.set(true);
    this.passwordError.set(null);
    this.auth.changePassword(currentPassword, password).subscribe({
      next: () => {
        this.savingPassword.set(false);
        this.passwordForm.reset();
        this.flash(this.translate.instant('profile.passwordChanged'));
      },
      error: (err: HttpErrorResponse) => {
        this.savingPassword.set(false);
        this.passwordError.set(err.error?.message ?? this.translate.instant('account.saveError'));
      },
    });
  }

  changeLang(code: string): void {
    this.lang.set(code as AppLang);
  }

  private flash(text: string, isError = false): void {
    this.message.set(text);
    this.messageIsError.set(isError);
    setTimeout(() => this.message.set(null), 3500);
  }
}
