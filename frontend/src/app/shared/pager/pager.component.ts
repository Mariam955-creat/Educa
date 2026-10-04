import { Component, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

/** Pagination « ‹ Précédent · page X / Y · Suivant › », masquée s'il n'y a qu'une page. Pages numérotées à partir de 0. */
@Component({
  selector: 'app-pager',
  imports: [TranslatePipe],
  template: `
    @if (totalPages() > 1) {
      <nav class="pager" [attr.aria-label]="'pager.label' | translate">
        <button type="button" [disabled]="page() === 0" (click)="pageChange.emit(page() - 1)">‹ {{ 'pager.previous' | translate }}</button>
        <span>{{ 'pager.status' | translate: { page: page() + 1, total: totalPages() } }}</span>
        <button type="button" [disabled]="page() + 1 >= totalPages()" (click)="pageChange.emit(page() + 1)">
          {{ 'pager.next' | translate }} ›
        </button>
      </nav>
    }
  `,
  styles: `
    .pager { display: flex; align-items: center; justify-content: center; gap: 1rem; margin-top: 1rem; color: var(--muted); font-size: 0.85rem; }
    button { padding: 0.35rem 0.8rem; border: 1px solid var(--border); border-radius: 6px; background: var(--surface); color: inherit; font: inherit; cursor: pointer; }
    button:disabled { opacity: 0.45; cursor: default; }
  `,
})
export class PagerComponent {
  readonly page = input(0);
  readonly totalPages = input(0);
  readonly pageChange = output<number>();
}
