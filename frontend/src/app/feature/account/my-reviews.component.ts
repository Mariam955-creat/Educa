import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { CourseApiService } from '../../core/courses/course-api.service';
import { MyReview } from '../../core/courses/course.models';
import { LanguageService } from '../../core/i18n/language.service';
import { LocalDatePipe } from '../../core/i18n/local-date.pipe';
import { StarRatingComponent } from '../../shared/star-rating/star-rating.component';

/** Section « Mes avis » de l'espace compte : notes et avis laissés, modifiables depuis la page du cours. */
@Component({
  selector: 'app-my-reviews',
  imports: [RouterLink, TranslatePipe, LocalDatePipe, StarRatingComponent],
  templateUrl: './my-reviews.component.html',
  styleUrl: './my-reviews.component.scss',
})
export class MyReviewsComponent implements OnInit {
  private readonly api = inject(CourseApiService);
  private readonly translate = inject(TranslateService);
  readonly lang = inject(LanguageService);

  readonly reviews = signal<MyReview[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.api.myReviews().subscribe({
      next: (list) => {
        this.reviews.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  remove(review: MyReview): void {
    if (!confirm(this.translate.instant('account.reviews.confirmDelete', { course: review.courseTitle }))) return;
    this.api.deleteMyReview(review.id).subscribe(() => {
      this.reviews.update((list) => list.filter((r) => r.id !== review.id));
    });
  }
}
