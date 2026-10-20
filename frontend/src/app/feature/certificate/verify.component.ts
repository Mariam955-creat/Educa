import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { LocalDatePipe } from '../../core/i18n/local-date.pipe';
import { LanguageService } from '../../core/i18n/language.service';
import { CertificateApiService, CertificateVerification } from '../../core/certificates/certificate-api.service';

@Component({
  selector: 'app-verify',
  imports: [LocalDatePipe, TranslatePipe],
  template: `
    <section class="page">
      <h1>{{ 'verify.title' | translate }}</h1>
      @let r = result();
      @if (loading()) {
        <p class="muted">{{ 'verify.checking' | translate }}</p>
      } @else if (r && r.valid) {
        <div class="ok">
          <p class="badge">{{ 'verify.valid' | translate }}</p>
          <p>{{ 'verify.validated' | translate: { holder: r.holderName, course: r.courseTitle } }}</p>
          <p>{{ 'verify.finalGrade' | translate: { grade: r.finalGrade } }}</p>
          <p class="meta">{{ 'verify.meta' | translate: { serial: r.serialNumber, date: (r.issuedAt | localDate: 'long' : lang.current()) } }}</p>
        </div>
      } @else {
        <p class="ko">{{ 'verify.invalid' | translate }}</p>
      }
    </section>
  `,
  styles: [
    `
      .page {
        max-width: 560px;
        margin: 0 auto;
        padding: 3rem 1rem;
      }
      h1 {
        font-size: 1.5rem;
        margin: 0 0 1.5rem;
        text-align: center;
      }
      .muted {
        color: var(--muted);
      }
      .ok {
        border: 1px solid var(--border, #dbe3ef);
        border-top: 6px solid var(--success, #10b981);
        background: var(--surface, #ffffff);
        border-radius: 12px;
        padding: 2rem 1.5rem;
        text-align: center;
        box-shadow: var(--shadow-raised, 0 10px 24px rgba(17, 24, 39, 0.12));
      }
      .badge::before {
        content: '✓';
        display: grid;
        place-items: center;
        width: 56px;
        height: 56px;
        margin: 0 auto 0.75rem;
        border-radius: 50%;
        background: var(--success-soft, #d1fae5);
        font-size: 1.8rem;
      }
      .badge {
        font-size: 1.15rem;
        font-weight: 700;
        color: var(--success-strong, #047857);
        margin-top: 0;
      }
      .meta {
        color: var(--muted);
        font-size: 0.85rem;
      }
      .ko {
        padding: 1.5rem;
        border: 1px solid var(--danger-border, #fecaca);
        border-top: 6px solid var(--danger, #b91c1c);
        border-radius: 12px;
        background: var(--danger-soft, #fef2f2);
        color: var(--danger, #b91c1c);
        font-weight: 600;
        text-align: center;
      }
    `,
  ],
})
export class VerifyComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(CertificateApiService);
  readonly lang = inject(LanguageService);

  readonly result = signal<CertificateVerification | null>(null);
  readonly loading = signal(true);

  ngOnInit(): void {
    const code = this.route.snapshot.paramMap.get('code')!;
    this.api.verify(code).subscribe({
      next: (r) => {
        this.result.set(r);
        this.loading.set(false);
      },
      error: () => {
        this.result.set({ valid: false } as CertificateVerification);
        this.loading.set(false);
      },
    });
  }
}
