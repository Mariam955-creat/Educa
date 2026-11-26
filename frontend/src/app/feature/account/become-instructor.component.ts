import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal, viewChild } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { InstructorRequest } from '../../core/admin/admin.models';
import { LanguageService } from '../../core/i18n/language.service';
import { LocalDatePipe } from '../../core/i18n/local-date.pipe';
import { InstructorRequestApiService } from '../../core/instructor/instructor-request-api.service';
import { InstructorApplicationComponent } from '../../shared/instructor-application/instructor-application.component';

/**
 * « Devenir formateur » (tableau de bord du compte, pour un apprenant) : formulaire de demande avec une motivation
 * facultative, puis suivi de la demande — en attente, ou refusée avec le motif et la possibilité de redemander.
 */
@Component({
  selector: 'app-become-instructor',
  imports: [TranslatePipe, LocalDatePipe, InstructorApplicationComponent],
  template: `
    @if (!loading()) {
      <article class="become">
        <h2>🧑‍🏫 {{ 'account.instructorRequest.title' | translate }}</h2>

        @if (request()?.status === 'PENDING') {
          <p class="state pending">
            {{ 'account.instructorRequest.pending' | translate: { date: (request()!.createdAt | localDate: 'long' : lang.current()) } }}
          </p>
          @if (request()!.motivation) {
            <p class="motivation">{{ request()!.motivation }}</p>
          }
        } @else {
          @if (request()?.status === 'REJECTED') {
            <p class="state rejected">
              {{ 'account.instructorRequest.rejected' | translate }}
              @if (request()!.rejectionReason) {
                <br /><strong>{{ 'admin.requests.reason' | translate }} :</strong> {{ request()!.rejectionReason }}
              }
            </p>
          }
          @if (failedAtSignup) {
            <p class="state rejected">{{ 'account.instructorRequest.failedAtSignup' | translate }}</p>
          }
          <p class="muted">{{ 'account.instructorRequest.intro' | translate }}</p>
          <app-instructor-application [showErrors]="tried()" />
          @if (error()) {
            <p class="error">{{ error() }}</p>
          }
          <button type="button" [disabled]="sending()" (click)="submit()">
            {{ 'account.instructorRequest.submit' | translate }}
          </button>
        }
      </article>
    }
  `,
  styles: `
    .become { display: flex; flex-direction: column; gap: 0.75rem; margin-top: 1.5rem; padding: 1.1rem 1.25rem; border: 1px solid var(--border); border-radius: 12px; background: var(--surface); }
    h2 { margin: 0; font-size: 1.05rem; }
    p { margin: 0; font-size: 0.9rem; }
    .muted { color: var(--muted); }
    .state { padding: 0.65rem 0.8rem; border-radius: 8px; }
    .pending { background: var(--accent-soft, #eff6ff); color: var(--accent-strong, #1e40af); }
    .rejected { background: #fef2f2; color: var(--danger, #b91c1c); white-space: pre-line; }
    .motivation { padding: 0.6rem 0.75rem; border-radius: 8px; background: var(--bg); white-space: pre-line; }
    button { align-self: flex-start; padding: 0.55rem 1.1rem; border: 0; border-radius: 8px; background: var(--accent, #2563eb); color: #fff; font: inherit; font-weight: 600; cursor: pointer; }
    button:disabled { opacity: 0.5; cursor: not-allowed; }
    .error { color: var(--danger, #b91c1c); }
  `,
})
export class BecomeInstructorComponent implements OnInit {
  private readonly api = inject(InstructorRequestApiService);
  private readonly translate = inject(TranslateService);
  readonly lang = inject(LanguageService);

  readonly loading = signal(true);
  readonly request = signal<InstructorRequest | null>(null);
  readonly sending = signal(false);
  readonly tried = signal(false);
  private readonly application = viewChild(InstructorApplicationComponent);
  /** L'inscription a créé le compte mais l'envoi de la candidature a échoué : on invite à la renvoyer. */
  readonly failedAtSignup = !!(history.state as { instructorRequestFailed?: boolean } | null)?.instructorRequestFailed;
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.api.latest().subscribe({
      next: (request) => {
        this.request.set(request);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  submit(): void {
    this.tried.set(true);
    const application = this.application();
    if (!application?.valid()) return;
    this.sending.set(true);
    this.error.set(null);
    this.api.submit(application.formData()).subscribe({
      next: (request) => {
        this.request.set(request);
        this.sending.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(err.error?.message ?? this.translate.instant('account.instructorRequest.error'));
        this.sending.set(false);
      },
    });
  }
}
