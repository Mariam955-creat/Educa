import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';

import { Certificate, CertificateApiService } from '../../core/certificates/certificate-api.service';

@Component({
  selector: 'app-my-certificates',
  imports: [DatePipe],
  templateUrl: './my-certificates.component.html',
  styleUrl: './my-certificates.component.scss',
})
export class MyCertificatesComponent implements OnInit {
  private readonly api = inject(CertificateApiService);

  readonly certificates = signal<Certificate[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.api.mine().subscribe({
      next: (list) => {
        this.certificates.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  download(certificate: Certificate): void {
    this.api.download(certificate.id).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank');
      setTimeout(() => URL.revokeObjectURL(url), 60_000);
    });
  }
}
