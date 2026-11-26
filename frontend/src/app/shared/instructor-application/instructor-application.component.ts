import { Component, OnInit, computed, input, signal } from '@angular/core';

import { InstructorRequest } from '../../core/admin/admin.models';
import { TranslatePipe } from '@ngx-translate/core';

/** Pièces jointes demandées, dans l'ordre d'affichage, avec les types acceptés par le serveur. */
export const APPLICATION_DOCUMENTS = [
  { kind: 'diploma', accept: '.pdf,.png,.jpg,.jpeg,.webp' },
  { kind: 'cv', accept: '.pdf,.doc,.docx' },
  { kind: 'idDocument', accept: '.pdf,.png,.jpg,.jpeg,.webp' },
] as const;

export type ApplicationDocumentKind = (typeof APPLICATION_DOCUMENTS)[number]['kind'];

/** Correspondance entre les noms de champ du formulaire et les types de pièce renvoyés par l'API. */
const API_KIND: Record<ApplicationDocumentKind, string> = { diploma: 'DIPLOMA', cv: 'CV', idDocument: 'ID_DOCUMENT' };

/** Taille maximale d'une pièce (même limite que le serveur). */
export const MAX_DOCUMENT_BYTES = 10 * 1024 * 1024;
export const MAX_MOTIVATION = 2000;

/**
 * Formulaire de candidature formateur : motivation et trois pièces obligatoires (dernier diplôme, CV, pièce
 * d'identité). Utilisé à l'inscription (seconde étape) et dans l'espace compte (« Devenir formateur »).
 * Le parent lit `valid()` et `formData()` ; `showErrors` affiche les erreurs après une tentative d'envoi.
 * Avec `previous` (demande refusée), le formulaire est prérempli : motivation reprise, pièces déjà envoyées
 * gardées sauf si le candidat en choisit une nouvelle (le serveur reprend celles qui ne sont pas renvoyées).
 */
@Component({
  selector: 'app-instructor-application',
  imports: [TranslatePipe],
  template: `
    <div class="application">
      <label>
        {{ 'application.motivation' | translate }} *
        <textarea rows="5" [attr.maxlength]="maxMotivation" [value]="motivation()"
                  (input)="motivation.set($any($event.target).value)"
                  [placeholder]="'application.motivationPlaceholder' | translate"></textarea>
        <small class="hint">{{ motivation().length }} / {{ maxMotivation }}</small>
        @if (showErrors() && !motivation().trim()) {
          <small class="field-error">{{ 'application.motivationRequired' | translate }}</small>
        }
      </label>

      @for (doc of documents; track doc.kind) {
        <div class="file-field">
          <span class="file-label">{{ 'application.documents.' + doc.kind | translate }} *</span>
          <label class="file-picker" [class.filled]="files()[doc.kind] || previousName(doc.kind)">
            <input type="file" [accept]="doc.accept" (change)="pick(doc.kind, $any($event.target))" />
            <span class="file-button">{{ (files()[doc.kind] || previousName(doc.kind) ? 'application.replace' : 'application.choose') | translate }}</span>
            <span class="file-name">{{ files()[doc.kind]?.name ?? previousName(doc.kind) ?? ('application.noFile' | translate) }}</span>
          </label>
          @if (!files()[doc.kind] && previousName(doc.kind)) {
            <small class="kept">✓ {{ 'application.kept' | translate }}</small>
          }
          <small class="hint">{{ 'application.hints.' + doc.kind | translate }}</small>
          @if (tooLarge()[doc.kind]) {
            <small class="field-error">{{ 'application.tooLarge' | translate }}</small>
          } @else if (showErrors() && !files()[doc.kind] && !previousName(doc.kind)) {
            <small class="field-error">{{ 'application.fileRequired' | translate }}</small>
          }
        </div>
      }
    </div>
  `,
  styles: `
    .application { display: flex; flex-direction: column; gap: 1rem; }
    label { display: flex; flex-direction: column; gap: 0.3rem; font-size: 0.85rem; font-weight: 600; }
    textarea { padding: 0.6rem 0.7rem; border: 1px solid var(--border, #dbe3ef); border-radius: 8px; font: inherit; font-weight: 400; resize: vertical; }
    .file-field { display: flex; flex-direction: column; gap: 0.3rem; }
    .file-label { font-size: 0.85rem; font-weight: 600; }
    .file-picker { flex-direction: row; align-items: center; gap: 0.75rem; padding: 0.5rem; border: 1px dashed var(--border, #dbe3ef); border-radius: 8px; font-weight: 400; cursor: pointer; }
    .kept { color: var(--success-strong, #047857); font-size: 0.75rem; font-weight: 600; }
    .file-picker.kept-file { border-style: solid; }
    .file-picker.filled { border-style: solid; border-color: var(--success, #10b981); background: var(--success-soft, #d1fae5); }
    .file-picker input { position: absolute; width: 1px; height: 1px; opacity: 0; }
    .file-button { flex-shrink: 0; padding: 0.35rem 0.75rem; border-radius: 6px; background: var(--accent, #2563eb); color: #fff; font-size: 0.8rem; font-weight: 600; }
    .file-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 0.85rem; }
    .hint { color: var(--muted, #6b7280); font-size: 0.75rem; font-weight: 400; }
    .field-error { color: var(--danger, #b91c1c); font-size: 0.78rem; font-weight: 600; }
  `,
})
export class InstructorApplicationComponent implements OnInit {
  readonly showErrors = input(false);
  /** Demande refusée à corriger : préremplit la motivation et garde ses pièces. */
  readonly previous = input<InstructorRequest | null>(null);

  readonly documents = APPLICATION_DOCUMENTS;
  readonly maxMotivation = MAX_MOTIVATION;
  readonly motivation = signal('');
  readonly files = signal<Partial<Record<ApplicationDocumentKind, File>>>({});
  readonly tooLarge = signal<Partial<Record<ApplicationDocumentKind, boolean>>>({});

  readonly valid = computed(
    () =>
      this.motivation().trim().length > 0 &&
      APPLICATION_DOCUMENTS.every(
        (d) => (!!this.files()[d.kind] || !!this.previousName(d.kind)) && !this.tooLarge()[d.kind],
      ),
  );

  ngOnInit(): void {
    const motivation = this.previous()?.motivation;
    if (motivation) this.motivation.set(motivation);
  }

  /** Nom de la pièce déjà envoyée (demande refusée) pour ce champ, si elle existe. */
  previousName(kind: ApplicationDocumentKind): string | null {
    return this.previous()?.documents.find((d) => d.kind === API_KIND[kind])?.originalName ?? null;
  }

  pick(kind: ApplicationDocumentKind, input: HTMLInputElement): void {
    const file = input.files?.[0];
    if (!file) return;
    const big = file.size > MAX_DOCUMENT_BYTES;
    this.tooLarge.update((t) => ({ ...t, [kind]: big }));
    this.files.update((f) => ({ ...f, [kind]: big ? undefined : file }));
    input.value = '';
  }

  /** Corps multipart attendu par `POST /me/instructor-request`. */
  formData(): FormData {
    const data = new FormData();
    data.append('motivation', this.motivation().trim());
    for (const doc of APPLICATION_DOCUMENTS) {
      const file = this.files()[doc.kind];
      if (file) data.append(doc.kind, file, file.name);
    }
    return data;
  }

  reset(): void {
    this.motivation.set('');
    this.files.set({});
    this.tooLarge.set({});
  }
}
