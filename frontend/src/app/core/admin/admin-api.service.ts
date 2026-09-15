import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api';
import { RoleName } from '../auth/auth.models';
import { AdminUser, CertificateRegistryEntry, Page } from './admin.models';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);
  private readonly base = API_BASE_URL;

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
}
