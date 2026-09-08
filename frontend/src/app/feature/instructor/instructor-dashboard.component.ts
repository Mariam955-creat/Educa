import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CourseApiService } from '../../core/courses/course-api.service';
import { CourseSummary } from '../../core/courses/course.models';

@Component({
  selector: 'app-instructor-dashboard',
  imports: [RouterLink],
  templateUrl: './instructor-dashboard.component.html',
  styleUrl: './instructor-dashboard.component.scss',
})
export class InstructorDashboardComponent implements OnInit {
  private readonly api = inject(CourseApiService);

  readonly courses = signal<CourseSummary[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.load();
  }

  togglePublish(course: CourseSummary): void {
    this.api.setPublished(course.id, !course.published).subscribe(() => this.load());
  }

  remove(course: CourseSummary): void {
    if (!confirm(`Supprimer « ${course.title} » ? Cette action est définitive.`)) return;
    this.api.deleteCourse(course.id).subscribe(() => this.load());
  }

  private load(): void {
    this.loading.set(true);
    this.api.myCourses().subscribe({
      next: (list) => {
        this.courses.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
