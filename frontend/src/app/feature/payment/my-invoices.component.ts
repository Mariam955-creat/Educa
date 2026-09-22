import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { Invoice, PaymentApiService } from '../../core/payment/payment-api.service';

@Component({
  selector: 'app-my-invoices',
  imports: [DatePipe, DecimalPipe, TranslatePipe],
  templateUrl: './my-invoices.component.html',
  styleUrl: './my-invoices.component.scss',
})
export class MyInvoicesComponent implements OnInit {
  private readonly api = inject(PaymentApiService);

  readonly invoices = signal<Invoice[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.api.myInvoices().subscribe({
      next: (list) => {
        this.invoices.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  download(invoice: Invoice): void {
    this.api.downloadInvoice(invoice.id).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `facture-${invoice.invoiceNumber}.pdf`;
      a.click();
      setTimeout(() => URL.revokeObjectURL(url), 10_000);
    });
  }
}
