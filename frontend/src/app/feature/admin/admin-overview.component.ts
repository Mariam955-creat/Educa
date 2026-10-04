import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, forkJoin, map, of } from 'rxjs';

import { AdminApiService } from '../../core/admin/admin-api.service';
import { AdminStats, AdminUser, PaymentRegistryEntry, ReviewRegistryEntry } from '../../core/admin/admin.models';
import { intlLocale } from '../../core/i18n/intl-locale';
import { LanguageService } from '../../core/i18n/language.service';
import { LocalDatePipe } from '../../core/i18n/local-date.pipe';
import { MoneyPipe } from '../../core/i18n/money.pipe';
import { StarRatingComponent } from '../../shared/star-rating/star-rating.component';

/** Tableau de bord admin : indicateurs de la plateforme et activité récente (paiements, inscrits, avis). */
@Component({
  selector: 'app-admin-overview',
  imports: [RouterLink, TranslatePipe, MoneyPipe, LocalDatePipe, StarRatingComponent],
  templateUrl: './admin-overview.component.html',
  styleUrls: ['./admin-shared.scss', './admin-overview.component.scss'],
})
export class AdminOverviewComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  readonly lang = inject(LanguageService);

  readonly stats = signal<AdminStats | null>(null);
  readonly payments = signal<PaymentRegistryEntry[]>([]);
  readonly users = signal<AdminUser[]>([]);
  readonly reviews = signal<ReviewRegistryEntry[]>([]);
  readonly error = signal(false);

  ngOnInit(): void {
    // Les listes d'activité sont facultatives : une erreur sur l'une n'empêche pas d'afficher le reste
    forkJoin({
      stats: this.api.stats(),
      payments: this.api.paymentRegistry(0, 5).pipe(map((p) => p.content), catchError(() => of<PaymentRegistryEntry[]>([]))),
      users: this.api.users(undefined, 0, 5).pipe(map((p) => p.content), catchError(() => of<AdminUser[]>([]))),
      reviews: this.api.reviewRegistry(0, 4).pipe(map((p) => p.content), catchError(() => of<ReviewRegistryEntry[]>([]))),
    }).subscribe({
      next: (data) => {
        this.stats.set(data.stats);
        this.payments.set(data.payments);
        this.users.set(data.users);
        this.reviews.set(data.reviews);
      },
      error: () => this.error.set(true),
    });
  }

  formatRating(value: number): string {
    return new Intl.NumberFormat(intlLocale(this.lang.current()), {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    }).format(value);
  }

  /** Part (en %) d'une valeur dans un total, pour les barres de répartition. */
  share(value: number, total: number): number {
    return total ? Math.round((value / total) * 100) : 0;
  }
}
