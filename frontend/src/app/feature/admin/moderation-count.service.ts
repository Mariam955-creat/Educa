import { Injectable, inject, signal } from '@angular/core';

import { AdminApiService } from '../../core/admin/admin-api.service';

/**
 * Éléments en attente de modération : demandes de rôle formateur et cours soumis. Alimente les pastilles du menu
 * admin ; rafraîchi à l'ouverture de l'espace admin et après chaque décision.
 */
@Injectable({ providedIn: 'root' })
export class ModerationCountService {
  private readonly api = inject(AdminApiService);

  readonly instructorRequests = signal(0);
  readonly courseReviews = signal(0);

  refresh(): void {
    this.api.instructorRequestCount().subscribe({
      next: (res) => this.instructorRequests.set(res.pending),
      error: () => undefined,
    });
    this.api.courseReviewCount().subscribe({
      next: (res) => this.courseReviews.set(res.pending),
      error: () => undefined,
    });
  }
}
