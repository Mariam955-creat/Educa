import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './auth.scss',
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(150)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
    preferredLanguage: ['fr', [Validators.required]],
  });

  readonly error = signal<string | null>(null);
  readonly loading = signal(false);

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);

    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => {
        const { email, password } = this.form.getRawValue();
        // Connexion automatique après inscription.
        this.auth.login(email, password).subscribe({
          next: () => this.router.navigateByUrl(this.auth.homePathForRole()),
          error: () => this.router.navigate(['/login']),
        });
      },
      error: (err: { status?: number }) => {
        this.error.set(
          err?.status === 409
            ? 'Un compte existe déjà pour cet email.'
            : err?.status === 400
              ? 'Vérifiez les champs du formulaire.'
              : 'Impossible de créer le compte.',
        );
        this.loading.set(false);
      },
    });
  }
}
