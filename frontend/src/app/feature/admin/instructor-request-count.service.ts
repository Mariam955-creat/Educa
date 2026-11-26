import { Injectable, inject, signal } from '@angular/core';

import { AdminApiService } from '../../core/admin/admin-api.service';

/** Nombre de demandes de rôle formateur en attente : pastille du menu admin, mise à jour après chaque décision. */
@Injectable({ providedIn: 'root' })
export class InstructorRequestCountService {
  private readonly api = inject(AdminApiService);

  readonly pending = signal(0);

  refresh(): void {
    this.api.instructorRequestCount().subscribe({
      next: (res) => this.pending.set(res.pending),
      error: () => undefined,
    });
  }
}
