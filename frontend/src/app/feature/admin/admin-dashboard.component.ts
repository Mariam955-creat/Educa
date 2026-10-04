import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { LocalDatePipe } from '../../core/i18n/local-date.pipe';
import { AdminApiService } from '../../core/admin/admin-api.service';
import {
  AdminUser,
  CertificateRegistryEntry,
  PaymentRegistryEntry,
  ReviewRegistryEntry,
} from '../../core/admin/admin.models';
import { AuthService } from '../../core/auth/auth.service';
import { RoleName } from '../../core/auth/auth.models';
import { LanguageService } from '../../core/i18n/language.service';
import { MoneyPipe } from '../../core/i18n/money.pipe';
import { CourseLanguage } from '../../core/language/language-api.service';
import { StarRatingComponent } from '../../shared/star-rating/star-rating.component';

type Tab = 'users' | 'certificates' | 'payments' | 'reviews' | 'languages';

@Component({
  selector: 'app-admin-dashboard',
  imports: [LocalDatePipe, FormsModule, RouterLink, MoneyPipe, TranslatePipe, StarRatingComponent],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly translate = inject(TranslateService);
  readonly lang = inject(LanguageService);
  readonly auth = inject(AuthService);

  readonly roleOptions: RoleName[] = ['LEARNER', 'INSTRUCTOR', 'ADMIN'];

  readonly tab = signal<Tab>('users');
  readonly currentUserId = computed(() => this.auth.user()?.id ?? null);
  readonly message = signal<string | null>(null);

  readonly q = signal('');
  readonly users = signal<AdminUser[]>([]);
  readonly usersLoading = signal(true);

  readonly certificates = signal<CertificateRegistryEntry[]>([]);
  readonly certificatesLoading = signal(true);
  private certificatesLoaded = false;

  readonly payments = signal<PaymentRegistryEntry[]>([]);
  readonly paymentsLoading = signal(true);
  private paymentsLoaded = false;

  readonly reviews = signal<ReviewRegistryEntry[]>([]);
  readonly reviewsLoading = signal(true);
  private reviewsLoaded = false;

  readonly languages = signal<CourseLanguage[]>([]);
  readonly languagesLoading = signal(true);
  private languagesLoaded = false;

  ngOnInit(): void {
    this.loadUsers();
  }

  selectTab(tab: Tab): void {
    this.tab.set(tab);
    if (tab === 'certificates' && !this.certificatesLoaded) {
      this.loadCertificates();
    }
    if (tab === 'payments' && !this.paymentsLoaded) {
      this.loadPayments();
    }
    if (tab === 'reviews' && !this.reviewsLoaded) {
      this.loadReviews();
    }
    if (tab === 'languages' && !this.languagesLoaded) {
      this.loadLanguages();
    }
  }

  search(): void {
    this.loadUsers();
  }

  primaryRole(user: AdminUser): RoleName {
    if (user.roles.includes('ADMIN')) return 'ADMIN';
    if (user.roles.includes('INSTRUCTOR')) return 'INSTRUCTOR';
    return 'LEARNER';
  }

  changeRole(user: AdminUser, role: string): void {
    this.api.updateRoles(user.id, [role as RoleName]).subscribe({
      next: (updated) => this.replaceUser(updated),
      error: (err: HttpErrorResponse) => {
        this.flash(err.error?.message ?? this.translate.instant('admin.users.updateError'));
        this.loadUsers();
      },
    });
  }

  toggleEnabled(user: AdminUser): void {
    const nextEnabled = !user.enabled;
    const key = nextEnabled ? 'admin.users.confirmEnable' : 'admin.users.confirmDisable';
    if (!confirm(this.translate.instant(key, { name: user.fullName }))) return;

    this.api.updateStatus(user.id, nextEnabled).subscribe({
      next: (updated) => this.replaceUser(updated),
      error: (err: HttpErrorResponse) => {
        this.flash(err.error?.message ?? this.translate.instant('admin.users.updateError'));
        this.loadUsers();
      },
    });
  }

  deleteReview(review: ReviewRegistryEntry): void {
    if (!confirm(this.translate.instant('admin.reviews.confirmDelete', { name: review.authorName }))) return;
    this.api.deleteReview(review.id).subscribe({
      next: () => this.reviews.update((list) => list.filter((r) => r.id !== review.id)),
      error: (err: HttpErrorResponse) => {
        this.flash(err.error?.message ?? this.translate.instant('admin.reviews.deleteError'));
        this.loadReviews();
      },
    });
  }

  toggleLanguage(lang: CourseLanguage): void {
    this.api.setLanguageActive(lang.code, !lang.active).subscribe({
      next: (updated) => {
        this.languages.update((list) => list.map((l) => (l.code === updated.code ? updated : l)));
      },
      error: (err: HttpErrorResponse) => {
        this.flash(err.error?.message ?? this.translate.instant('admin.users.updateError'));
      },
    });
  }

  private replaceUser(updated: AdminUser): void {
    this.users.update((list) => list.map((u) => (u.id === updated.id ? updated : u)));
  }

  private loadUsers(): void {
    this.usersLoading.set(true);
    this.api.users(this.q().trim() || undefined).subscribe({
      next: (page) => {
        this.users.set(page.content);
        this.usersLoading.set(false);
      },
      error: () => this.usersLoading.set(false),
    });
  }

  private loadCertificates(): void {
    this.certificatesLoading.set(true);
    this.api.certificateRegistry().subscribe({
      next: (page) => {
        this.certificates.set(page.content);
        this.certificatesLoading.set(false);
        this.certificatesLoaded = true;
      },
      error: () => this.certificatesLoading.set(false),
    });
  }

  private loadPayments(): void {
    this.paymentsLoading.set(true);
    this.api.paymentRegistry().subscribe({
      next: (page) => {
        this.payments.set(page.content);
        this.paymentsLoading.set(false);
        this.paymentsLoaded = true;
      },
      error: () => this.paymentsLoading.set(false),
    });
  }

  private loadReviews(): void {
    this.reviewsLoading.set(true);
    this.api.reviewRegistry().subscribe({
      next: (page) => {
        this.reviews.set(page.content);
        this.reviewsLoading.set(false);
        this.reviewsLoaded = true;
      },
      error: () => this.reviewsLoading.set(false),
    });
  }

  private loadLanguages(): void {
    this.languagesLoading.set(true);
    this.api.languages().subscribe({
      next: (list) => {
        this.languages.set(list);
        this.languagesLoading.set(false);
        this.languagesLoaded = true;
      },
      error: () => this.languagesLoading.set(false),
    });
  }

  private flash(text: string): void {
    this.message.set(text);
    setTimeout(() => this.message.set(null), 3500);
  }
}
