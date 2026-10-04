import { UpperCasePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { CourseApiService } from '../../core/courses/course-api.service';
import { CourseSummary } from '../../core/courses/course.models';
import { intlLocale } from '../../core/i18n/intl-locale';
import { LanguageService } from '../../core/i18n/language.service';
import { MoneyPipe } from '../../core/i18n/money.pipe';
import { fallbackCover } from '../../shared/course-cover';
import { StarRatingComponent } from '../../shared/star-rating/star-rating.component';

@Component({
  selector: 'app-catalog',
  imports: [ReactiveFormsModule, RouterLink, UpperCasePipe, TranslatePipe, MoneyPipe, StarRatingComponent],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.scss',
})
export class CatalogComponent implements OnInit {
  readonly api = inject(CourseApiService);
  readonly lang = inject(LanguageService);

  readonly search = new FormControl('', { nonNullable: true });
  readonly courses = signal<CourseSummary[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.load('');
    this.search.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged())
      .subscribe((value) => this.load(value));
  }

  /** Note moyenne dans la langue de l'interface : « 4,5 » (fr), « 4.5 » (en). */
  formatRating(value: number): string {
    return new Intl.NumberFormat(intlLocale(this.lang.current()), {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    }).format(value);
  }

  readonly fallbackCover = fallbackCover;

  private load(q: string): void {
    this.loading.set(true);
    this.api.catalog(q || undefined).subscribe({
      next: (page) => {
        this.courses.set(page.content);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
