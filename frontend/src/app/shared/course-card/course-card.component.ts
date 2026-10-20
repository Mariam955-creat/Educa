import { UpperCasePipe } from '@angular/common';
import { Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { CourseApiService } from '../../core/courses/course-api.service';
import { CourseSummary, discountPercent } from '../../core/courses/course.models';
import { intlLocale } from '../../core/i18n/intl-locale';
import { LanguageService } from '../../core/i18n/language.service';
import { MoneyPipe } from '../../core/i18n/money.pipe';
import { fallbackCover } from '../course-cover';
import { StarRatingComponent } from '../star-rating/star-rating.component';

/** Pictogramme affiché sur la couverture de repli, selon la catégorie du cours. */
const CATEGORY_ICONS: Record<string, string> = {
  DEVELOPMENT: '💻',
  DATA: '🗄️',
  NETWORK_SECURITY: '🔐',
  OFFICE: '📊',
  LANGUAGES: '🗣️',
  DESIGN: '🎨',
  BUSINESS: '💼',
};

/**
 * Carte d'un cours, commune au catalogue et à l'aperçu de la page d'accueil.
 * Verticale sur tablette et ordinateur, horizontale (vignette à gauche) sur téléphone.
 */
@Component({
  selector: 'app-course-card',
  imports: [RouterLink, UpperCasePipe, TranslatePipe, MoneyPipe, StarRatingComponent],
  templateUrl: './course-card.component.html',
  styleUrl: './course-card.component.scss',
})
export class CourseCardComponent {
  readonly course = input.required<CourseSummary>();

  readonly api = inject(CourseApiService);
  readonly lang = inject(LanguageService);

  readonly coverSrc = computed(() => this.api.coverSrc(this.course().coverImageUrl));
  readonly fallback = computed(() => fallbackCover(this.course().id));
  readonly discount = computed(() => {
    const c = this.course();
    return c.promoActive && c.promoPrice != null ? discountPercent(c.price, c.promoPrice) : null;
  });
  readonly icon = computed(() => CATEGORY_ICONS[this.course().category ?? ''] ?? '📘');
  readonly initials = computed(() =>
    this.course()
      .instructorName.split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0].toUpperCase())
      .join(''),
  );

  /** Note moyenne dans la langue de l'interface : « 4,5 » (fr), « 4.5 » (en). */
  formatRating(value: number): string {
    return new Intl.NumberFormat(intlLocale(this.lang.current()), {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    }).format(value);
  }
}
