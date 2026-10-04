import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { Enrollment } from '../../core/courses/course.models';
import { EnrollmentApiService } from '../../core/enrollments/enrollment-api.service';

/** Section « Mes cours » de l'espace compte : cours suivis et leur progression. */
@Component({
  selector: 'app-learner-dashboard',
  imports: [RouterLink, TranslatePipe],
  templateUrl: './learner-dashboard.component.html',
  styleUrl: './dashboard.scss',
})
export class LearnerDashboardComponent implements OnInit {
  private readonly enrollmentApi = inject(EnrollmentApiService);

  readonly enrollments = signal<Enrollment[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.enrollmentApi.myEnrollments().subscribe({
      next: (list) => {
        this.enrollments.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
