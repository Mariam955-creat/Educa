import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { debounceTime } from 'rxjs';

import { CourseApiService } from '../../core/courses/course-api.service';
import {
  CATALOG_SORTS,
  COURSE_CATEGORIES,
  COURSE_LEVELS,
  CatalogFilters,
  CatalogSort,
  CourseCategory,
  CourseLevel,
  CourseSummary,
} from '../../core/courses/course.models';
import { LanguageService } from '../../core/i18n/language.service';
import { MoneyPipe } from '../../core/i18n/money.pipe';
import { CourseLanguage, LanguageApiService } from '../../core/language/language-api.service';
import { CourseCardComponent } from '../../shared/course-card/course-card.component';
import { PagerComponent } from '../../shared/pager/pager.component';

const PAGE_SIZE = 12;

/**
 * Catalogue public : recherche, filtres, tri et pagination. Les critères vivent dans l'URL
 * (`/catalog?category=DESIGN&sort=PRICE_ASC`) : la page se partage, se met en favori et suit le bouton Retour.
 */
@Component({
  selector: 'app-catalog',
  imports: [ReactiveFormsModule, TranslatePipe, MoneyPipe, CourseCardComponent, PagerComponent],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.scss',
})
export class CatalogComponent implements OnInit {
  private readonly api = inject(CourseApiService);
  private readonly languageApi = inject(LanguageApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly lang = inject(LanguageService);

  readonly categories = COURSE_CATEGORIES;
  readonly levels = COURSE_LEVELS;
  readonly sorts = CATALOG_SORTS;
  readonly priceCaps = [15, 20, 30, 40];
  readonly ratingFloors = [4, 4.5];

  readonly search = new FormControl('', { nonNullable: true });
  readonly filters = new FormGroup({
    category: new FormControl<CourseCategory | ''>('', { nonNullable: true }),
    level: new FormControl<CourseLevel | ''>('', { nonNullable: true }),
    language: new FormControl('', { nonNullable: true }),
    maxPrice: new FormControl('', { nonNullable: true }),
    minRating: new FormControl('', { nonNullable: true }),
    promo: new FormControl(false, { nonNullable: true }),
    sort: new FormControl<CatalogSort>('NEWEST', { nonNullable: true }),
  });

  readonly courses = signal<CourseSummary[]>([]);
  readonly courseLanguages = signal<CourseLanguage[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly loading = signal(true);
  readonly error = signal(false);
  /** Nombre de critères qui restreignent la liste (le tri n'en est pas un). */
  readonly activeCount = signal(0);
  /** Panneau des filtres ouvert (téléphone ; toujours visible sur grand écran). */
  readonly filtersOpen = signal(false);

  ngOnInit(): void {
    this.languageApi.active().subscribe({
      next: (languages) => this.courseLanguages.set(languages),
      error: () => this.courseLanguages.set([]),
    });

    // L'URL est la source de vérité : chaque changement de critère la modifie, puis on recharge depuis elle
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      this.applyToForm(params);
      this.load(params);
    });

    this.search.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.navigate(true));
    this.filters.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.navigate(false));
  }

  changePage(page: number): void {
    this.navigate(false, page);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  reset(): void {
    this.router.navigate([], { relativeTo: this.route, queryParams: {} });
  }

  toggleFilters(): void {
    this.filtersOpen.update((open) => !open);
  }

  private navigate(replaceUrl: boolean, page = 0): void {
    const f = this.filters.getRawValue();
    const queryParams: Record<string, string | number | null> = {
      q: this.search.value.trim() || null,
      category: f.category || null,
      level: f.level || null,
      language: f.language || null,
      maxPrice: f.maxPrice || null,
      minRating: f.minRating || null,
      promo: f.promo ? 'true' : null,
      sort: f.sort === 'NEWEST' ? null : f.sort,
      page: page > 0 ? page + 1 : null,
    };
    this.router.navigate([], { relativeTo: this.route, queryParams, replaceUrl });
  }

  /** Recopie l'URL dans le formulaire sans redéclencher de navigation. */
  private applyToForm(params: ParamMap): void {
    const q = params.get('q') ?? '';
    if (this.search.value !== q) {
      this.search.setValue(q, { emitEvent: false });
    }
    this.filters.setValue(
      {
        category: this.oneOf(params.get('category'), this.categories) ?? '',
        level: this.oneOf(params.get('level'), this.levels) ?? '',
        language: params.get('language') ?? '',
        maxPrice: params.get('maxPrice') ?? '',
        minRating: params.get('minRating') ?? '',
        promo: params.get('promo') === 'true',
        sort: this.oneOf(params.get('sort'), this.sorts) ?? 'NEWEST',
      },
      { emitEvent: false },
    );
  }

  private load(params: ParamMap): void {
    const f = this.filters.getRawValue();
    const criteria: CatalogFilters = {
      q: this.search.value.trim() || undefined,
      category: f.category || undefined,
      level: f.level || undefined,
      language: f.language || undefined,
      maxPrice: f.maxPrice ? Number(f.maxPrice) : undefined,
      minRating: f.minRating ? Number(f.minRating) : undefined,
      promo: f.promo || undefined,
      sort: f.sort,
    };
    const page = Math.max(Number(params.get('page')) - 1, 0) || 0;
    this.activeCount.set(this.countActive());
    this.loading.set(true);
    this.error.set(false);
    this.api.catalog(criteria, page, PAGE_SIZE).subscribe({
      next: (result) => {
        this.courses.set(result.content);
        this.total.set(result.totalElements);
        this.page.set(result.page);
        this.totalPages.set(result.totalPages);
        this.loading.set(false);
      },
      error: () => {
        this.courses.set([]);
        this.total.set(0);
        this.totalPages.set(0);
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  private countActive(): number {
    const f = this.filters.getRawValue();
    return [this.search.value.trim(), f.category, f.level, f.language, f.maxPrice, f.minRating, f.promo].filter(
      Boolean,
    ).length;
  }

  private oneOf<T extends string>(value: string | null, allowed: readonly T[]): T | undefined {
    return allowed.find((item) => item === value);
  }
}
