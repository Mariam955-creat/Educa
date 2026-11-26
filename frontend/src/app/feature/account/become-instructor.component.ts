import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal, viewChild } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { InstructorRequest } from '../../core/admin/admin.models';
import { LanguageService } from '../../core/i18n/language.service';
import { LocalDatePipe } from '../../core/i18n/local-date.pipe';
import { InstructorRequestApiService } from '../../core/instructor/instructor-request-api.service';
import { InstructorApplicationComponent } from '../../shared/instructor-application/instructor-application.component';

/**
 * Candidature formateur dans le profil d'un apprenant : état de la demande (en attente, refusée avec le motif),
 * pièces envoyées, et formulaire — vierge pour une première demande, prérempli pour corriger et renvoyer après
 * un refus (les pièces non remplacées sont reprises par le serveur).
 */
@Component({
  selector: 'app-become-instructor',
  imports: [TranslatePipe, LocalDatePipe, InstructorApplicationComponent],
  template: `
    @if (!loading()) {
      <article class="become" id="instructor-request">
        <div class="head">
          <h2>🧑‍🏫 {{ 'account.instructorRequest.title' | translate }}</h2>
          @if (request(); as r) {
            <span class="status" [class]="r.status.toLowerCase()">{{ 'admin.requests.status.' + r.status | translate }}</span>
          }
        </div>

        @if (request(); as r) {
          <p class="meta">
            {{ 'admin.requests.sentOn' | translate: { date: (r.createdAt | localDate: 'long' : lang.current()) } }}
            @if (r.decidedAt) {
              · {{ 'admin.requests.decidedOn' | translate: { date: (r.decidedAt | localDate: 'long' : lang.current()) } }}
            }
          </p>
        }

        @if (request()?.status === 'PENDING') {
          <p class="state pending">{{ 'account.instructorRequest.pendingInfo' | translate }}</p>
          <h3>{{ 'account.instructorRequest.sent' | translate }}</h3>
          @if (request()!.motivation) {
            <p class="motivation">{{ request()!.motivation }}</p>
          }
          <ul class="docs">
            @for (d of request()!.documents; track d.id) {
              <li><span>{{ 'admin.requests.documents.' + d.kind | translate }}</span> {{ d.originalName }}</li>
            }
          </ul>
        } @else if (request()?.status === 'APPROVED') {
          <p class="state approved">{{ 'account.instructorRequest.approvedInfo' | translate }}</p>
        } @else {
          @if (request()?.status === 'REJECTED') {
            <div class="state rejected">
              <strong>{{ 'account.instructorRequest.rejected' | translate }}</strong>
              @if (request()!.rejectionReason) {
                <p><strong>{{ 'admin.requests.reason' | translate }} :</strong> {{ request()!.rejectionReason }}</p>
              }
            </div>
            <h3>{{ 'account.instructorRequest.fixTitle' | translate }}</h3>
            <p class="muted">{{ 'account.instructorRequest.fixIntro' | translate }}</p>
          } @else {
            @if (failedAtSignup) {
              <p class="state rejected">{{ 'account.instructorRequest.failedAtSignup' | translate }}</p>
            }
            <p class="muted">{{ 'account.instructorRequest.intro' | translate }}</p>
          }
          <app-instructor-application [showErrors]="tried()" [previous]="rejected()" />
          @if (error()) {
            <p class="error">{{ error() }}</p>
          }
          <button type="button" [disabled]="sending()" (click)="submit()">
            {{ (request()?.status === 'REJECTED' ? 'account.instructorRequest.resubmit' : 'account.instructorRequest.submit') | translate }}
          </button>
        }
      </article>
    }
  `,
  styles: `
    .become { display: flex; flex-direction: column; gap: 0.75rem; margin-top: 1.5rem; padding: 1.1rem 1.25rem; border: 1px solid var(--border); border-radius: 12px; background: var(--surface); }
    .head { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 0.5rem; }
    h2 { margin: 0; font-size: 1.05rem; }
    h3 { margin: 0.25rem 0 0; font-size: 0.92rem; }
    p { margin: 0; font-size: 0.9rem; }
    .meta, .muted { color: var(--muted); font-size: 0.82rem; }
    .status { padding: 0.2rem 0.65rem; border-radius: 999px; font-size: 0.78rem; font-weight: 700; }
    .status.pending { background: #fef3c7; color: #92400e; }
    .status.approved { background: var(--success-soft, #d1fae5); color: var(--success-strong, #047857); }
    .status.rejected { background: #fee2e2; color: var(--danger, #b91c1c); }
    .state { display: flex; flex-direction: column; gap: 0.35rem; padding: 0.7rem 0.85rem; border-radius: 8px; font-size: 0.9rem; }
    .state.pending { background: var(--accent-soft, #eff6ff); color: var(--accent-strong, #1e40af); }
    .state.approved { background: var(--success-soft, #d1fae5); color: var(--success-strong, #047857); }
    .state.rejected { background: #fef2f2; color: var(--danger, #b91c1c); white-space: pre-line; overflow-wrap: anywhere; }
    .motivation { max-height: 8rem; overflow-y: auto; padding: 0.6rem 0.75rem; border-radius: 8px; background: var(--bg); white-space: pre-line; overflow-wrap: anywhere; }
    .docs { display: flex; flex-direction: column; gap: 0.25rem; margin: 0; padding: 0; list-style: none; font-size: 0.85rem; }
    .docs li { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .docs span { display: inline-block; min-width: 8.5rem; color: var(--muted); font-weight: 600; }
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
  /** Demande refusée, transmise au formulaire pour le préremplir. */
  readonly rejected = signal<InstructorRequest | null>(null);
  readonly sending = signal(false);
  readonly tried = signal(false);
  readonly error = signal<string | null>(null);
  private readonly application = viewChild(InstructorApplicationComponent);
  /** L'inscription a créé le compte mais l'envoi de la candidature a échoué : on invite à la renvoyer. */
  readonly failedAtSignup = !!(history.state as { instructorRequestFailed?: boolean } | null)?.instructorRequestFailed;

  ngOnInit(): void {
    this.api.latest().subscribe({
      next: (request) => this.show(request),
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
        this.sending.set(false);
        this.tried.set(false);
        this.show(request);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(err.error?.message ?? this.translate.instant('account.instructorRequest.error'));
        this.sending.set(false);
      },
    });
  }

  private show(request: InstructorRequest | null): void {
    this.request.set(request);
    this.rejected.set(request?.status === 'REJECTED' ? request : null);
    this.loading.set(false);
  }
}
