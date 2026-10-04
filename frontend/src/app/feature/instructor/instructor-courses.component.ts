import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { catchError, forkJoin, of } from 'rxjs';

import { CourseApiService } from '../../core/courses/course-api.service';
import { CourseSummary } from '../../core/courses/course.models';
import { intlLocale } from '../../core/i18n/intl-locale';
import { LanguageService } from '../../core/i18n/language.service';
import { MoneyPipe } from '../../core/i18n/money.pipe';
import { InstructorApiService, InstructorSale } from '../../core/instructor/instructor-api.service';
import { fallbackCover } from '../../shared/course-cover';

type StatusFilter = 'all' | 'published' | 'draft';

/** « Mes cours » de l'espace formateur : cartes (couverture, statut, apprenants, note, revenus) et actions. */
@Component({
  selector: 'app-instructor-courses',
  imports: [RouterLink, TranslatePipe, MoneyPipe],
  templateUrl: './instructor-courses.component.html',
  styleUrl: './instructor-courses.component.scss',
})
export class InstructorCoursesComponent implements OnInit {
  readonly api = inject(CourseApiService);
  private readonly instructorApi = inject(InstructorApiService);
  private readonly translate = inject(TranslateService);
  readonly lang = inject(LanguageService);

  readonly courses = signal<CourseSummary[]>([]);
  private readonly sales = signal<InstructorSale[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly notice = signal<string | null>(null);
  readonly filter = signal<StatusFilter>('all');
  readonly query = signal('');

  readonly fallbackCover = fallbackCover;
  readonly filters: StatusFilter[] = ['all', 'published', 'draft'];

  readonly counts = computed(() => ({
    all: this.courses().length,
    published: this.courses().filter((c) => c.published).length,
    draft: this.courses().filter((c) => !c.published).length,
  }));

  readonly visible = computed(() => {
    const q = this.query().trim().toLowerCase();
    return this.courses().filter(
      (c) =>
        (this.filter() === 'all' || c.published === (this.filter() === 'published')) &&
        (!q || c.title.toLowerCase().includes(q)),
    );
  });

  /** Revenus par cours (somme des ventes). */
  private readonly revenueByCourse = computed(() => {
    const map = new Map<number, number>();
    for (const s of this.sales()) map.set(s.courseId, (map.get(s.courseId) ?? 0) + s.amount);
    return map;
  });
  readonly currency = computed(() => this.sales()[0]?.currency ?? 'EUR');

  ngOnInit(): void {
    this.load();
  }

  revenueOf(course: CourseSummary): number {
    return this.revenueByCourse().get(course.id) ?? 0;
  }

  formatRating(value: number): string {
    return new Intl.NumberFormat(intlLocale(this.lang.current()), {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    }).format(value);
  }

  togglePublish(course: CourseSummary): void {
    this.api.setPublished(course.id, !course.published).subscribe((updated) => {
      this.courses.update((list) => list.map((c) => (c.id === updated.id ? updated : c)));
    });
  }

  /** Met le cours à la corbeille (restaurable depuis « Corbeille »). */
  remove(course: CourseSummary): void {
    if (!confirm(this.translate.instant('trash.confirmMoveCourse', { name: course.title }))) return;
    this.error.set(null);
    this.api.deleteCourse(course.id).subscribe({
      next: () => {
        this.courses.update((list) => list.filter((c) => c.id !== course.id));
        this.notice.set(this.translate.instant('trash.movedCourse', { name: course.title }));
        setTimeout(() => this.notice.set(null), 4000);
      },
      error: (err: HttpErrorResponse) =>
        this.error.set(err.error?.message ?? this.translate.instant('instructorDashboard.deleteError')),
    });
  }

  private load(): void {
    this.loading.set(true);
    forkJoin({
      courses: this.api.myCourses(),
      sales: this.instructorApi.sales().pipe(catchError(() => of<InstructorSale[]>([]))),
    }).subscribe({
      next: ({ courses, sales }) => {
        this.courses.set(courses);
        this.sales.set(sales);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
