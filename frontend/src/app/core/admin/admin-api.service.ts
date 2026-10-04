import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api';
import { RoleName } from '../auth/auth.models';
import { CourseSummary, TrashedCourse } from '../courses/course.models';
import { CourseLanguage } from '../language/language-api.service';
import {
  AdminStats,
  AdminUser,
  CertificateRegistryEntry,
  Page,
  PaymentRegistryEntry,
  ReviewRegistryEntry,
  TrashedUser,
} from './admin.models';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);
  private readonly base = API_BASE_URL;

  stats(): Observable<AdminStats> {
    return this.http.get<AdminStats>(`${this.base}/admin/stats`);
  }

  /** @param published `true` publiés, `false` brouillons, absent = tous */
  courses(q?: string, published?: boolean, page = 0, size = 20): Observable<Page<CourseSummary>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q) params = params.set('q', q);
    if (published !== undefined) params = params.set('published', published);
    return this.http.get<Page<CourseSummary>>(`${this.base}/admin/courses`, { params });
  }

  users(q?: string, page = 0, size = 50): Observable<Page<AdminUser>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q) params = params.set('q', q);
    return this.http.get<Page<AdminUser>>(`${this.base}/admin/users`, { params });
  }

  updateRoles(id: number, roles: RoleName[]): Observable<AdminUser> {
    return this.http.patch<AdminUser>(`${this.base}/admin/users/${id}/roles`, { roles });
  }

  updateStatus(id: number, enabled: boolean): Observable<AdminUser> {
    return this.http.patch<AdminUser>(`${this.base}/admin/users/${id}/status`, { enabled });
  }

  certificateRegistry(page = 0, size = 50): Observable<Page<CertificateRegistryEntry>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<CertificateRegistryEntry>>(`${this.base}/admin/certificates`, { params });
  }

  paymentRegistry(page = 0, size = 50): Observable<Page<PaymentRegistryEntry>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<PaymentRegistryEntry>>(`${this.base}/admin/payments`, { params });
  }

  reviewRegistry(page = 0, size = 50): Observable<Page<ReviewRegistryEntry>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<ReviewRegistryEntry>>(`${this.base}/admin/reviews`, { params });
  }

  deleteReview(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/admin/reviews/${id}`);
  }

  // ---------- corbeille ----------

  courseTrash(): Observable<TrashedCourse[]> {
    return this.http.get<TrashedCourse[]>(`${this.base}/admin/trash/courses`);
  }

  /** Met le compte à la corbeille (connexion bloquée, restaurable). */
  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/admin/users/${id}`);
  }

  userTrash(): Observable<TrashedUser[]> {
    return this.http.get<TrashedUser[]>(`${this.base}/admin/users/trash`);
  }

  restoreUser(id: number): Observable<AdminUser> {
    return this.http.post<AdminUser>(`${this.base}/admin/users/${id}/restore`, {});
  }

  deleteUserPermanently(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/admin/users/${id}/permanent`);
  }

  reviewTrash(): Observable<ReviewRegistryEntry[]> {
    return this.http.get<ReviewRegistryEntry[]>(`${this.base}/admin/reviews/trash`);
  }

  restoreReview(id: number): Observable<void> {
    return this.http.post<void>(`${this.base}/admin/reviews/${id}/restore`, {});
  }

  deleteReviewPermanently(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/admin/reviews/${id}/permanent`);
  }

  languages(): Observable<CourseLanguage[]> {
    return this.http.get<CourseLanguage[]>(`${this.base}/admin/languages`);
  }

  setLanguageActive(code: string, active: boolean): Observable<CourseLanguage> {
    return this.http.patch<CourseLanguage>(`${this.base}/admin/languages/${code}`, { active });
  }
}
