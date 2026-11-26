import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { AdminApiService } from '../../core/admin/admin-api.service';
import { CourseApiService } from '../../core/courses/course-api.service';
import { CourseSummary } from '../../core/courses/course.models';
import { LanguageService } from '../../core/i18n/language.service';
import { LocalDatePipe } from '../../core/i18n/local-date.pipe';
import { MoneyPipe } from '../../core/i18n/money.pipe';
import { PagerComponent } from '../../shared/pager/pager.component';
import { ModerationCountService } from './moderation-count.service';

const PAGE_SIZE = 20;

/**
 * Cours soumis par les formateurs, en attente de validation (les plus anciens d'abord). L'admin ouvre l'aperçu du
 * cours, puis le valide (il est publié) ou le refuse avec un motif ; le formateur est notifié et reçoit un email.
 */
@Component({
  selector: 'app-admin-course-reviews',
  imports: [RouterLink, TranslatePipe, LocalDatePipe, MoneyPipe, PagerComponent],
  template: `
    <section class="page">
      <div class="head">
        <div>
          <h1>{{ 'admin.nav.courseReviews' | translate }}</h1>
          <p class="subtitle">{{ 'admin.courseReviews.pendingCount' | translate: { count: count.courseReviews() } }}</p>
        </div>
      </div>

      @if (message(); as m) {
        <p class="flash" [class.ok-flash]="!m.error">{{ m.text }}</p>
      }

      @if (loading()) {
        <p class="muted">{{ 'common.loading' | translate }}</p>
      } @else if (courses().length === 0) {
        <p class="muted">{{ 'admin.courseReviews.empty' | translate }}</p>
      } @else {
        <div class="cards">
          @for (c of courses(); track c.id) {
            <article class="review">
              <div class="top">
                <span class="cover" aria-hidden="true">
                  @if (courseApi.coverSrc(c.coverImageUrl); as src) {
                    <img [src]="src" alt="" loading="lazy" />
                  } @else {
                    {{ c.title.charAt(0) }}
                  }
                </span>
                <div class="titles">
                  <strong>{{ c.title }}</strong>
                  @if (c.subtitle) {
                    <small>{{ c.subtitle }}</small>
                  }
                </div>
              </div>

              <dl class="facts">
                <div><dt>{{ 'admin.courseReviews.instructor' | translate }}</dt><dd>{{ c.instructorName }}</dd></div>
                <div>
                  <dt>{{ 'admin.courseReviews.price' | translate }}</dt>
                  <dd>{{ c.price === 0 ? ('course.free' | translate) : (c.price | money: 'EUR' : lang.current()) }}</dd>
                </div>
                <div>
                  <dt>{{ 'admin.courseReviews.content' | translate }}</dt>
                  <dd>{{ (c.chapterCount === 1 ? 'catalog.chapterOne' : 'catalog.chapters') | translate: { count: c.chapterCount } }} · {{ c.language.toUpperCase() }}</dd>
                </div>
                @if (c.submittedAt) {
                  <div><dt>{{ 'admin.courseReviews.submitted' | translate }}</dt><dd>{{ c.submittedAt | localDate: 'long' : lang.current() }}</dd></div>
                }
              </dl>

              @if (c.description) {
                <p class="description">{{ c.description }}</p>
              }

              <a class="preview" [routerLink]="['/courses', c.slug]" target="_blank">👁 {{ 'admin.courseReviews.preview' | translate }}</a>

              @if (rejecting() === c.id) {
                <label class="reject-box">
                  {{ 'admin.courseReviews.reasonLabel' | translate }}
                  <textarea rows="3" maxlength="2000" [value]="reason()" (input)="reason.set($any($event.target).value)"
                            [placeholder]="'admin.courseReviews.reasonPlaceholder' | translate"></textarea>
                </label>
                <div class="decision">
                  <button type="button" (click)="cancelReject()">{{ 'trash.cancel' | translate }}</button>
                  <button type="button" class="danger" [disabled]="!reason().trim() || busy()" (click)="confirmReject(c)">
                    {{ 'admin.requests.confirmReject' | translate }}
                  </button>
                </div>
              } @else {
                <div class="decision">
                  <button type="button" class="danger" [disabled]="busy()" (click)="startReject(c)">{{ 'admin.requests.reject' | translate }}</button>
                  <button type="button" class="approve" [disabled]="busy()" (click)="approve(c)">✓ {{ 'admin.courseReviews.approve' | translate }}</button>
                </div>
              }
            </article>
          }
        </div>
        <app-pager [page]="page()" [totalPages]="totalPages()" (pageChange)="load($event)" />
      }
    </section>
  `,
  styleUrl: './admin-shared.scss',
  styles: `
    .flash.ok-flash { border-color: #a7f3d0; background: var(--success-soft, #d1fae5); color: var(--success-strong, #047857); }
    .cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(340px, 100%), 1fr)); gap: 1rem; }
    .review { display: flex; flex-direction: column; gap: 0.75rem; min-width: 0; padding: 1.1rem; border: 1px solid var(--border); border-radius: 12px; background: var(--surface); box-shadow: var(--shadow-card, 0 1px 2px rgba(31, 41, 55, 0.05)); }
    .top { display: flex; align-items: center; gap: 0.75rem; min-width: 0; }
    .cover { display: grid; flex-shrink: 0; place-items: center; width: 64px; height: 40px; overflow: hidden; border-radius: 6px; background: var(--accent-soft, #eff6ff); color: var(--accent, #2563eb); font-weight: 800; }
    .cover img { width: 100%; height: 100%; object-fit: cover; }
    .titles { display: flex; flex-direction: column; min-width: 0; }
    .titles strong, .titles small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .titles small { color: var(--muted); font-size: 0.8rem; }
    .facts { display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem 1rem; margin: 0; }
    .facts dt { color: var(--muted); font-size: 0.72rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.03em; }
    .facts dd { margin: 0; font-size: 0.85rem; overflow-wrap: anywhere; }
    .description { max-height: 6rem; overflow-y: auto; margin: 0; padding: 0.6rem 0.75rem; border-radius: 8px; background: var(--bg); font-size: 0.85rem; line-height: 1.45; white-space: pre-line; overflow-wrap: anywhere; }
    .preview { align-self: flex-start; font-size: 0.85rem; font-weight: 600; text-decoration: none; }
    .reject-box { display: flex; flex-direction: column; gap: 0.35rem; font-size: 0.85rem; font-weight: 600; }
    .reject-box textarea { padding: 0.5rem 0.65rem; border: 1px solid var(--border); border-radius: 8px; font: inherit; font-weight: 400; resize: vertical; }
    .decision { display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem; margin-top: auto; }
    .decision button { padding: 0.55rem 0.75rem; border: 1px solid var(--border); border-radius: 8px; background: transparent; color: inherit; font: inherit; font-size: 0.85rem; font-weight: 600; cursor: pointer; }
    .decision button:disabled { opacity: 0.45; cursor: not-allowed; }
    .decision .danger { border-color: #fecaca; color: var(--danger, #b91c1c); }
    .decision .approve { border-color: var(--success, #10b981); background: var(--success, #10b981); color: #fff; }
  `,
})
export class AdminCourseReviewsComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly translate = inject(TranslateService);
  readonly courseApi = inject(CourseApiService);
  readonly count = inject(ModerationCountService);
  readonly lang = inject(LanguageService);

  readonly courses = signal<CourseSummary[]>([]);
  readonly loading = signal(true);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly busy = signal(false);
  readonly rejecting = signal<number | null>(null);
  readonly reason = signal('');
  readonly message = signal<{ text: string; error: boolean } | null>(null);

  ngOnInit(): void {
    this.load(0);
  }

  load(page: number): void {
    this.loading.set(true);
    this.api.courseReviews(page, PAGE_SIZE).subscribe({
      next: (res) => {
        this.courses.set(res.content);
        this.page.set(res.page);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.count.refresh();
  }

  approve(course: CourseSummary): void {
    this.busy.set(true);
    this.api.approveCourse(course.id).subscribe({
      next: () => this.done(this.translate.instant('admin.courseReviews.approved', { name: course.title })),
      error: (err: HttpErrorResponse) => this.fail(err),
    });
  }

  startReject(course: CourseSummary): void {
    this.rejecting.set(course.id);
    this.reason.set('');
  }

  cancelReject(): void {
    this.rejecting.set(null);
    this.reason.set('');
  }

  confirmReject(course: CourseSummary): void {
    const reason = this.reason().trim();
    if (!reason) return;
    this.busy.set(true);
    this.api.rejectCourse(course.id, reason).subscribe({
      next: () => this.done(this.translate.instant('admin.courseReviews.rejected', { name: course.title })),
      error: (err: HttpErrorResponse) => this.fail(err),
    });
  }

  private done(text: string): void {
    this.busy.set(false);
    this.cancelReject();
    this.flash(text, false);
    this.load(this.page());
  }

  private fail(err: HttpErrorResponse): void {
    this.busy.set(false);
    this.flash(err.error?.message ?? this.translate.instant('admin.users.updateError'), true);
    this.load(this.page());
  }

  private flash(text: string, error: boolean): void {
    this.message.set({ text, error });
    setTimeout(() => this.message.set(null), 4000);
  }
}
