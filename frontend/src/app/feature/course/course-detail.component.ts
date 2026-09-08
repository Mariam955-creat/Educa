import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { CertificateApiService } from '../../core/certificates/certificate-api.service';
import { CourseApiService } from '../../core/courses/course-api.service';
import { ContentItem, CourseDetail } from '../../core/courses/course.models';
import { EnrollmentApiService } from '../../core/enrollments/enrollment-api.service';
import { QuizApiService } from '../../core/quiz/quiz-api.service';
import { CourseGrade, CourseQuizzes, QuizRef } from '../../core/quiz/quiz.models';

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
  private readonly quizApi = inject(QuizApiService);
  private readonly certificateApi = inject(CertificateApiService);

  readonly course = signal<CourseDetail | null>(null);
  readonly completedIds = signal<number[]>([]);
  readonly progressPercent = signal(0);
  readonly quizzes = signal<CourseQuizzes | null>(null);
  readonly grade = signal<CourseGrade | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly enrolling = signal(false);

  readonly canEnroll = computed(() => {
    const c = this.course();
    return !!c && c.published && !c.contentsVisible;
  });

  ngOnInit(): void {
    this.reload(this.route.snapshot.paramMap.get('slug')!);
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
      this.refreshGrade();
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

  controlFor(chapterId: number): QuizRef | undefined {
    return this.quizzes()?.controls.find((q) => q.chapterId === chapterId);
  }

  controlBest(quizId: number): number | null {
    const control = this.grade()?.controls.find((c) => c.quizId === quizId);
    return control && control.attempts > 0 ? control.bestScore : null;
  }

  downloadCertificate(): void {
    const id = this.grade()?.certificateId;
    if (!id) return;
    this.certificateApi.download(id).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `certificat-${id}.pdf`;
      a.click();
      setTimeout(() => URL.revokeObjectURL(url), 10_000);
    });
  }

  private refreshGrade(): void {
    const c = this.course();
    if (c?.contentsVisible) {
      this.quizApi.courseGrade(c.id).subscribe({ next: (g) => this.grade.set(g), error: () => undefined });
    }
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
            error: () => undefined,
          });
          this.quizApi.courseQuizzes(course.id).subscribe({ next: (q) => this.quizzes.set(q), error: () => undefined });
          this.refreshGrade();
        }
      },
      error: (err: { status?: number }) => {
        this.error.set(err?.status === 404 ? 'Cours introuvable.' : 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }
}
