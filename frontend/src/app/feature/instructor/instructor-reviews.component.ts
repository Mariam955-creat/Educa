import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { intlLocale } from '../../core/i18n/intl-locale';
import { LanguageService } from '../../core/i18n/language.service';
import { LocalDatePipe } from '../../core/i18n/local-date.pipe';
import { InstructorApiService, ReceivedReview } from '../../core/instructor/instructor-api.service';
import { StarRatingComponent } from '../../shared/star-rating/star-rating.component';

/** « Avis reçus » de l'espace formateur : moyenne, répartition des étoiles, avis filtrables par cours. */
@Component({
  selector: 'app-instructor-reviews',
  imports: [RouterLink, TranslatePipe, LocalDatePipe, StarRatingComponent],
  template: `
    <section class="page">
      <h1>{{ 'instructor.nav.reviews' | translate }}</h1>

      @if (loading()) {
        <p class="muted">{{ 'common.loading' | translate }}</p>
      } @else if (reviews().length === 0) {
        <p class="muted">{{ 'instructor.overview.noReviews' | translate }}</p>
      } @else {
        <div class="summary">
          <div class="average">
            <strong>{{ formatRating(average()) }}</strong>
            <app-star-rating [value]="average()" [label]="formatRating(average()) + ' / 5'" />
            <span class="muted">{{ 'instructor.overview.ratingCount' | translate: { count: filtered().length } }}</span>
          </div>
          <div class="distribution">
            @for (row of distribution(); track row.stars) {
              <div class="dist-row">
                <span>{{ row.stars }} ★</span>
                <span class="bar"><span class="bar-fill" [style.width.%]="row.percent"></span></span>
                <span class="muted">{{ row.count }}</span>
              </div>
            }
          </div>
        </div>

        <select class="course-filter" [value]="courseFilter()" (change)="courseFilter.set($any($event.target).value)">
          <option value="">{{ 'instructor.reviews.allCourses' | translate }}</option>
          @for (c of courseOptions(); track c.id) {
            <option [value]="c.id">{{ c.title }}</option>
          }
        </select>

        @for (r of filtered(); track r.id) {
          <article class="review">
            <div class="review-head">
              <span class="avatar" aria-hidden="true">{{ r.authorName.charAt(0) }}</span>
              <div>
                <strong>{{ r.authorName }}</strong>
                <div class="meta">
                  <app-star-rating [value]="r.stars" size="small" [label]="r.stars + ' / 5'" />
                  <span>{{ r.updatedAt | localDate: 'long' : lang.current() }}</span>
                  <a [routerLink]="['/courses', r.courseSlug]">{{ r.courseTitle }}</a>
                </div>
              </div>
            </div>
            @if (r.comment) {
              <p>{{ r.comment }}</p>
            }
          </article>
        }
      }
    </section>
  `,
  styles: `
    .page { max-width: 900px; margin: 0 auto; padding: 2rem 1.25rem; }
    h1 { margin: 0 0 1.25rem; font-size: 1.5rem; }
    .muted { color: var(--muted); }
    .summary { display: flex; flex-wrap: wrap; gap: 2rem; align-items: center; padding: 1.25rem; border: 1px solid var(--border); border-radius: 12px; background: var(--surface); }
    .average { display: flex; flex-direction: column; align-items: center; gap: 0.25rem; }
    .average strong { color: #b45309; font-size: 2.6rem; line-height: 1; }
    .average .muted { font-size: 0.8rem; }
    .distribution { display: flex; flex: 1; flex-direction: column; gap: 0.35rem; min-width: 220px; }
    .dist-row { display: grid; grid-template-columns: 2.5rem 1fr 2rem; align-items: center; gap: 0.6rem; font-size: 0.85rem; }
    .bar { height: 8px; overflow: hidden; border-radius: 999px; background: var(--border); }
    .bar-fill { display: block; height: 100%; background: #f59e0b; }
    .course-filter { margin: 1.25rem 0 0.5rem; padding: 0.5rem 0.75rem; border: 1px solid var(--border); border-radius: 8px; font: inherit; }
    .review { padding: 1rem 0; border-bottom: 1px solid var(--border); }
    .review-head { display: flex; align-items: center; gap: 0.75rem; }
    .avatar { display: grid; place-items: center; flex-shrink: 0; width: 2.25rem; height: 2.25rem; border-radius: 50%; background: #f3e8ff; color: var(--accent); font-weight: 800; text-transform: uppercase; }
    .meta { display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem; color: var(--muted); font-size: 0.78rem; }
    .review p { margin: 0.6rem 0 0; line-height: 1.55; white-space: pre-line; }
  `,
})
export class InstructorReviewsComponent implements OnInit {
  private readonly api = inject(InstructorApiService);
  readonly lang = inject(LanguageService);

  readonly reviews = signal<ReceivedReview[]>([]);
  readonly loading = signal(true);
  /** Id du cours filtré (chaîne vide = tous). */
  readonly courseFilter = signal('');

  readonly courseOptions = computed(() => {
    const map = new Map<number, string>();
    for (const r of this.reviews()) map.set(r.courseId, r.courseTitle);
    return [...map.entries()].map(([id, title]) => ({ id, title }));
  });
  readonly filtered = computed(() =>
    this.courseFilter() ? this.reviews().filter((r) => String(r.courseId) === this.courseFilter()) : this.reviews(),
  );
  readonly average = computed(() => {
    const list = this.filtered();
    return list.length ? list.reduce((sum, r) => sum + r.stars, 0) / list.length : 0;
  });
  /** Répartition 5 → 1 étoile, en pourcentage des avis affichés. */
  readonly distribution = computed(() => {
    const list = this.filtered();
    return [5, 4, 3, 2, 1].map((stars) => {
      const count = list.filter((r) => r.stars === stars).length;
      return { stars, count, percent: list.length ? (count / list.length) * 100 : 0 };
    });
  });

  ngOnInit(): void {
    this.api.reviews().subscribe({
      next: (list) => {
        this.reviews.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  formatRating(value: number): string {
    return new Intl.NumberFormat(intlLocale(this.lang.current()), {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    }).format(value);
  }
}
