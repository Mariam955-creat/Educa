import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api';
import { LanguageService } from '../i18n/language.service';

export type PaymentProvider = 'STRIPE' | 'ORANGE_MONEY';
export type PaymentStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED';
/** Achat d'un cours, ou pass d'abonnement (accès à tous les cours). */
export type PaymentKind = 'COURSE' | 'SUBSCRIPTION';

export interface CheckoutResponse {
  checkoutUrl: string;
}

export interface Invoice {
  id: number;
  invoiceNumber: string;
  /** Absent pour un abonnement. */
  courseId?: number;
  courseTitle: string;
  kind: PaymentKind;
  /** Abonnement : fin de l'accès accordé par ce paiement. */
  accessUntil?: string;
  provider: PaymentProvider;
  amount: number;
  currency: string;
  status: PaymentStatus;
  createdAt: string;
}

/** Offre d'abonnement (prix, durée d'un pass) et, si connecté, son état. */
export interface Subscription {
  price: number;
  currency: string;
  days: number;
  active: boolean;
  accessUntil?: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentApiService {
  private readonly http = inject(HttpClient);
  private readonly lang = inject(LanguageService);
  private readonly base = API_BASE_URL;

  checkout(courseId: number, provider: PaymentProvider): Observable<CheckoutResponse> {
    return this.http.post<CheckoutResponse>(`${this.base}/courses/${courseId}/checkout`, { provider });
  }

  subscription(): Observable<Subscription> {
    return this.http.get<Subscription>(`${this.base}/subscription`);
  }

  subscribe(provider: PaymentProvider): Observable<CheckoutResponse> {
    return this.http.post<CheckoutResponse>(`${this.base}/subscription/checkout`, { provider });
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
