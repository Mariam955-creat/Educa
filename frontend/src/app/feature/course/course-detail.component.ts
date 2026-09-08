import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { CourseApiService } from '../../core/courses/course-api.service';
import { ContentItem, CourseDetail } from '../../core/courses/course.models';
import { EnrollmentApiService } from '../../core/enrollments/enrollment-api.service';

@Component({
  selector: 'app-course-detail',
  imports: [RouterLink],
  templateUrl: './course-detail.component.html',
  styleUrl: './course-detail.component.scss',
})
export class CourseDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(CourseApiService);
  private readonly enrollmentApi = inject(EnrollmentApiService);

  readonly course = signal<CourseDetail | null>(null);
  readonly completedIds = signal<number[]>([]);
  readonly progressPercent = signal(0);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly enrolling = signal(false);

  readonly canEnroll = computed(() => {
    const c = this.course();
    return !!c && c.published && !c.contentsVisible;
  });

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug')!;
    this.reload(slug);
  }

  enroll(): void {
    const c = this.course();
    if (!c) return;
    this.enrolling.set(true);
    this.enrollmentApi.enroll(c.id).subscribe({
      next: () => this.reload(c.slug),
      error: () => this.enrolling.set(false),
    });
  }

  markComplete(content: ContentItem): void {
    this.enrollmentApi.completeContent(content.id).subscribe((progress) => {
      this.completedIds.set(progress.completedContentIds);
      this.progressPercent.set(progress.progressPercent);
    });
  }

  isDone(content: ContentItem): boolean {
    return this.completedIds().includes(content.id);
  }

  openFile(content: ContentItem): void {
    this.api.downloadFile(content.id).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank');
      setTimeout(() => URL.revokeObjectURL(url), 60_000);
    });
  }

  private reload(slug: string): void {
    this.loading.set(true);
    this.api.detail(slug).subscribe({
      next: (course) => {
        this.course.set(course);
        this.loading.set(false);
        this.enrolling.set(false);
        if (course.contentsVisible) {
          this.enrollmentApi.courseProgress(course.id).subscribe({
            next: (p) => {
              this.completedIds.set(p.completedContentIds);
              this.progressPercent.set(p.progressPercent);
            },
            error: () => undefined, // propriétaire non inscrit : pas de progression
          });
        }
      },
      error: (err: { status?: number }) => {
        this.error.set(err?.status === 404 ? 'Cours introuvable.' : 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }
}
