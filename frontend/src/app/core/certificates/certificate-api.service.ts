import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api';
import { LanguageService } from '../i18n/language.service';

export interface Certificate {
  id: number;
  serialNumber: string;
  verificationCode: string;
  courseId: number;
  courseTitle: string;
  holderName: string;
  controlsAverage: number;
  finalExamScore: number;
  finalGrade: number;
  issuedAt: string;
}

export interface CertificateVerification {
  valid: boolean;
  serialNumber: string | null;
  holderName: string | null;
  courseTitle: string | null;
  finalGrade: number | null;
  issuedAt: string | null;
}

@Injectable({ providedIn: 'root' })
export class CertificateApiService {
  private readonly http = inject(HttpClient);
  private readonly lang = inject(LanguageService);
  private readonly base = API_BASE_URL;

  mine(): Observable<Certificate[]> {
    return this.http.get<Certificate[]>(`${this.base}/certificates/me`);
  }

  download(id: number): Observable<Blob> {
    // PDF dans la langue de l'interface (le backend retombe sinon sur la langue du titulaire).
    return this.http.get(`${this.base}/certificates/${id}/download`, {
      params: { lang: this.lang.current() },
      responseType: 'blob',
    });
  }

  verify(code: string): Observable<CertificateVerification> {
    return this.http.get<CertificateVerification>(`${this.base}/certificates/verify/${code}`);
  }
}
