import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { CourseApiService } from '../../core/courses/course-api.service';
import { ContentType, CourseDetail } from '../../core/courses/course.models';
import { QuizApiService } from '../../core/quiz/quiz-api.service';
import { CourseQuizzes, QuizRef } from '../../core/quiz/quiz.models';

@Component({
  selector: 'app-course-editor',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './course-editor.component.html',
  styleUrl: './course-editor.component.scss',
})
export class CourseEditorComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(CourseApiService);
  private readonly quizApi = inject(QuizApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly course = signal<CourseDetail | null>(null);
  readonly quizzes = signal<CourseQuizzes | null>(null);
  readonly isNew = computed(() => this.course() === null);
  readonly message = signal<string | null>(null);
  readonly pickedFile = signal<File | null>(null);

  controlFor(chapterId: number): QuizRef | undefined {
    return this.quizzes()?.controls.find((q) => q.chapterId === chapterId);
  }

  createControl(chapterId: number): void {
    this.quizApi.createControl(chapterId, 'Contrôle de chapitre').subscribe((q) => {
      void this.router.navigate(['/instructor/quizzes', q.id, 'edit']);
    });
  }

  createFinalExam(): void {
    const c = this.course();
    if (!c) return;
    this.quizApi.createFinalExam(c.id, 'Examen final').subscribe((q) => {
      void this.router.navigate(['/instructor/quizzes', q.id, 'edit']);
    });
  }

  readonly courseForm = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: [''],
    language: ['fr', [Validators.required]],
  });

  readonly chapterForm = this.fb.nonNullable.group({
    title: ['', [Validators.required]],
    position: [1, [Validators.required, Validators.min(1)]],
  });

  readonly contentForm = this.fb.nonNullable.group({
    chapterId: [0, [Validators.required, Validators.min(1)]],
    type: ['TEXT' as ContentType, [Validators.required]],
    title: ['', [Validators.required]],
    position: [1, [Validators.required, Validators.min(1)]],
    textBody: [''],
  });

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug');
    if (slug) {
      this.loadCourse(slug);
    }
  }

  saveCourse(): void {
    if (this.courseForm.invalid) {
      this.courseForm.markAllAsTouched();
      return;
    }
    const value = this.courseForm.getRawValue();
    const current = this.course();
    const request$ = current
      ? this.api.updateCourse(current.id, value)
      : this.api.createCourse(value);

    request$.subscribe((summary) => {
      this.flash('Cours enregistré.');
      if (!current) {
        void this.router.navigate(['/instructor/courses', summary.slug, 'edit']);
      } else {
        this.loadCourse(summary.slug);
      }
    });
  }

  addChapter(): void {
    const c = this.course();
    if (!c || this.chapterForm.invalid) {
      this.chapterForm.markAllAsTouched();
      return;
    }
    this.api.addChapter(c.id, this.chapterForm.getRawValue()).subscribe(() => {
      this.chapterForm.reset({ title: '', position: c.chapters.length + 1 });
      this.loadCourse(c.slug);
    });
  }

  deleteChapter(chapterId: number): void {
    const c = this.course();
    if (!c || !confirm('Supprimer ce chapitre et ses contenus ?')) return;
    this.api.deleteChapter(chapterId).subscribe(() => this.loadCourse(c.slug));
  }

  onFilePicked(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.pickedFile.set(input.files?.[0] ?? null);
  }

  addContent(): void {
    const c = this.course();
    if (!c || this.contentForm.invalid) {
      this.contentForm.markAllAsTouched();
      return;
    }
    const value = this.contentForm.getRawValue();
    this.api
      .addContent(value.chapterId, {
        type: value.type,
        title: value.title,
        position: value.position,
        textBody: value.type === 'TEXT' ? value.textBody : undefined,
      })
      .subscribe((content) => {
        const file = this.pickedFile();
        if (value.type !== 'TEXT' && file) {
          this.api.uploadFile(content.id, file).subscribe(() => this.afterContentAdded(c.slug));
        } else {
          this.afterContentAdded(c.slug);
        }
      });
  }

  deleteContent(contentId: number): void {
    const c = this.course();
    if (!c) return;
    this.api.deleteContent(contentId).subscribe(() => this.loadCourse(c.slug));
  }

  private afterContentAdded(slug: string): void {
    this.contentForm.patchValue({ title: '', textBody: '' });
    this.pickedFile.set(null);
    this.loadCourse(slug);
  }

  private loadCourse(slug: string): void {
    this.api.detail(slug).subscribe((course) => {
      this.course.set(course);
      this.courseForm.patchValue({
        title: course.title,
        description: course.description ?? '',
        language: course.language,
      });
      if (course.chapters.length > 0 && this.contentForm.controls.chapterId.value === 0) {
        this.contentForm.patchValue({ chapterId: course.chapters[0].id });
      }
      this.quizApi.courseQuizzes(course.id).subscribe({
        next: (q) => this.quizzes.set(q),
        error: () => this.quizzes.set({ controls: [], finalExam: null }),
      });
    });
  }

  private flash(text: string): void {
    this.message.set(text);
    setTimeout(() => this.message.set(null), 2500);
  }
}
