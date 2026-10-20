import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { LanguageService } from '../../core/i18n/language.service';
import { LocalDatePipe } from '../../core/i18n/local-date.pipe';
import { MoneyPipe } from '../../core/i18n/money.pipe';
import { InstructorApiService, InstructorSale } from '../../core/instructor/instructor-api.service';

interface CourseRevenue {
  courseId: number;
  courseTitle: string;
  count: number;
  total: number;
}

/** « Revenus » de l'espace formateur : total, répartition par cours et historique des ventes. */
@Component({
  selector: 'app-instructor-sales',
  imports: [TranslatePipe, MoneyPipe, LocalDatePipe],
  template: `
    <section class="page">
      <h1>{{ 'instructor.nav.sales' | translate }}</h1>

      @if (loading()) {
        <p class="muted">{{ 'common.loading' | translate }}</p>
      } @else if (sales().length === 0) {
        <p class="muted">{{ 'instructor.overview.noSales' | translate }}</p>
      } @else {
        <div class="summary">
          <div>
            <span class="label">{{ 'instructor.overview.revenue' | translate }}</span>
            <strong>{{ total() | money: currency() : lang.current() }}</strong>
          </div>
          <div>
            <span class="label">{{ 'instructor.overview.sales' | translate }}</span>
            <strong>{{ sales().length }}</strong>
          </div>
        </div>

        <h2>{{ 'instructor.sales.byCourse' | translate }}</h2>
        <div class="by-course">
          @for (c of byCourse(); track c.courseId) {
            <div class="bar-row">
              <span class="bar-title">{{ c.courseTitle }}</span>
              <span class="bar"><span class="bar-fill" [style.width.%]="(c.total / maxCourseTotal()) * 100"></span></span>
              <span class="bar-value">{{ c.total | money: currency() : lang.current() }} · {{ c.count }}</span>
            </div>
          }
        </div>

        <h2>{{ 'instructor.sales.history' | translate }}</h2>
        <table>
          <thead>
            <tr>
              <th>{{ 'instructor.sales.date' | translate }}</th>
              <th>{{ 'instructor.sales.course' | translate }}</th>
              <th>{{ 'instructor.sales.buyer' | translate }}</th>
              <th class="num">{{ 'instructor.sales.amount' | translate }}</th>
            </tr>
          </thead>
          <tbody>
            @for (s of sales(); track s.id) {
              <tr>
                <td>{{ s.createdAt | localDate: 'short' : lang.current() }}</td>
                <td>{{ s.courseTitle }}</td>
                <td>{{ s.buyerName }}</td>
                <td class="num">{{ s.amount | money: s.currency : lang.current() }}</td>
              </tr>
            }
          </tbody>
        </table>
      }
    </section>
  `,
  styles: `
    .page { max-width: 1000px; margin: 0 auto; padding: 2rem 1.25rem; }
    h1 { margin: 0 0 1.25rem; font-size: 1.5rem; }
    h2 { margin: 1.75rem 0 0.75rem; font-size: 1.05rem; }
    .muted { color: var(--muted); }
    .summary { display: flex; flex-wrap: wrap; gap: 1rem; }
    .summary div { display: flex; flex-direction: column; min-width: 170px; padding: 1rem 1.2rem; border: 1px solid var(--border); border-radius: 12px; background: var(--surface); }
    .summary div:first-child { border-color: transparent; background: var(--brand-gradient, linear-gradient(135deg, #2563eb, #1e40af)); color: #fff; }
    .summary div:first-child .label { color: rgba(255, 255, 255, 0.85); }
    .label { color: var(--muted); font-size: 0.8rem; font-weight: 600; }
    .summary strong { font-size: 1.6rem; }
    .by-course { display: flex; flex-direction: column; gap: 0.6rem; padding: 1rem 1.2rem; border: 1px solid var(--border); border-radius: 12px; background: var(--surface); }
    .bar-row { display: grid; grid-template-columns: minmax(0, 1fr) minmax(80px, 2fr) auto; align-items: center; gap: 0.85rem; font-size: 0.88rem; }
    .bar-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .bar { height: 8px; overflow: hidden; border-radius: 999px; background: var(--border); }
    .bar-fill { display: block; height: 100%; border-radius: 999px; background: var(--accent, #2563eb); }
    .bar-value { color: var(--muted); font-size: 0.8rem; white-space: nowrap; }
    table { width: 100%; border-collapse: collapse; overflow: hidden; border: 1px solid var(--border); border-radius: 12px; background: var(--surface); font-size: 0.88rem; }
    th, td { padding: 0.65rem 0.9rem; border-bottom: 1px solid var(--border); text-align: start; }
    th { color: var(--muted); font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.04em; }
    tr:last-child td { border-bottom: 0; }
    .num { text-align: end; }
  `,
})
export class InstructorSalesComponent implements OnInit {
  private readonly api = inject(InstructorApiService);
  readonly lang = inject(LanguageService);

  readonly sales = signal<InstructorSale[]>([]);
  readonly loading = signal(true);

  readonly currency = computed(() => this.sales()[0]?.currency ?? 'EUR');
  readonly total = computed(() => this.sales().reduce((sum, s) => sum + s.amount, 0));
  /** Revenus par cours, du plus rentable au moins rentable. */
  readonly byCourse = computed<CourseRevenue[]>(() => {
    const map = new Map<number, CourseRevenue>();
    for (const s of this.sales()) {
      const row = map.get(s.courseId) ?? { courseId: s.courseId, courseTitle: s.courseTitle, count: 0, total: 0 };
      row.count++;
      row.total += s.amount;
      map.set(s.courseId, row);
    }
    return [...map.values()].sort((a, b) => b.total - a.total);
  });
  readonly maxCourseTotal = computed(() => Math.max(1, ...this.byCourse().map((c) => c.total)));

  ngOnInit(): void {
    this.api.sales().subscribe({
      next: (list) => {
        this.sales.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
