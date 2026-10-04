import { Component, computed, input, output, signal } from '@angular/core';

/**
 * Étoiles de notation (sur 5).
 *  - lecture seule (défaut) : affiche `value`, avec remplissage partiel (3,5 → trois étoiles et demie) ;
 *  - `editable` : chaque étoile est un bouton, survol = aperçu, clic = émet `rate` (1 à 5).
 */
@Component({
  selector: 'app-star-rating',
  template: `
    <span
      class="stars"
      [class.editable]="editable()"
      [class.small]="size() === 'small'"
      [attr.role]="editable() ? 'radiogroup' : 'img'"
      [attr.aria-label]="label()"
      (mouseleave)="hover.set(0)"
    >
      @for (star of starIndexes; track star) {
        @if (editable()) {
          <button
            type="button"
            class="star"
            role="radio"
            [attr.aria-checked]="star === value()"
            [attr.aria-label]="star + ' / 5'"
            (mouseenter)="hover.set(star)"
            (focus)="hover.set(star)"
            (blur)="hover.set(0)"
            (click)="rate.emit(star)"
          >
            <span class="fill" [style.width.%]="fillFor(star)">★</span>★
          </button>
        } @else {
          <span class="star" aria-hidden="true"><span class="fill" [style.width.%]="fillFor(star)">★</span>★</span>
        }
      }
    </span>
  `,
  styles: `
    .stars {
      display: inline-flex;
      gap: 0.05em;
      font-size: 1.15rem;
      line-height: 1;
      vertical-align: middle;
    }

    .stars.small {
      font-size: 0.95rem;
    }

    .star {
      position: relative;
      display: inline-block;
      color: #d4d4d8;
    }

    button.star {
      padding: 0 0.05em;
      border: 0;
      background: none;
      font: inherit;
      cursor: pointer;
      transition: transform 0.1s;
    }

    button.star:hover,
    button.star:focus-visible {
      transform: scale(1.15);
      outline: none;
    }

    .fill {
      position: absolute;
      inset-block: 0;
      inset-inline-start: 0;
      overflow: hidden;
      color: #f59e0b;
      white-space: nowrap;
    }

    button.star .fill {
      inset-inline-start: 0.05em;
    }
  `,
})
export class StarRatingComponent {
  readonly value = input<number | null | undefined>(0);
  readonly editable = input(false);
  readonly size = input<'normal' | 'small'>('normal');
  /** Libellé accessible (ex. « Note : 4,5 sur 5 »). */
  readonly label = input('');
  readonly rate = output<number>();

  readonly starIndexes = [1, 2, 3, 4, 5];
  readonly hover = signal(0);
  private readonly shown = computed(() => this.hover() || this.value() || 0);

  /** Pourcentage de remplissage de l'étoile n (0, partiel ou 100). */
  fillFor(star: number): number {
    return Math.max(0, Math.min(1, this.shown() - (star - 1))) * 100;
  }
}
