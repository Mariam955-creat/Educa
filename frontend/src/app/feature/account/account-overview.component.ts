import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, catchError, forkJoin, of } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { Certificate, CertificateApiService } from '../../core/certificates/certificate-api.service';
import { CourseApiService } from '../../core/courses/course-api.service';
import { CourseSummary, Enrollment, MyReview } from '../../core/courses/course.models';
import { EnrollmentApiService } from '../../core/enrollments/enrollment-api.service';
import { intlLocale } from '../../core/i18n/intl-locale';
import { LanguageService } from '../../core/i18n/language.service';
import { LocalDatePipe } from '../../core/i18n/local-date.pipe';
import { MoneyPipe } from '../../core/i18n/money.pipe';
import { Invoice, PaymentApiService } from '../../core/payment/payment-api.service';

/** Tableau de bord de l'espace compte : indicateurs (apprenant, et formateur le cas échéant) + cours à reprendre. */
@Component({
  selector: 'app-account-overview',
  imports: [RouterLink, TranslatePipe, MoneyPipe, LocalDatePipe],
  templateUrl: './account-overview.component.html',
  styleUrl: './account-overview.component.scss',
})
export class AccountOverviewComponent implements OnInit {
  private readonly enrollmentApi = inject(EnrollmentApiService);
  private readonly certificateApi = inject(CertificateApiService);
  private readonly paymentApi = inject(PaymentApiService);
  private readonly courseApi = inject(CourseApiService);
  readonly auth = inject(AuthService);
  readonly lang = inject(LanguageService);

  readonly loading = signal(true);
  readonly enrollments = signal<Enrollment[]>([]);
  readonly certificates = signal<Certificate[]>([]);
  readonly invoices = signal<Invoice[]>([]);
  readonly reviews = signal<MyReview[]>([]);
  readonly taughtCourses = signal<CourseSummary[]>([]);

  readonly isInstructor = computed(() => this.auth.hasAnyRole(['INSTRUCTOR', 'ADMIN']));

  // --- Indicateurs apprenant ---
  readonly completedCount = computed(() => this.enrollments().filter((e) => e.progressPercent >= 100).length);
  readonly averageProgress = computed(() => {
    const list = this.enrollments();
    return list.length ? Math.round(list.reduce((sum, e) => sum + e.progressPercent, 0) / list.length) : 0;
  });
  readonly paidInvoices = computed(() => this.invoices().filter((i) => i.status === 'SUCCEEDED'));
  readonly totalSpent = computed(() => this.paidInvoices().reduce((sum, i) => sum + i.amount, 0));
  readonly spentCurrency = computed(() => this.paidInvoices()[0]?.currency ?? 'EUR');
  /** Cours commencés mais pas finis, les plus avancés d'abord. */
  readonly inProgress = computed(() =>
    this.enrollments()
      .filter((e) => e.progressPercent < 100)
      .sort((a, b) => b.progressPercent - a.progressPercent)
      .slice(0, 3),
  );
  readonly latestCertificates = computed(() => this.certificates().slice(0, 3));

  // --- Indicateurs formateur ---
  readonly publishedCount = computed(() => this.taughtCourses().filter((c) => c.published).length);
  readonly totalLearners = computed(() => this.taughtCourses().reduce((sum, c) => sum + c.learnerCount, 0));
  /** Moyenne pondérée par le nombre de notes de chaque cours ; `null` si aucune note. */
  readonly teachingRating = computed(() => {
    const rated = this.taughtCourses().filter((c) => c.averageRating != null && c.ratingCount > 0);
    const count = rated.reduce((sum, c) => sum + c.ratingCount, 0);
    return count ? rated.reduce((sum, c) => sum + c.averageRating! * c.ratingCount, 0) / count : null;
  });

  ngOnInit(): void {
    // Chaque source est indépendante : une erreur sur l'une n'empêche pas d'afficher les autres
    forkJoin({
      enrollments: orEmpty(this.enrollmentApi.myEnrollments()),
      certificates: orEmpty(this.certificateApi.mine()),
      invoices: orEmpty(this.paymentApi.myInvoices()),
      reviews: orEmpty(this.courseApi.myReviews()),
      taught: this.isInstructor() ? orEmpty(this.courseApi.myCourses()) : of<CourseSummary[]>([]),
    }).subscribe((data) => {
      this.enrollments.set(data.enrollments);
      this.certificates.set(data.certificates);
      this.invoices.set(data.invoices);
      this.reviews.set(data.reviews);
      this.taughtCourses.set(data.taught);
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
