import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { CourseApiService } from '../../core/courses/course-api.service';
import { CourseRating, CourseReview } from '../../core/courses/course.models';
import { intlLocale } from '../../core/i18n/intl-locale';
import { LanguageService } from '../../core/i18n/language.service';
import { LocalDatePipe } from '../../core/i18n/local-date.pipe';
import { StarRatingComponent } from '../../shared/star-rating/star-rating.component';

/** Section « Avis des apprenants » de la page cours : moyenne, formulaire (inscrits) et liste des avis écrits. */
@Component({
  selector: 'app-course-reviews',
  imports: [FormsModule, TranslatePipe, LocalDatePipe, StarRatingComponent],
  templateUrl: './course-reviews.component.html',
  styleUrl: './course-reviews.component.scss',
})
export class CourseReviewsComponent implements OnInit {
  private readonly api = inject(CourseApiService);
  readonly lang = inject(LanguageService);

  readonly courseId = input.required<number>();
  /** Progression courante de l'apprenant (page cours) : le formulaire s'ouvre dès le seuil atteint, sans recharger. */
  readonly progress = input(0);
  /** Émis après publication d'un avis, pour mettre à jour la note affichée en tête de page. */
  readonly ratingChange = output<CourseRating>();

  readonly rating = signal<CourseRating | null>(null);
  readonly saving = signal(false);
  readonly saved = signal(false);
  /** Formulaire : étoiles choisies et texte, pré-remplis avec l'avis existant de l'apprenant. */
  readonly draftStars = signal(0);
  readonly draftComment = signal('');
  readonly reviews = signal<CourseReview[]>([]);
  readonly hasMore = signal(false);
  private page = 0;

  /** Progression la plus à jour : celle de la page (suivie en direct) ou celle renvoyée avec la note (chargée en premier). */
  readonly currentProgress = computed(() => Math.max(this.progress(), this.rating()?.progressPercent ?? 0));
  /** Inscrit mais pas encore assez avancé pour noter. */
  readonly progressTooLow = computed(() => {
    const r = this.rating();
    return !!r && r.enrolled && this.currentProgress() < r.requiredProgress;
  });
  readonly canRate = computed(() => {
    const r = this.rating();
    return !!r && r.enrolled && this.currentProgress() >= r.requiredProgress;
  });

  ngOnInit(): void {
    this.api.rating(this.courseId()).subscribe({ next: (r) => this.setRating(r), error: () => undefined });
    this.loadReviews(true);
  }

  submit(): void {
    const stars = this.draftStars();
    if (!stars) return;
    this.saving.set(true);
    this.api.rate(this.courseId(), stars, this.draftComment().trim() || undefined).subscribe({
      next: (rating) => {
        this.setRating(rating);
        this.ratingChange.emit(rating);
        this.loadReviews(true);
        this.saving.set(false);
        this.saved.set(true);
        setTimeout(() => this.saved.set(false), 2500);
      },
      error: () => this.saving.set(false),
    });
  }

  loadMore(): void {
    this.loadReviews(false);
  }

  /** Note moyenne dans la langue de l'interface : « 4,5 » (fr), « 4.5 » (en). */
  formatRating(value: number): string {
    return new Intl.NumberFormat(intlLocale(this.lang.current()), {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    }).format(value);
  }

  private setRating(rating: CourseRating): void {
    this.rating.set(rating);
    this.draftStars.set(rating.myStars ?? 0);
    this.draftComment.set(rating.myComment ?? '');
  }

  /** @param reset `true` : recharge la première page (après publication d'un avis) ; `false` : page suivante. */
  private loadReviews(reset: boolean): void {
    const page = reset ? 0 : this.page + 1;
    this.api.reviews(this.courseId(), page).subscribe({
      next: (res) => {
        this.page = res.page;
        this.reviews.set(reset ? res.content : [...this.reviews(), ...res.content]);
        this.hasMore.set(res.page + 1 < res.totalPages);
      },
      error: () => undefined,
    });
  }
}
