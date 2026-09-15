import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api';

export interface CourseLanguage {
  code: string;
  name: string;
  active: boolean;
}

/** Langues disponibles pour le contenu des cours (distinct de la langue d'interface, voir LanguageService). */
@Injectable({ providedIn: 'root' })
export class LanguageApiService {
  private readonly http = inject(HttpClient);
  private readonly base = API_BASE_URL;

  active(): Observable<CourseLanguage[]> {
    return this.http.get<CourseLanguage[]>(`${this.base}/languages`);
  }
}
