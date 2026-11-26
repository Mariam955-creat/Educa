import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, output, signal } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { AuthService } from '../../core/auth/auth.service';
import { CourseApiService } from '../../core/courses/course-api.service';
import { CourseDetail } from '../../core/courses/course.models';
import { LanguageService } from '../../core/i18n/language.service';
import { LocalDatePipe } from '../../core/i18n/local-date.pipe';

/**
 * Bandeau de l'éditeur de cours : étape de validation par l'administration (brouillon, en attente, refusé avec le
 * motif, validé, publié) et action suivante (soumettre, resoumettre après correction, publier).
 */
@Component({
  selector: 'app-course-review-banner',
  imports: [TranslatePipe, LocalDatePipe],
  template: `
    @if (course(); as c) {
      <div class="banner" [class]="'banner ' + state().toLowerCase()">
        <div class="text">
          <strong>{{ 'courseReview.status.' + state() | translate }}</strong>
          <span>{{ 'courseReview.explain.' + state() | translate }}</span>
          @if (state() === 'PENDING' && c.submittedAt) {
            <small>{{ 'courseReview.submittedOn' | translate: { date: (c.submittedAt | localDate: 'long' : lang.current()) } }}</small>
          }
          @if (state() === 'REJECTED' && c.reviewReason) {
            <p class="reason"><strong>{{ 'admin.requests.reason' | translate }} :</strong> {{ c.reviewReason }}</p>
          }
          @if (error()) {
            <p class="error">{{ error() }}</p>
          }
        </div>
        @if (state() === 'DRAFT' || state() === 'REJECTED') {
          @if (isAdmin()) {
            <button type="button" [disabled]="busy()" (click)="publish(c)">{{ 'instructorDashboard.publish' | translate }}</button>
          } @else {
            <button type="button" [disabled]="busy()" (click)="submit(c)">
              📤 {{ (state() === 'REJECTED' ? 'courseReview.resubmit' : 'courseReview.submit') | translate }}
            </button>
          }
        } @else if (state() === 'APPROVED') {
          <button type="button" [disabled]="busy()" (click)="publish(c)">{{ 'instructorDashboard.publish' | translate }}</button>
        }
      </div>
    }
  `,
  styles: `
    .banner { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 0.75rem; margin: 0 0 1.25rem; padding: 0.85rem 1rem; border: 1px solid var(--border); border-left-width: 4px; border-radius: 10px; background: var(--surface); }
    .banner.draft { border-left-color: var(--muted, #6b7280); }
    .banner.pending { border-left-color: var(--warning, #f59e0b); background: #fffbeb; }
    .banner.rejected { border-left-color: var(--danger, #b91c1c); background: #fef2f2; }
    .banner.approved, .banner.published { border-left-color: var(--success, #10b981); background: var(--success-soft, #d1fae5); }
    .text { display: flex; flex-direction: column; gap: 0.25rem; flex: 1 1 18rem; min-width: 0; font-size: 0.88rem; }
    small { color: var(--muted); }
    .reason { margin: 0.25rem 0 0; color: var(--danger, #b91c1c); white-space: pre-line; overflow-wrap: anywhere; }
    .error { margin: 0; color: var(--danger, #b91c1c); font-weight: 600; }
    button { padding: 0.55rem 1rem; border: 0; border-radius: 8px; background: var(--accent, #2563eb); color: #fff; font: inherit; font-weight: 600; cursor: pointer; }
    button:disabled { opacity: 0.5; cursor: not-allowed; }
  `,
})
export class CourseReviewBannerComponent {
  private readonly api = inject(CourseApiService);
  private readonly translate = inject(TranslateService);
  private readonly auth = inject(AuthService);
  readonly lang = inject(LanguageService);

  readonly course = input<CourseDetail | null>(null);
  /** Émis après une soumission ou une publication : l'éditeur recharge le cours. */
  readonly changed = output<void>();

  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly isAdmin = computed(() => this.auth.hasRole('ADMIN'));
  readonly state = computed(() => {
    const c = this.course();
    return !c ? 'DRAFT' : c.published ? 'PUBLISHED' : c.reviewStatus;
  });

  submit(course: CourseDetail): void {
    this.run(this.api.submitForReview(course.id));
  }

  publish(course: CourseDetail): void {
    this.run(this.api.setPublished(course.id, true));
  }

  private run(request: ReturnType<CourseApiService['submitForReview']>): void {
    this.busy.set(true);
    this.error.set(null);
    request.subscribe({
      next: () => {
        this.busy.set(false);
        this.changed.emit();
      },
      error: (err: HttpErrorResponse) => {
        this.busy.set(false);
        this.error.set(err.error?.message ?? this.translate.instant('courseReview.submitError'));
      },
    });
  }
}
