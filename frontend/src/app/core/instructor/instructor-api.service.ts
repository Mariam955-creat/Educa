import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api';

/**
 * Vente d'un cours du formateur connecté : achat à l'unité (`COURSE`) ou part d'un pass d'abonnement
 * (`SUBSCRIPTION`, partagé entre les cours suivis par l'abonné).
 */
export interface InstructorSale {
  id: number;
  courseId: number;
  courseTitle: string;
  buyerName: string;
  amount: number;
  currency: string;
  createdAt: string;
  kind: 'COURSE' | 'SUBSCRIPTION';
}

/** Avis reçu sur un cours du formateur connecté. */
export interface ReceivedReview {
  id: number;
  courseId: number;
  courseTitle: string;
  courseSlug: string;
  authorName: string;
  stars: number;
  comment: string | null;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class InstructorApiService {
  private readonly http = inject(HttpClient);
  private readonly base = API_BASE_URL;

  sales(): Observable<InstructorSale[]> {
    return this.http.get<InstructorSale[]>(`${this.base}/instructor/sales`);
  }

  reviews(): Observable<ReceivedReview[]> {
    return this.http.get<ReceivedReview[]>(`${this.base}/instructor/reviews`);
  }
}
