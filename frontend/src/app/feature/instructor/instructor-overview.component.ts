import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, catchError, forkJoin, of } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { CourseApiService } from '../../core/courses/course-api.service';
import { CourseSummary } from '../../core/courses/course.models';
import { intlLocale } from '../../core/i18n/intl-locale';
import { LanguageService } from '../../core/i18n/language.service';
import { LocalDatePipe } from '../../core/i18n/local-date.pipe';
import { MoneyPipe } from '../../core/i18n/money.pipe';
import { InstructorApiService, InstructorSale, ReceivedReview } from '../../core/instructor/instructor-api.service';
import { StarRatingComponent } from '../../shared/star-rating/star-rating.component';

const DAY_MS = 24 * 60 * 60 * 1000;

/** Tableau de bord formateur : revenus, ventes, apprenants, note, et derniers événements (ventes, avis). */
@Component({
  selector: 'app-instructor-overview',
  imports: [RouterLink, TranslatePipe, MoneyPipe, LocalDatePipe, StarRatingComponent],
  templateUrl: './instructor-overview.component.html',
  styleUrl: './instructor-overview.component.scss',
})
export class InstructorOverviewComponent implements OnInit {
  private readonly courseApi = inject(CourseApiService);
  private readonly instructorApi = inject(InstructorApiService);
  readonly auth = inject(AuthService);
  readonly lang = inject(LanguageService);

  readonly loading = signal(true);
  readonly courses = signal<CourseSummary[]>([]);
  readonly sales = signal<InstructorSale[]>([]);
  readonly reviews = signal<ReceivedReview[]>([]);

  readonly currency = computed(() => this.sales()[0]?.currency ?? 'EUR');
  readonly revenue = computed(() => this.sales().reduce((sum, s) => sum + s.amount, 0));
  /** Revenus des 30 derniers jours. */
  readonly revenue30 = computed(() => {
    const since = Date.now() - 30 * DAY_MS;
    return this.sales()
      .filter((s) => new Date(s.createdAt).getTime() >= since)
      .reduce((sum, s) => sum + s.amount, 0);
  });
  readonly learners = computed(() => this.courses().reduce((sum, c) => sum + c.learnerCount, 0));
  readonly publishedCount = computed(() => this.courses().filter((c) => c.published).length);
  readonly ratingCount = computed(() => this.courses().reduce((sum, c) => sum + c.ratingCount, 0));
  /** Moyenne pondérée par le nombre de notes de chaque cours ; `null` si aucune note. */
  readonly averageRating = computed(() => {
    const count = this.ratingCount();
    return count
      ? this.courses().reduce((sum, c) => sum + (c.averageRating ?? 0) * c.ratingCount, 0) / count
      : null;
  });
  /** Cours les plus suivis. */
  readonly topCourses = computed(() =>
    [...this.courses()].sort((a, b) => b.learnerCount - a.learnerCount).slice(0, 4),
  );
  readonly latestSales = computed(() => this.sales().slice(0, 5));
  readonly latestReviews = computed(() => this.reviews().slice(0, 3));

  ngOnInit(): void {
    forkJoin({
      courses: orEmpty(this.courseApi.myCourses()),
      sales: orEmpty(this.instructorApi.sales()),
      reviews: orEmpty(this.instructorApi.reviews()),
    }).subscribe((data) => {
      this.courses.set(data.courses);
      this.sales.set(data.sales);
      this.reviews.set(data.reviews);
      this.loading.set(false);
    });
  }

  formatRating(value: number): string {
    return new Intl.NumberFormat(intlLocale(this.lang.current()), {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    }).format(value);
  }
}

function orEmpty<T>(source: Observable<T[]>): Observable<T[]> {
  return source.pipe(catchError(() => of<T[]>([])));
}
