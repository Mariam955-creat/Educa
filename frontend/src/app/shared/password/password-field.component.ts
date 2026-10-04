import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { AbstractControl, FormControl, ReactiveFormsModule, ValidationErrors } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';

/** Règle partagée avec le backend (`PasswordRules`) : 8 caractères minimum, au moins une lettre et un chiffre. */
export const PASSWORD_PATTERN = /^(?=.*\p{L})(?=.*\d).+$/u;

/** Les deux mots de passe saisis sont identiques. */
export function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return password && confirm && password !== confirm ? { passwordMismatch: true } : null;
}

/** Score de 0 à 4 : longueur, mélange de casses, chiffres, caractères spéciaux. */
export function passwordScore(value: string): number {
  if (!value) return 0;
  let score = 0;
  if (value.length >= 8) score++;
  if (value.length >= 12) score++;
  if (/[a-z]/.test(value) && /[A-Z]/.test(value)) score++;
  if (/\d/.test(value) && /[^\p{L}\d]/u.test(value)) score++;
  return Math.min(score, 4);
}

/**
 * Champ mot de passe avec bouton afficher/masquer et, en option, une jauge de force.
 * Le contrôle reste dans le formulaire parent (passé via `control`).
 */
@Component({
  selector: 'app-password-field',
  imports: [ReactiveFormsModule, TranslatePipe],
  template: `
    <span class="wrap">
      <input
        [type]="visible() ? 'text' : 'password'"
        [formControl]="control()"
        [attr.autocomplete]="autocomplete()"
        [attr.id]="inputId()"
      />
      <button type="button" class="toggle" (click)="visible.set(!visible())" [attr.aria-pressed]="visible()">
        {{ (visible() ? 'password.hide' : 'password.show') | translate }}
      </button>
    </span>
    @if (showStrength() && value()) {
      <span class="meter" aria-hidden="true">
        @for (i of [1, 2, 3, 4]; track i) {
          <span class="seg" [class.on]="score() >= i" [attr.data-level]="score()"></span>
        }
      </span>
      <small class="strength">{{ 'password.strength.' + score() | translate }}</small>
    }
  `,
  styles: `
    :host { display: flex; flex-direction: column; gap: 0.3rem; }
    .wrap { position: relative; display: flex; }
    input { flex: 1; padding: 0.6rem 4.8rem 0.6rem 0.7rem; border: 1px solid var(--border); border-radius: 8px; font: inherit; font-weight: 400; }
    .toggle { position: absolute; inset-block: 0.3rem; inset-inline-end: 0.3rem; margin: 0; padding: 0 0.6rem; border: 0; border-radius: 6px; background: var(--bg); color: var(--muted); font: inherit; font-size: 0.75rem; font-weight: 600; cursor: pointer; }
    .meter { display: grid; grid-template-columns: repeat(4, 1fr); gap: 4px; }
    .seg { height: 4px; border-radius: 999px; background: var(--border); }
    .seg.on[data-level='1'] { background: #dc2626; }
    .seg.on[data-level='2'] { background: #f59e0b; }
    .seg.on[data-level='3'] { background: #65a30d; }
    .seg.on[data-level='4'] { background: var(--success-strong); }
    .strength { color: var(--muted); font-size: 0.75rem; font-weight: 400; }
  `,
})
export class PasswordFieldComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);

  readonly control = input.required<FormControl<string>>();
  readonly autocomplete = input('current-password');
  readonly showStrength = input(false);
  readonly inputId = input<string | null>(null);

  readonly visible = signal(false);
  readonly value = signal('');
  readonly score = computed(() => passwordScore(this.value()));

  ngOnInit(): void {
    // Suit aussi les changements venus du parent (ex. réinitialisation après enregistrement)
    const control = this.control();
    this.value.set(control.value);
    control.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((v) => this.value.set(v));
  }
}
