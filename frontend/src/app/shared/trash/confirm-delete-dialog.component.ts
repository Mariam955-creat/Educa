import { Component, ElementRef, HostListener, computed, effect, inject, input, output, signal, viewChild } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

/**
 * Confirmation d'une suppression définitive : l'utilisateur doit taper le mot affiché (« SUPPRIMER » en
 * français, traduit dans les autres langues) pour activer le bouton. Échap ou « Annuler » ferment la boîte.
 */
@Component({
  selector: 'app-confirm-delete-dialog',
  imports: [TranslatePipe],
  template: `
    @if (open()) {
      <div class="backdrop" (click)="cancel.emit()" aria-hidden="true"></div>
      <div class="dialog" role="alertdialog" aria-modal="true" [attr.aria-labelledby]="'confirm-delete-title'">
        <h2 id="confirm-delete-title">⚠ {{ 'trash.permanentTitle' | translate }}</h2>
        <p>{{ 'trash.permanentText' | translate: { name: itemName() } }}</p>
        @if (warning()) {
          <p class="warning">{{ warning() }}</p>
        }
        <label>
          {{ 'trash.typeToConfirm' | translate: { word: word() } }}
          <input #confirmInput type="text" autocomplete="off" [value]="typed()" (input)="typed.set($any($event.target).value)"
                 (keyup.enter)="submit()" [attr.placeholder]="word()" />
        </label>
        <div class="buttons">
          <button type="button" class="ghost" (click)="cancel.emit()">{{ 'trash.cancel' | translate }}</button>
          <button type="button" class="danger" [disabled]="!matches() || busy()" (click)="submit()">
            {{ 'trash.deleteForever' | translate }}
          </button>
        </div>
      </div>
    }
  `,
  styles: `
    .backdrop { position: fixed; inset: 0; z-index: 100; background: rgba(17, 24, 39, 0.5); }
    .dialog { position: fixed; top: 50%; left: 50%; z-index: 101; width: min(440px, calc(100vw - 2rem)); transform: translate(-50%, -50%); padding: 1.5rem; border-radius: 14px; background: var(--surface); box-shadow: 0 24px 60px rgba(17, 24, 39, 0.3); }
    h2 { margin: 0 0 0.75rem; color: #b91c1c; font-size: 1.15rem; }
    p { margin: 0 0 0.75rem; font-size: 0.92rem; line-height: 1.5; }
    .warning { padding: 0.6rem 0.75rem; border-radius: 8px; background: #fef3c7; color: #92400e; font-size: 0.85rem; }
    label { display: flex; flex-direction: column; gap: 0.4rem; font-size: 0.85rem; font-weight: 600; }
    input { padding: 0.6rem 0.7rem; border: 1px solid var(--border); border-radius: 8px; font: inherit; font-weight: 700; letter-spacing: 0.05em; }
    input:focus { border-color: #b91c1c; outline: 2px solid #fee2e2; }
    .buttons { display: flex; justify-content: flex-end; gap: 0.6rem; margin-top: 1.25rem; }
    button { padding: 0.55rem 1rem; border-radius: 8px; font: inherit; font-weight: 600; cursor: pointer; }
    .ghost { border: 1px solid var(--border); background: transparent; color: inherit; }
    .danger { border: 0; background: #b91c1c; color: #fff; }
    .danger:disabled { opacity: 0.4; cursor: not-allowed; }
  `,
})
export class ConfirmDeleteDialogComponent {
  private readonly translate = inject(TranslateService);

  readonly open = input(false);
  /** Nom de l'élément supprimé (titre du cours, nom du compte…). */
  readonly itemName = input('');
  /** Conséquence à signaler (ex. « supprime aussi ses achats »). */
  readonly warning = input<string | null>(null);
  readonly busy = input(false);
  readonly confirmed = output<void>();
  readonly cancel = output<void>();

  readonly typed = signal('');
  private readonly confirmInput = viewChild<ElementRef<HTMLInputElement>>('confirmInput');

  /** Mot à taper, dans la langue de l'interface (« SUPPRIMER » en français). */
  readonly word = computed(() => {
    this.open(); // relu à chaque ouverture (la langue a pu changer)
    return this.translate.instant('trash.confirmWord') as string;
  });
  readonly matches = computed(() => this.typed().trim().toUpperCase() === this.word().toUpperCase());

  constructor() {
    // Champ vidé et focalisé à chaque ouverture
    effect(() => {
      if (this.open()) {
        this.typed.set('');
        setTimeout(() => this.confirmInput()?.nativeElement.focus());
      }
    });
  }

  submit(): void {
    if (this.matches() && !this.busy()) this.confirmed.emit();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open()) this.cancel.emit();
  }
}
