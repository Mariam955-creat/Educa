import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { AuthService } from '../../core/auth/auth.service';
import { CertificateApiService } from '../../core/certificates/certificate-api.service';
import { CourseApiService } from '../../core/courses/course-api.service';
import { ContentItem, CourseDetail, CourseRating } from '../../core/courses/course.models';
import { EnrollmentApiService } from '../../core/enrollments/enrollment-api.service';
import { intlLocale } from '../../core/i18n/intl-locale';
import { LanguageService } from '../../core/i18n/language.service';
import { MoneyPipe } from '../../core/i18n/money.pipe';
import { PaymentApiService, PaymentProvider } from '../../core/payment/payment-api.service';
import { QuizApiService } from '../../core/quiz/quiz-api.service';
import { CourseGrade, CourseQuizzes, QuizRef } from '../../core/quiz/quiz.models';
import { StarRatingComponent } from '../../shared/star-rating/star-rating.component';
import { CourseChatComponent } from './course-chat.component';
import { CoursePresentationComponent } from './course-presentation.component';
import { CourseReviewsComponent } from './course-reviews.component';

const ENROLL_RETRY_DELAY_MS = 2000;
const ENROLL_RETRIES_AFTER_PAYMENT = 5;

@Component({
  selector: 'app-course-detail',
  imports: [RouterLink, CourseChatComponent, CoursePresentationComponent, CourseReviewsComponent, TranslatePipe, MoneyPipe, StarRatingComponent],
  templateUrl: './course-detail.component.html',
  styleUrl: './course-detail.component.scss',
})
export class CourseDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  readonly api = inject(CourseApiService);
  private readonly enrollmentApi = inject(EnrollmentApiService);
  private readonly paymentApi = inject(PaymentApiService);
  private readonly quizApi = inject(QuizApiService);
  private readonly certificateApi = inject(CertificateApiService);
  private readonly translate = inject(TranslateService);
  private readonly auth = inject(AuthService);
  readonly lang = inject(LanguageService);

  readonly course = signal<CourseDetail | null>(null);
  readonly completedIds = signal<number[]>([]);
  readonly progressPercent = signal(0);
  readonly quizzes = signal<CourseQuizzes | null>(null);
  readonly grade = signal<CourseGrade | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly enrolling = signal(false);
  readonly enrollError = signal<string | null>(null);
  readonly buying = signal<PaymentProvider | null>(null);
  readonly paymentNotice = signal<'success' | 'cancelled' | null>(null);
  /** Cours déjà payé mais pas encore inscrit (confirmation reçue après le retour de Stripe) → « S'inscrire ». */
  readonly purchased = signal(false);

  readonly canEnroll = computed(() => {
    const c = this.course();
    return !!c && c.published && !c.contentsVisible;
  });

  ngOnInit(): void {
    const payment = this.route.snapshot.queryParamMap.get('payment');
    if (payment === 'success' || payment === 'cancelled') {
      this.paymentNotice.set(payment);
    }
    this.reload(this.route.snapshot.paramMap.get('slug')!, payment === 'success');
  }

  enroll(retriesLeft = 0): void {
    const c = this.course();
    if (!c) return;
    this.enrolling.set(true);
    this.enrollError.set(null);
    this.enrollmentApi.enroll(c.id).subscribe({
      next: () => this.reload(c.slug),
      error: (err: HttpErrorResponse) => {
        // 402 juste après le retour de Stripe : la confirmation (webhook) peut arriver quelques secondes après
        if (err.status === 402 && retriesLeft > 0) {
          setTimeout(() => this.enroll(retriesLeft - 1), ENROLL_RETRY_DELAY_MS);
          return;
        }
        this.enrolling.set(false);
        this.enrollError.set(err.error?.message ?? this.translate.instant('course.enrollError'));
      },
    });
  }

  buy(provider: PaymentProvider): void {
    const c = this.course();
    if (!c) return;
    this.buying.set(provider);
    this.enrollError.set(null);
    this.paymentApi.checkout(c.id, provider).subscribe({
      next: (res) => {
        window.location.href = res.checkoutUrl;
      },
      error: (err: HttpErrorResponse) => {
        this.buying.set(null);
        this.enrollError.set(err.error?.message ?? this.translate.instant('course.enrollError'));
      },
    });
  }

  /** Note moyenne dans la langue de l'interface : « 4,5 » (fr), « 4.5 » (en). */
  formatRating(value: number): string {
    return new Intl.NumberFormat(intlLocale(this.lang.current()), {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    }).format(value);
  }

  /** Après publication d'un avis : met à jour la note affichée en tête de page. */
  onRatingChange(rating: CourseRating): void {
    const c = this.course();
    if (c) this.course.set({ ...c, averageRating: rating.average, ratingCount: rating.count });
  }

  markComplete(content: ContentItem): void {
    this.enrollmentApi.completeContent(content.id).subscribe((progress) => {
      this.completedIds.set(progress.completedContentIds);
      this.progressPercent.set(progress.progressPercent);
      this.refreshGrade();
    });
  }

  isDone(content: ContentItem): boolean {
    return this.completedIds().includes(content.id);
  }

  openFile(content: ContentItem): void {
    this.api.downloadFile(content.id).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank');
      setTimeout(() => URL.revokeObjectURL(url), 60_000);
    });
  }

  controlFor(chapterId: number): QuizRef | undefined {
    return this.quizzes()?.controls.find((q) => q.chapterId === chapterId);
  }

  controlBest(quizId: number): number | null {
    const control = this.grade()?.controls.find((c) => c.quizId === quizId);
    return control && control.attempts > 0 ? control.bestScore : null;
  }

  downloadCertificate(): void {
    const id = this.grade()?.certificateId;
    if (!id) return;
    this.certificateApi.download(id).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `certificat-${id}.pdf`;
      a.click();
      setTimeout(() => URL.revokeObjectURL(url), 10_000);
    });
  }

  private refreshGrade(): void {
    const c = this.course();
    if (c?.contentsVisible) {
      this.quizApi.courseGrade(c.id).subscribe({ next: (g) => this.grade.set(g), error: () => undefined });
    }
  }

  private reload(slug: string, autoEnrollAfterPayment = false): void {
    this.loading.set(true);
    this.api.detail(slug, true).subscribe({
      next: (course) => {
        this.course.set(course);
        this.loading.set(false);
        this.enrolling.set(false);
        this.buying.set(null);
        if (autoEnrollAfterPayment && !course.contentsVisible) {
          this.enroll(ENROLL_RETRIES_AFTER_PAYMENT);
          return;
        }
        if (!course.contentsVisible && course.price > 0 && this.auth.isAuthenticated()) {
          this.paymentApi.myInvoices().subscribe({
            next: (invoices) => this.purchased.set(invoices.some((i) => i.courseId === course.id)),
            error: () => this.purchased.set(false),
          });
        }
        if (course.contentsVisible) {
          this.enrollmentApi.courseProgress(course.id).subscribe({
            next: (p) => {
              this.completedIds.set(p.completedContentIds);
              this.progressPercent.set(p.progressPercent);
            },
            error: () => undefined,
          });
          this.quizApi.courseQuizzes(course.id).subscribe({ next: (q) => this.quizzes.set(q), error: () => undefined });
          this.refreshGrade();
        }
      },
      error: (err: { status?: number }) => {
        this.error.set(this.translate.instant(err?.status === 404 ? 'course.notFound' : 'course.loadError'));
        this.loading.set(false);
      },
    });
  }
}
