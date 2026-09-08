import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { CertificateApiService, CertificateVerification } from '../../core/certificates/certificate-api.service';

@Component({
  selector: 'app-verify',
  imports: [DatePipe],
  template: `
    <section class="page">
      <h1>Vérification d'un certificat</h1>
      @let r = result();
      @if (loading()) {
        <p class="muted">Vérification…</p>
      } @else if (r && r.valid) {
        <div class="ok">
          <p class="badge">✅ Certificat authentique</p>
          <p><b>{{ r.holderName }}</b> a validé la formation <b>« {{ r.courseTitle }} »</b>.</p>
          <p>Note finale : {{ r.finalGrade }} / 100</p>
          <p class="meta">N° {{ r.serialNumber }} · délivré le {{ r.issuedAt | date: 'longDate' }}</p>
        </div>
      } @else {
        <p class="ko">❌ Aucun certificat ne correspond à ce code.</p>
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
        font-size: 1.4rem;
        margin: 0 0 1.5rem;
      }
      .muted {
        color: var(--muted);
      }
      .ok {
        border: 1px solid #bbf7d0;
        background: #f0fdf4;
        border-radius: 12px;
        padding: 1.5rem;
      }
      .badge {
        font-weight: 700;
        color: #166534;
        margin-top: 0;
      }
      .meta {
        color: var(--muted);
        font-size: 0.85rem;
      }
      .ko {
        color: #b91c1c;
        font-weight: 600;
      }
    `,
  ],
})
export class VerifyComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(CertificateApiService);

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
