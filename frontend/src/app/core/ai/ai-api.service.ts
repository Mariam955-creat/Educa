import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api';

export interface ChatTurn {
  role: 'user' | 'assistant';
  content: string;
}

export interface AiReply {
  reply: string;
  degraded: boolean;
}

@Injectable({ providedIn: 'root' })
export class AiApiService {
  private readonly http = inject(HttpClient);

  chat(courseId: number, message: string, history: ChatTurn[]): Observable<AiReply> {
    return this.http.post<AiReply>(`${API_BASE_URL}/ai/chat`, { courseId, message, history });
  }
}
