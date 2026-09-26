import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api';
import { LanguageService } from '../i18n/language.service';

export type PaymentProvider = 'STRIPE' | 'ORANGE_MONEY';
export type PaymentStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED';

export interface CheckoutResponse {
  checkoutUrl: string;
}

export interface Invoice {
  id: number;
  invoiceNumber: string;
  courseTitle: string;
  provider: PaymentProvider;
  amount: number;
  currency: string;
  status: PaymentStatus;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentApiService {
  private readonly http = inject(HttpClient);
  private readonly lang = inject(LanguageService);
  private readonly base = API_BASE_URL;

  checkout(courseId: number, provider: PaymentProvider): Observable<CheckoutResponse> {
    return this.http.post<CheckoutResponse>(`${this.base}/courses/${courseId}/checkout`, { provider });
  }

  myInvoices(): Observable<Invoice[]> {
    return this.http.get<Invoice[]>(`${this.base}/payments/me`);
  }

  downloadInvoice(id: number): Observable<Blob> {
    return this.http.get(`${this.base}/payments/${id}/invoice/download`, {
      params: { lang: this.lang.current() },
      responseType: 'blob',
    });
  }
}
