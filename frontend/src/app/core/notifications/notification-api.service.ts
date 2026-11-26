import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api';

export interface AppNotification {
  id: number;
  /** Type traduit par l'interface : `notifications.types.<type>`. */
  type: string;
  /** Objet inséré dans le texte (ex. titre du cours concerné). */
  subject: string | null;
  /** Information libre affichée avec le texte (ex. motif d'un refus). */
  detail: string | null;
  /** Route ouverte au clic. */
  link: string | null;
  read: boolean;
  createdAt: string;
}

export interface NotificationList {
  items: AppNotification[];
  unreadCount: number;
}

/** Notifications de l'utilisateur connecté (cloche de l'en-tête). */
@Injectable({ providedIn: 'root' })
export class NotificationApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/me/notifications`;

  list(): Observable<NotificationList> {
    return this.http.get<NotificationList>(this.url);
  }

  markRead(id: number): Observable<void> {
    return this.http.post<void>(`${this.url}/${id}/read`, {});
  }

  markAllRead(): Observable<void> {
    return this.http.post<void>(`${this.url}/read-all`, {});
  }
}
