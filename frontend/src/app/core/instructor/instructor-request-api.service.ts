import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { InstructorRequest } from '../admin/admin.models';
import { API_BASE_URL } from '../api';

/** Demande de rôle formateur de l'utilisateur connecté (« Devenir formateur »). */
@Injectable({ providedIn: 'root' })
export class InstructorRequestApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/me/instructor-request`;

  /** Dernière demande (en attente, acceptée ou refusée) ; `null` s'il n'y en a jamais eu (204). */
  latest(): Observable<InstructorRequest | null> {
    return this.http.get<InstructorRequest>(this.url, { observe: 'response' }).pipe(map((res) => res.body ?? null));
  }

  submit(motivation: string): Observable<InstructorRequest> {
    return this.http.post<InstructorRequest>(this.url, { motivation: motivation.trim() || null });
  }
}
