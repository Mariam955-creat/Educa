import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { RoleName } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { AppLang, LanguageService } from '../../core/i18n/language.service';

const ROLE_KEYS: Record<RoleName, string> = {
  LEARNER: 'userMenu.roleLearner',
  INSTRUCTOR: 'userMenu.roleInstructor',
  ADMIN: 'userMenu.roleAdmin',
};

/** Section « Mon profil » de l'espace compte : identité (nom modifiable), rôles, langue de l'interface. */
@Component({
  selector: 'app-account-profile',
  imports: [ReactiveFormsModule, TranslatePipe],
  templateUrl: './account-profile.component.html',
  styleUrl: './account-profile.component.scss',
})
export class AccountProfileComponent {
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);
  readonly auth = inject(AuthService);
  readonly lang = inject(LanguageService);

  readonly form = this.fb.nonNullable.group({
    fullName: [this.auth.user()?.fullName ?? '', [Validators.required, Validators.maxLength(150)]],
  });
  readonly saving = signal(false);
  readonly message = signal<string | null>(null);
  readonly messageIsError = signal(false);

  readonly roleKeys = computed(() => (this.auth.user()?.roles ?? []).map((r) => ROLE_KEYS[r]));

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.auth.updateFullName(this.form.getRawValue().fullName.trim()).subscribe({
      next: () => {
        this.saving.set(false);
        this.flash(this.translate.instant('account.saved'));
        this.form.markAsPristine();
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.flash(err.error?.message ?? this.translate.instant('account.saveError'), true);
      },
    });
  }

  changeLang(code: string): void {
    this.lang.set(code as AppLang);
  }

  private flash(text: string, isError = false): void {
    this.message.set(text);
    this.messageIsError.set(isError);
    setTimeout(() => this.message.set(null), 3000);
  }
}
