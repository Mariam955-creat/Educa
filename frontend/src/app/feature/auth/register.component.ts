import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { AuthService } from '../../core/auth/auth.service';
import { InstructorRequestApiService } from '../../core/instructor/instructor-request-api.service';
import { InstructorApplicationComponent } from '../../shared/instructor-application/instructor-application.component';
import { countryOptions } from '../../core/i18n/countries';
import { AppLang, LanguageService } from '../../core/i18n/language.service';
import { PASSWORD_PATTERN, PasswordFieldComponent, passwordsMatch } from '../../shared/password/password-field.component';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe, PasswordFieldComponent, InstructorApplicationComponent],
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
      wantsToTeach: [false],
    },
    { validators: passwordsMatch },
  );

  readonly countries = computed(() => countryOptions(this.lang.current()));
  readonly error = signal<string | null>(null);
  readonly loading = signal(false);

  /** Étape affichée : le compte, puis la candidature formateur si « Je veux enseigner » est coché. */
  readonly step = signal<'account' | 'application'>('account');
  readonly wantsToTeach = toSignal(this.form.controls.wantsToTeach.valueChanges, { initialValue: false });
  /** Erreurs de la candidature affichées seulement après une tentative d'envoi. */
  readonly applicationTried = signal(false);
  readonly submitLabel = computed(() =>
    this.loading()
      ? 'auth.register.submitting'
      : this.step() === 'application'
        ? 'application.submitWithAccount'
        : this.wantsToTeach()
          ? 'application.next'
          : 'auth.register.submit',
  );
  private readonly application = viewChild(InstructorApplicationComponent);
  private readonly instructorRequests = inject(InstructorRequestApiService);

  /** La langue choisie dans le formulaire s'applique tout de suite à l'interface. */
  onLanguageChange(code: string): void {
    this.lang.set(code as AppLang);
  }

  submit(): void {
    if (this.step() === 'account') {
      if (this.form.invalid) {
        this.form.markAllAsTouched();
        return;
      }
      if (this.wantsToTeach()) {
        // « Je veux enseigner » : seconde étape, la candidature (pièces + motivation)
        this.error.set(null);
        this.step.set('application');
        return;
      }
      this.createAccount(null);
      return;
    }
    this.applicationTried.set(true);
    const application = this.application();
    if (!application?.valid()) return;
    this.createAccount(application.formData());
  }

  /**
   * Crée le compte, connecte, puis envoie la candidature s'il y en a une. Si cet envoi échoue, le compte existe
   * déjà : on ouvre l'espace compte, où le formulaire « Devenir formateur » permet de la renvoyer.
   */
  private createAccount(application: FormData | null): void {
    this.loading.set(true);
    this.error.set(null);
    const { fullName, email, password, country, preferredLanguage } = this.form.getRawValue();
    this.auth
      .register({ fullName: fullName.trim(), email: email.trim(), password, preferredLanguage, country: country || undefined })
      .subscribe({
        next: () => {
          this.auth.login(email.trim(), password).subscribe({
            next: () => {
              if (!application) {
                this.router.navigateByUrl(this.auth.homePathForRole());
                return;
              }
              this.instructorRequests.submit(application).subscribe({
                next: () => this.router.navigate(['/account/profile']),
                error: () => this.router.navigate(['/account/profile'], { state: { instructorRequestFailed: true } }),
              });
            },
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
          // Erreur sur le compte (email déjà pris, champ invalide) : retour à l'étape où la corriger
          if (err.status === 409 || err.status === 400) this.step.set('account');
        },
      });
  }
}
