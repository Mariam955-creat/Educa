import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api';

export type SubscriptionPlan = 'MONTHLY' | 'ANNUAL';
export type PaymentProvider = 'STRIPE' | 'ORANGE_MONEY';

export interface CheckoutResponse {
  checkoutUrl: string;
}

export interface Subscription {
  hasSubscription: boolean;
  plan: SubscriptionPlan | null;
  provider: PaymentProvider | null;
  status: 'PENDING' | 'ACTIVE' | 'EXPIRED' | 'CANCELLED' | null;
  currentPeriodEnd: string | null;
  cancelAtPeriodEnd: boolean;
  active: boolean;
}

@Injectable({ providedIn: 'root' })
export class PaymentApiService {
  private readonly http = inject(HttpClient);
  private readonly base = API_BASE_URL;

  checkout(plan: SubscriptionPlan, provider: PaymentProvider): Observable<CheckoutResponse> {
    return this.http.post<CheckoutResponse>(`${this.base}/subscriptions/checkout`, { plan, provider });
  }

  mySubscription(): Observable<Subscription> {
    return this.http.get<Subscription>(`${this.base}/subscriptions/me`);
  }

  cancel(): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.base}/subscriptions/cancel`, {});
  }
}
