import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { AdminApiService } from '../../core/admin/admin-api.service';
import { CertificateRegistryEntry, PaymentRegistryEntry, ReviewRegistryEntry } from '../../core/admin/admin.models';
import { LanguageService } from '../../core/i18n/language.service';
import { LocalDatePipe } from '../../core/i18n/local-date.pipe';
import { MoneyPipe } from '../../core/i18n/money.pipe';
import { CourseLanguage } from '../../core/language/language-api.service';
import { PagerComponent } from '../../shared/pager/pager.component';
import { StarRatingComponent } from '../../shared/star-rating/star-rating.component';

const PAGE_SIZE = 20;

type PaymentFilter = 'all' | 'SUCCEEDED' | 'PENDING' | 'FAILED';

/** Registre des paiements : statut, prestataire, facture ; filtre par statut (sur la page affichée). */
@Component({
  selector: 'app-admin-payments',
  imports: [TranslatePipe, MoneyPipe, LocalDatePipe, PagerComponent],
  template: `
    <section class="page">
      <div class="head">
        <div>
          <h1>{{ 'admin.nav.payments' | translate }}</h1>
          <p class="subtitle">{{ 'admin.registry.count' | translate: { count: total() } }}</p>
        </div>
      </div>
      <div class="toolbar">
        <div class="chips">
          @for (f of filters; track f) {
            <button type="button" [class.active]="filter() === f" (click)="filter.set(f)">
              {{ (f === 'all' ? 'instructor.courses.filter.all' : 'admin.payments.status.' + f) | translate }}
            </button>
          }
        </div>
      </div>
      @if (loading()) {
        <p class="muted">{{ 'common.loading' | translate }}</p>
      } @else if (visible().length === 0) {
        <p class="muted">{{ 'admin.payments.empty' | translate }}</p>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'admin.payments.headerDate' | translate }}</th>
                <th>{{ 'admin.payments.headerUser' | translate }}</th>
                <th>{{ 'admin.payments.headerCourse' | translate }}</th>
                <th>{{ 'admin.payments.headerProvider' | translate }}</th>
                <th>{{ 'admin.payments.headerStatus' | translate }}</th>
                <th>{{ 'admin.payments.headerInvoice' | translate }}</th>
                <th class="num">{{ 'admin.payments.headerAmount' | translate }}</th>
              </tr>
            </thead>
            <tbody>
              @for (p of visible(); track p.id) {
                <tr>
                  <td class="muted">{{ p.createdAt | localDate: 'short' : lang.current() }}</td>
                  <td>{{ p.userName }}</td>
                  <td>{{ p.courseTitle }}</td>
                  <td>{{ p.provider === 'STRIPE' ? 'Stripe' : 'Orange Money' }}</td>
                  <td>
                    <span class="badge" [class.ok]="p.status === 'SUCCEEDED'" [class.warn]="p.status === 'PENDING'" [class.ko]="p.status === 'FAILED'">
                      {{ 'admin.payments.status.' + p.status | translate }}
                    </span>
                  </td>
                  <td>{{ p.invoiceNumber ?? '—' }}</td>
                  <td class="num"><strong>{{ p.amount | money: p.currency : lang.current() }}</strong></td>
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
})
export class AdminPaymentsComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  readonly lang = inject(LanguageService);

  readonly filters: PaymentFilter[] = ['all', 'SUCCEEDED', 'PENDING', 'FAILED'];
  readonly payments = signal<PaymentRegistryEntry[]>([]);
  readonly filter = signal<PaymentFilter>('all');
  readonly visible = computed(() =>
    this.filter() === 'all' ? this.payments() : this.payments().filter((p) => p.status === this.filter()),
  );
  readonly loading = signal(true);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly total = signal(0);

  ngOnInit(): void {
    this.load(0);
  }

  load(page: number): void {
    this.loading.set(true);
    this.api.paymentRegistry(page, PAGE_SIZE).subscribe({
      next: (res) => {
        this.payments.set(res.content);
        this.page.set(res.page);
        this.totalPages.set(res.totalPages);
        this.total.set(res.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}

/** Registre des certificats délivrés, avec lien vers la page de vérification publique. */
@Component({
  selector: 'app-admin-certificates',
  imports: [RouterLink, TranslatePipe, LocalDatePipe, PagerComponent],
  template: `
    <section class="page">
      <div class="head">
        <div>
          <h1>{{ 'admin.nav.certificates' | translate }}</h1>
          <p class="subtitle">{{ 'admin.registry.count' | translate: { count: total() } }}</p>
        </div>
      </div>
      @if (loading()) {
        <p class="muted">{{ 'common.loading' | translate }}</p>
      } @else if (certificates().length === 0) {
        <p class="muted">{{ 'admin.certificates.empty' | translate }}</p>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'admin.certificates.headerSerial' | translate }}</th>
                <th>{{ 'admin.certificates.headerHolder' | translate }}</th>
                <th>{{ 'admin.certificates.headerCourse' | translate }}</th>
                <th class="num">{{ 'admin.certificates.headerGrade' | translate }}</th>
                <th>{{ 'admin.certificates.headerIssued' | translate }}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (c of certificates(); track c.id) {
                <tr>
                  <td><code>{{ c.serialNumber }}</code></td>
                  <td>{{ c.holderName }}</td>
                  <td>{{ c.courseTitle }}</td>
                  <td class="num"><span class="badge ok">{{ c.finalGrade }} / 100</span></td>
                  <td class="muted">{{ c.issuedAt | localDate: 'long' : lang.current() }}</td>
                  <td>
                    <div class="actions">
                      <a [routerLink]="['/verify', c.verificationCode]">{{ 'admin.registry.verify' | translate }}</a>
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
})
export class AdminCertificatesComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  readonly lang = inject(LanguageService);

  readonly certificates = signal<CertificateRegistryEntry[]>([]);
  readonly loading = signal(true);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly total = signal(0);

  ngOnInit(): void {
    this.load(0);
  }

  load(page: number): void {
    this.loading.set(true);
    this.api.certificateRegistry(page, PAGE_SIZE).subscribe({
      next: (res) => {
        this.certificates.set(res.content);
        this.page.set(res.page);
        this.totalPages.set(res.totalPages);
        this.total.set(res.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}

/** Modération des avis : toutes les notes, suppression (note + texte, la moyenne est recalculée). */
@Component({
  selector: 'app-admin-reviews',
  imports: [RouterLink, TranslatePipe, LocalDatePipe, PagerComponent, StarRatingComponent],
  template: `
    <section class="page">
      <div class="head">
        <div>
          <h1>{{ 'admin.nav.reviews' | translate }}</h1>
          <p class="subtitle">{{ 'admin.registry.count' | translate: { count: total() } }}</p>
        </div>
      </div>
      @if (message()) {
        <p class="flash">{{ message() }}</p>
      }
      @if (loading()) {
        <p class="muted">{{ 'common.loading' | translate }}</p>
      } @else if (reviews().length === 0) {
        <p class="muted">{{ 'admin.reviews.empty' | translate }}</p>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'admin.reviews.headerAuthor' | translate }}</th>
                <th>{{ 'admin.reviews.headerRating' | translate }}</th>
                <th>{{ 'admin.reviews.headerComment' | translate }}</th>
                <th>{{ 'admin.reviews.headerDate' | translate }}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (r of reviews(); track r.id) {
                <tr>
                  <td>
                    <span class="person-text">{{ r.authorName }}
                      <small><a [routerLink]="['/courses', r.courseSlug]">{{ r.courseTitle }}</a></small>
                    </span>
                  </td>
                  <td><app-star-rating [value]="r.stars" size="small" [label]="r.stars + ' / 5'" /></td>
                  <td class="comment">{{ r.comment ?? '—' }}</td>
                  <td class="muted">{{ r.updatedAt | localDate: 'short' : lang.current() }}</td>
                  <td>
                    <div class="actions">
                      <button type="button" class="danger" (click)="remove(r)">{{ 'common.delete' | translate }}</button>
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
  styles: `.comment { max-width: 28rem; white-space: pre-line; }`,
})
export class AdminReviewsComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly translate = inject(TranslateService);
  readonly lang = inject(LanguageService);

  readonly reviews = signal<ReviewRegistryEntry[]>([]);
  readonly loading = signal(true);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly total = signal(0);
  readonly message = signal<string | null>(null);

  ngOnInit(): void {
    this.load(0);
  }

  load(page: number): void {
    this.loading.set(true);
    this.api.reviewRegistry(page, PAGE_SIZE).subscribe({
      next: (res) => {
        this.reviews.set(res.content);
        this.page.set(res.page);
        this.totalPages.set(res.totalPages);
        this.total.set(res.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  remove(review: ReviewRegistryEntry): void {
    if (!confirm(this.translate.instant('admin.reviews.confirmDelete', { name: review.authorName }))) return;
    this.api.deleteReview(review.id).subscribe({
      next: () => this.load(this.page()),
      error: (err: HttpErrorResponse) => {
        this.message.set(err.error?.message ?? this.translate.instant('admin.reviews.deleteError'));
        setTimeout(() => this.message.set(null), 3500);
      },
    });
  }
}

/** Langues de contenu des cours : activer / désactiver (la dernière langue active ne peut pas l'être). */
@Component({
  selector: 'app-admin-languages',
  imports: [TranslatePipe],
  template: `
    <section class="page">
      <div class="head">
        <div>
          <h1>{{ 'admin.nav.languages' | translate }}</h1>
          <p class="subtitle">{{ 'admin.languagesPage.subtitle' | translate }}</p>
        </div>
      </div>
      @if (message()) {
        <p class="flash">{{ message() }}</p>
      }
      @if (loading()) {
        <p class="muted">{{ 'common.loading' | translate }}</p>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'admin.languages.headerCode' | translate }}</th>
                <th>{{ 'admin.languages.headerName' | translate }}</th>
                <th>{{ 'admin.users.headerStatus' | translate }}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (l of languages(); track l.code) {
                <tr>
                  <td><span class="badge info">{{ l.code.toUpperCase() }}</span></td>
                  <td>{{ l.name }}</td>
                  <td>
                    <span class="badge" [class.ok]="l.active">
                      {{ (l.active ? 'admin.users.enabled' : 'admin.users.disabled') | translate }}
                    </span>
                  </td>
                  <td>
                    <div class="actions">
                      <button type="button" [class.danger]="l.active" (click)="toggle(l)">
                        {{ (l.active ? 'admin.languages.deactivate' : 'admin.languages.activate') | translate }}
                      </button>
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `,
  styleUrl: './admin-shared.scss',
})
export class AdminLanguagesComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly translate = inject(TranslateService);

  readonly languages = signal<CourseLanguage[]>([]);
  readonly loading = signal(true);
  readonly message = signal<string | null>(null);

  ngOnInit(): void {
    this.api.languages().subscribe({
      next: (list) => {
        this.languages.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  toggle(lang: CourseLanguage): void {
    this.api.setLanguageActive(lang.code, !lang.active).subscribe({
      next: (updated) => this.languages.update((list) => list.map((l) => (l.code === updated.code ? updated : l))),
      error: (err: HttpErrorResponse) => {
        this.message.set(err.error?.message ?? this.translate.instant('admin.users.updateError'));
        setTimeout(() => this.message.set(null), 3500);
      },
    });
  }
}
