import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api';

export interface ChatTurn {
  role: 'user' | 'assistant';
  content: string;
  /** Réponse de repli (assistant indisponible). */
  degraded?: boolean;
}

/** Message de l'historique enregistré côté serveur. */
export interface ChatMessage extends ChatTurn {
  createdAt: string;
}

export interface AiReply {
  reply: string;
  degraded: boolean;
}

@Injectable({ providedIn: 'root' })
export class AiApiService {
  private readonly http = inject(HttpClient);

  /** Pose une question ; le serveur enregistre l'échange et fournit lui-même l'historique au modèle. */
  chat(courseId: number, message: string): Observable<AiReply> {
    return this.http.post<AiReply>(`${API_BASE_URL}/ai/chat`, { courseId, message });
  }

  /** Ma conversation avec l'assistant de ce cours (derniers messages, ordre chronologique). */
  history(courseId: number): Observable<ChatMessage[]> {
    const params = new HttpParams().set('courseId', courseId);
    return this.http.get<ChatMessage[]>(`${API_BASE_URL}/ai/chat/history`, { params });
  }

  clearHistory(courseId: number): Observable<void> {
    const params = new HttpParams().set('courseId', courseId);
    return this.http.delete<void>(`${API_BASE_URL}/ai/chat/history`, { params });
  }
}
