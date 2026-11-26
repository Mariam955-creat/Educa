import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { AdminApiService } from '../../core/admin/admin-api.service';
import { InstructorRequest, InstructorRequestDocument, InstructorRequestStatus } from '../../core/admin/admin.models';
import { LanguageService } from '../../core/i18n/language.service';
import { LocalDatePipe } from '../../core/i18n/local-date.pipe';
import { PagerComponent } from '../../shared/pager/pager.component';
import { ModerationCountService } from './moderation-count.service';

const PAGE_SIZE = 20;

/**
 * Demandes de rôle formateur : en attente (les plus anciennes d'abord), acceptées, refusées.
 * Valider ajoute le rôle formateur ; refuser exige un motif. Le candidat est notifié dans les deux cas.
 */
@Component({
  selector: 'app-admin-instructor-requests',
  imports: [TranslatePipe, LocalDatePipe, PagerComponent],
  template: `
    <section class="page">
      <div class="head">
        <div>
          <h1>{{ 'admin.nav.instructorRequests' | translate }}</h1>
          <p class="subtitle">{{ 'admin.requests.pendingCount' | translate: { count: count.instructorRequests() } }}</p>
        </div>
      </div>

      @if (message(); as m) {
        <p class="flash" [class.ok-flash]="!m.error">{{ m.text }}</p>
      }

      <div class="toolbar">
        <div class="chips">
          @for (s of statuses; track s) {
            <button type="button" [class.active]="status() === s" (click)="show(s)">
              {{ 'admin.requests.status.' + s | translate }}
              @if (s === 'PENDING') {
                <span class="count">{{ count.instructorRequests() }}</span>
              }
            </button>
          }
        </div>
      </div>

      @if (loading()) {
        <p class="muted">{{ 'common.loading' | translate }}</p>
      } @else if (requests().length === 0) {
        <p class="muted">{{ 'admin.requests.empty.' + status() | translate }}</p>
      } @else {
        <div class="cards">
          @for (r of requests(); track r.id) {
            <article class="request">
              <div class="who">
                <span class="avatar" aria-hidden="true">{{ (r.fullName ?? '?').charAt(0) }}</span>
                <div>
                  <strong>{{ r.fullName }}</strong>
                  <small>{{ r.email }}</small>
                  @if (r.headline) {
                    <small>{{ r.headline }}</small>
                  }
                </div>
              </div>
              <p class="date">{{ 'admin.requests.sentOn' | translate: { date: (r.createdAt | localDate: 'long' : lang.current()) } }}</p>

              <p class="motivation" [class.empty]="!r.motivation">
                {{ r.motivation || ('admin.requests.noMotivation' | translate) }}
              </p>

              @if (r.documents.length) {
                <ul class="docs">
                  @for (d of r.documents; track d.id) {
                    <li>
                      <button type="button" class="doc" (click)="download(r, d)" [title]="d.originalName">
                        <span class="doc-icon" aria-hidden="true">{{ d.mimeType.startsWith('image/') ? '🖼' : '📄' }}</span>
                        <span class="doc-text">
                          <span class="doc-kind">{{ 'admin.requests.documents.' + d.kind | translate }}</span>
                          <span class="doc-name">{{ d.originalName }}</span>
                        </span>
                        <span class="doc-size">{{ size(d.sizeBytes) }}</span>
                        <span class="doc-dl" aria-hidden="true">⬇</span>
                      </button>
                    </li>
                  }
                </ul>
              } @else {
                <p class="date">{{ 'admin.requests.noDocuments' | translate }}</p>
              }

              @if (r.status === 'REJECTED' && r.rejectionReason) {
                <p class="reason"><strong>{{ 'admin.requests.reason' | translate }} :</strong> {{ r.rejectionReason }}</p>
              }
              @if (r.decidedAt) {
                <p class="date">{{ 'admin.requests.decidedOn' | translate: { date: (r.decidedAt | localDate: 'long' : lang.current()) } }}</p>
              }

              @if (r.status === 'PENDING') {
                @if (rejecting() === r.id) {
                  <label class="reject-box">
                    {{ 'admin.requests.reasonLabel' | translate }}
                    <textarea rows="3" maxlength="1000" [value]="reason()" (input)="reason.set($any($event.target).value)"
                              [placeholder]="'admin.requests.reasonPlaceholder' | translate"></textarea>
                  </label>
                  <div class="decision">
                    <button type="button" (click)="cancelReject()">{{ 'trash.cancel' | translate }}</button>
                    <button type="button" class="danger" [disabled]="!reason().trim() || busy()" (click)="confirmReject(r)">
                      {{ 'admin.requests.confirmReject' | translate }}
                    </button>
                  </div>
                } @else {
                  <div class="decision">
                    <button type="button" class="danger" [disabled]="busy()" (click)="startReject(r)">
                      {{ 'admin.requests.reject' | translate }}
                    </button>
                    <button type="button" class="approve" [disabled]="busy()" (click)="approve(r)">
                      ✓ {{ 'admin.requests.approve' | translate }}
                    </button>
                  </div>
                }
              }
            </article>
          }
        </div>
        <app-pager [page]="page()" [totalPages]="totalPages()" (pageChange)="load($event)" />
      }
    </section>
  `,
  styleUrl: './admin-shared.scss',
  styles: `
    .count { margin-inline-start: 0.3rem; padding: 0 0.4rem; border-radius: 999px; background: var(--warning, #f59e0b); color: var(--text, #1f2937); font-size: 0.75rem; font-weight: 700; }
    .flash.ok-flash { border-color: #a7f3d0; background: var(--success-soft, #d1fae5); color: var(--success-strong, #047857); }

    /* Grille de cartes : min-width: 0 empêche un contenu long d'élargir la colonne */
    .cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(340px, 100%), 1fr)); gap: 1rem; }
    .request { display: flex; flex-direction: column; gap: 0.75rem; min-width: 0; padding: 1.1rem; border: 1px solid var(--border); border-radius: 12px; background: var(--surface); box-shadow: var(--shadow-card, 0 1px 2px rgba(31, 41, 55, 0.05)); }

    .who { display: flex; align-items: center; gap: 0.7rem; min-width: 0; }
    .who .avatar { flex-shrink: 0; }
    .who div { display: flex; flex-direction: column; min-width: 0; }
    .who strong, .who small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .who small, .date { color: var(--muted); font-size: 0.8rem; }
    .date { margin: 0; }

    /* Texte libre du candidat : coupé même sans espaces, hauteur bornée avec défilement */
    .motivation, .reason { margin: 0; padding: 0.6rem 0.75rem; border-radius: 8px; font-size: 0.88rem; line-height: 1.45; white-space: pre-line; overflow-wrap: anywhere; }
    .motivation { max-height: 9.5rem; overflow-y: auto; background: var(--bg); }
    .motivation.empty { color: var(--muted); font-style: italic; }
    .reason { background: #fef2f2; color: var(--danger, #b91c1c); font-size: 0.85rem; }

    /* Pièces jointes : une ligne par pièce, nom tronqué (complet au survol) */
    .docs { display: flex; flex-direction: column; gap: 0.4rem; margin: 0; padding: 0; list-style: none; }
    .doc { display: grid; grid-template-columns: auto minmax(0, 1fr) auto auto; align-items: center; gap: 0.6rem; width: 100%; padding: 0.5rem 0.65rem; border: 1px solid var(--border); border-radius: 8px; background: var(--surface); color: inherit; font: inherit; text-align: start; cursor: pointer; transition: border-color 0.15s, background 0.15s; }
    .doc:hover { border-color: var(--accent, #2563eb); background: var(--accent-soft, #eff6ff); }
    .doc-icon { font-size: 1.1rem; }
    .doc-text { display: flex; flex-direction: column; min-width: 0; }
    .doc-kind { color: var(--muted); font-size: 0.72rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.03em; }
    .doc-name { overflow: hidden; color: var(--accent, #2563eb); font-size: 0.85rem; text-overflow: ellipsis; white-space: nowrap; }
    .doc-size { color: var(--muted); font-size: 0.75rem; white-space: nowrap; }
    .doc-dl { color: var(--accent, #2563eb); font-weight: 700; }

    .reject-box { display: flex; flex-direction: column; gap: 0.35rem; font-size: 0.85rem; font-weight: 600; }
    .reject-box textarea { padding: 0.5rem 0.65rem; border: 1px solid var(--border); border-radius: 8px; font: inherit; font-weight: 400; resize: vertical; }

    /* Décision : deux boutons de même largeur, en bas de la carte */
    .decision { display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem; margin-top: auto; }
    .decision button { padding: 0.55rem 0.75rem; border: 1px solid var(--border); border-radius: 8px; background: transparent; color: inherit; font: inherit; font-size: 0.85rem; font-weight: 600; cursor: pointer; }
    .decision button:disabled { opacity: 0.45; cursor: not-allowed; }
    .decision .danger { border-color: #fecaca; color: var(--danger, #b91c1c); }
    .decision .danger:hover:not(:disabled) { background: #fef2f2; }
    .decision .approve { border-color: var(--success, #10b981); background: var(--success, #10b981); color: #fff; }
    .decision .approve:hover:not(:disabled) { background: var(--success-strong, #047857); }
  `,
})
export class AdminInstructorRequestsComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly translate = inject(TranslateService);
  readonly count = inject(ModerationCountService);
  readonly lang = inject(LanguageService);

  readonly statuses: InstructorRequestStatus[] = ['PENDING', 'APPROVED', 'REJECTED'];
  readonly status = signal<InstructorRequestStatus>('PENDING');
  readonly requests = signal<InstructorRequest[]>([]);
  readonly loading = signal(true);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly busy = signal(false);
  /** Demande dont le formulaire de refus est ouvert. */
  readonly rejecting = signal<number | null>(null);
  readonly reason = signal('');
  readonly message = signal<{ text: string; error: boolean } | null>(null);

  ngOnInit(): void {
    this.load(0);
  }

  show(status: InstructorRequestStatus): void {
    this.status.set(status);
    this.cancelReject();
    this.load(0);
  }

  load(page: number): void {
    this.loading.set(true);
    this.api.instructorRequests(this.status(), page, PAGE_SIZE).subscribe({
      next: (res) => {
        this.requests.set(res.content);
        this.page.set(res.page);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.count.refresh();
  }

  /** Télécharge une pièce jointe (requête authentifiée, puis lien temporaire vers le fichier reçu). */
  download(request: InstructorRequest, document: InstructorRequestDocument): void {
    this.api.downloadInstructorDocument(request.id, document.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = window.document.createElement('a');
        a.href = url;
        a.download = document.originalName;
        a.click();
        setTimeout(() => URL.revokeObjectURL(url), 10_000);
      },
      error: () => this.flash(this.translate.instant('admin.requests.downloadError'), true),
    });
  }

  size(bytes: number): string {
    return bytes >= 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)} Mo` : `${Math.max(1, Math.round(bytes / 1024))} Ko`;
  }

  approve(request: InstructorRequest): void {
    this.busy.set(true);
    this.api.approveInstructorRequest(request.id).subscribe({
      next: () => this.done(this.translate.instant('admin.requests.approved', { name: request.fullName })),
      error: (err: HttpErrorResponse) => this.fail(err),
    });
  }

  startReject(request: InstructorRequest): void {
    this.rejecting.set(request.id);
    this.reason.set('');
  }

  cancelReject(): void {
    this.rejecting.set(null);
    this.reason.set('');
  }

  confirmReject(request: InstructorRequest): void {
    const reason = this.reason().trim();
    if (!reason) return;
    this.busy.set(true);
    this.api.rejectInstructorRequest(request.id, reason).subscribe({
      next: () => this.done(this.translate.instant('admin.requests.rejected', { name: request.fullName })),
      error: (err: HttpErrorResponse) => this.fail(err),
    });
  }

  private done(text: string): void {
    this.busy.set(false);
    this.cancelReject();
    this.flash(text, false);
    this.load(this.page());
  }

  private fail(err: HttpErrorResponse): void {
    this.busy.set(false);
    this.flash(err.error?.message ?? this.translate.instant('admin.users.updateError'), true);
    this.load(this.page());
  }

  private flash(text: string, error: boolean): void {
    this.message.set({ text, error });
    setTimeout(() => this.message.set(null), 4000);
  }
}
