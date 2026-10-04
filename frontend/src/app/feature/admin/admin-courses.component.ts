import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';

import { AdminApiService } from '../../core/admin/admin-api.service';
import { CourseApiService } from '../../core/courses/course-api.service';
import { CourseSummary } from '../../core/courses/course.models';
import { LanguageService } from '../../core/i18n/language.service';
import { MoneyPipe } from '../../core/i18n/money.pipe';
import { fallbackCover } from '../../shared/course-cover';
import { PagerComponent } from '../../shared/pager/pager.component';

type StatusFilter = 'all' | 'published' | 'draft';
const PAGE_SIZE = 20;

/** Tous les cours de la plateforme : statut, formateur, apprenants, note, prix ; publier / dépublier. */
@Component({
  selector: 'app-admin-courses',
  imports: [RouterLink, TranslatePipe, MoneyPipe, PagerComponent],
  template: `
    <section class="page">
      <div class="head">
        <div>
          <h1>{{ 'admin.nav.courses' | translate }}</h1>
          <p class="subtitle">{{ 'admin.coursesPage.subtitle' | translate: { count: total() } }}</p>
        </div>
      </div>

      @if (message()) {
        <p class="flash">{{ message() }}</p>
      }

      <div class="toolbar">
        <div class="chips">
          @for (f of filters; track f) {
            <button type="button" [class.active]="filter() === f" (click)="setFilter(f)">
              {{ 'instructor.courses.filter.' + f | translate }}
            </button>
          }
        </div>
        <input class="search" type="search" [value]="query()" (input)="onSearch($any($event.target).value)"
               [placeholder]="'catalog.searchPlaceholder' | translate" />
      </div>

      @if (loading()) {
        <p class="muted">{{ 'common.loading' | translate }}</p>
      } @else if (courses().length === 0) {
        <p class="muted">{{ 'instructor.courses.noMatch' | translate }}</p>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'admin.coursesPage.course' | translate }}</th>
                <th>{{ 'admin.coursesPage.status' | translate }}</th>
                <th class="num">{{ 'instructor.overview.learners' | translate }}</th>
                <th class="num">{{ 'instructor.overview.rating' | translate }}</th>
                <th class="num">{{ 'admin.coursesPage.price' | translate }}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (c of courses(); track c.id) {
                <tr>
                  <td>
                    <span class="person">
                      <span class="thumb" [style.background]="c.coverImageUrl ? null : fallbackCover(c.id)">
                        @if (courseApi.coverSrc(c.coverImageUrl); as src) {
                          <img [src]="src" alt="" loading="lazy" />
                        } @else {
                          {{ c.title.charAt(0) }}
                        }
                      </span>
                      <span class="person-text">{{ c.title }}<small>{{ c.instructorName }} · {{ c.language.toUpperCase() }}</small></span>
                    </span>
                  </td>
                  <td>
                    <span class="badge" [class.ok]="c.published">
                      {{ (c.published ? 'instructorDashboard.published' : 'instructorDashboard.draft') | translate }}
                    </span>
                  </td>
                  <td class="num">{{ c.learnerCount }}</td>
                  <td class="num">{{ c.averageRating != null ? '⭐ ' + c.averageRating + ' (' + c.ratingCount + ')' : '—' }}</td>
                  <td class="num">{{ c.price === 0 ? ('course.free' | translate) : (c.price | money: 'EUR' : lang.current()) }}</td>
                  <td>
                    <div class="actions">
                      <a [routerLink]="['/courses', c.slug]">{{ 'instructor.courses.view' | translate }}</a>
                      <a [routerLink]="['/instructor/courses', c.slug, 'edit']">{{ 'common.edit' | translate }}</a>
                      <button type="button" [class.danger]="c.published" (click)="togglePublish(c)">
                        {{ (c.published ? 'instructorDashboard.unpublish' : 'instructorDashboard.publish') | translate }}
                      </button>
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <app-pager [page]="page()" [totalPages]="totalPages()" (pageChange)="load($event)" />
      }
    </section>
  `,
  styleUrl: './admin-shared.scss',
  styles: `
    .thumb { display: grid; place-items: center; flex-shrink: 0; width: 3.5rem; height: 2.2rem; overflow: hidden; border-radius: 6px; color: #fff; font-weight: 800; }
    .thumb img { width: 100%; height: 100%; object-fit: cover; }
  `,
})
export class AdminCoursesComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly translate = inject(TranslateService);
  readonly courseApi = inject(CourseApiService);
  readonly lang = inject(LanguageService);

  readonly filters: StatusFilter[] = ['all', 'published', 'draft'];
  readonly fallbackCover = fallbackCover;

  readonly courses = signal<CourseSummary[]>([]);
  readonly loading = signal(true);
  readonly filter = signal<StatusFilter>('all');
  readonly query = signal('');
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly total = signal(0);
  readonly message = signal<string | null>(null);

  private readonly search$ = new Subject<string>();

  ngOnInit(): void {
    this.search$.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => this.load(0));
    this.load(0);
  }

  onSearch(value: string): void {
    this.query.set(value);
    this.search$.next(value);
  }

  setFilter(filter: StatusFilter): void {
    this.filter.set(filter);
    this.load(0);
  }

  load(page: number): void {
    const published = this.filter() === 'all' ? undefined : this.filter() === 'published';
    this.loading.set(true);
    this.api.courses(this.query().trim() || undefined, published, page, PAGE_SIZE).subscribe({
      next: (res) => {
        this.courses.set(res.content);
        this.page.set(res.page);
        this.totalPages.set(res.totalPages);
        this.total.set(res.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  togglePublish(course: CourseSummary): void {
    this.courseApi.setPublished(course.id, !course.published).subscribe({
      next: (updated) => this.courses.update((list) => list.map((c) => (c.id === updated.id ? updated : c))),
      error: (err: HttpErrorResponse) => {
        this.message.set(err.error?.message ?? this.translate.instant('admin.users.updateError'));
        setTimeout(() => this.message.set(null), 3500);
      },
    });
  }
}
