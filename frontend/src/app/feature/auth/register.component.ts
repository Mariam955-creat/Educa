import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { AuthService } from '../../core/auth/auth.service';
import { countryOptions } from '../../core/i18n/countries';
import { AppLang, LanguageService } from '../../core/i18n/language.service';
import { PASSWORD_PATTERN, PasswordFieldComponent, passwordsMatch } from '../../shared/password/password-field.component';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe, PasswordFieldComponent],
  templateUrl: './register.component.html',
  styleUrl: './auth.scss',
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);
  readonly lang = inject(LanguageService);

  readonly form = this.fb.nonNullable.group(
    {
      fullName: ['', [Validators.required, Validators.maxLength(150)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100), Validators.pattern(PASSWORD_PATTERN)]],
      confirmPassword: ['', [Validators.required]],
      country: [''],
      preferredLanguage: [this.lang.current() as string, [Validators.required]],
    },
    { validators: passwordsMatch },
  );

  readonly countries = computed(() => countryOptions(this.lang.current()));
  readonly error = signal<string | null>(null);
  readonly loading = signal(false);

  /** La langue choisie dans le formulaire s'applique tout de suite à l'interface. */
  onLanguageChange(code: string): void {
    this.lang.set(code as AppLang);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);

    const { fullName, email, password, country, preferredLanguage } = this.form.getRawValue();
    this.auth
      .register({ fullName: fullName.trim(), email: email.trim(), password, preferredLanguage, country: country || undefined })
      .subscribe({
        next: () => {
          // Connexion automatique après inscription
          this.auth.login(email.trim(), password).subscribe({
            next: () => this.router.navigateByUrl(this.auth.homePathForRole()),
            error: () => this.router.navigate(['/login']),
          });
        },
        error: (err: HttpErrorResponse) => {
          const key =
            err.status === 409
              ? 'auth.register.errorExists'
              : err.status === 400
                ? 'auth.register.errorInvalid'
                : 'auth.register.errorServer';
          this.error.set(this.translate.instant(key));
          this.loading.set(false);
        },
      });
  }
}
