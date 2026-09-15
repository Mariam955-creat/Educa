import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { CourseApiService } from '../../core/courses/course-api.service';
import { ContentType, CourseDetail } from '../../core/courses/course.models';
import { CourseLanguage, LanguageApiService } from '../../core/language/language-api.service';
import { QuizApiService } from '../../core/quiz/quiz-api.service';
import { CourseQuizzes, QuizRef } from '../../core/quiz/quiz.models';

interface ChapterTranslationDraft {
  chapterId: number;
  originalTitle: string;
  title: string;
}

interface TranslationDraft {
  courseTitle: string;
  courseDescription: string;
  chapters: ChapterTranslationDraft[];
}

@Component({
  selector: 'app-course-editor',
  imports: [FormsModule, ReactiveFormsModule, RouterLink, TranslatePipe],
  templateUrl: './course-editor.component.html',
  styleUrl: './course-editor.component.scss',
})
export class CourseEditorComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(CourseApiService);
  private readonly quizApi = inject(QuizApiService);
  private readonly languageApi = inject(LanguageApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  readonly course = signal<CourseDetail | null>(null);
  readonly quizzes = signal<CourseQuizzes | null>(null);
  readonly languages = signal<CourseLanguage[]>([]);
  readonly isNew = computed(() => this.course() === null);
  /** Langues actives + langue déjà choisie par ce cours, même si désactivée depuis (évite de perdre la sélection visuelle). */
  readonly selectableLanguages = computed<CourseLanguage[]>(() => {
    const active = this.languages();
    const current = this.course()?.language;
    if (!current || active.some((l) => l.code === current)) return active;
    return [...active, { code: current, name: current.toUpperCase(), active: false }];
  });
  readonly message = signal<string | null>(null);
  readonly messageIsError = signal(false);
  readonly pickedFile = signal<File | null>(null);

  readonly translationLanguages = signal<string[]>([]);
  readonly selectedTranslationLang = signal('');
  readonly translationDraft = signal<TranslationDraft | null>(null);
  readonly translationLoading = signal(false);

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
    this.languageApi.active().subscribe({ next: (list) => this.languages.set(list), error: () => undefined });
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

    request$.subscribe({
      next: (summary) => {
        this.flash(this.translate.instant('courseEditor.saved'));
        if (!current) {
          void this.router.navigate(['/instructor/courses', summary.slug, 'edit']);
        } else {
          this.loadCourse(summary.slug);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.flash(err.error?.message ?? this.translate.instant('courseEditor.saveError'), true);
      },
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
    if (!c || !confirm(this.translate.instant('courseEditor.confirmDeleteChapter'))) return;
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

  selectTranslationLang(lang: string): void {
    this.selectedTranslationLang.set(lang);
    const c = this.course();
    if (!c || !lang) {
      this.translationDraft.set(null);
      return;
    }
    this.translationLoading.set(true);
    this.api.translation(c.id, lang).subscribe({
      next: (edit) => {
        this.translationDraft.set({
          courseTitle: edit.courseTitle ?? '',
          courseDescription: edit.courseDescription ?? '',
          chapters: edit.chapters.map((ch) => ({
            chapterId: ch.chapterId,
            originalTitle: ch.originalTitle,
            title: ch.translatedTitle ?? '',
          })),
        });
        this.translationLoading.set(false);
      },
      error: () => this.translationLoading.set(false),
    });
  }

  saveTranslation(): void {
    const c = this.course();
    const draft = this.translationDraft();
    const lang = this.selectedTranslationLang();
    if (!c || !draft || !lang || !draft.courseTitle.trim()) return;

    this.api
      .saveTranslation(c.id, lang, {
        courseTitle: draft.courseTitle,
        courseDescription: draft.courseDescription,
        chapters: draft.chapters.map((ch) => ({ chapterId: ch.chapterId, title: ch.title })),
      })
      .subscribe({
        next: () => {
          this.flash(this.translate.instant('courseEditor.translations.saved'));
          this.loadTranslationLanguages(c.id);
        },
        error: (err: HttpErrorResponse) => {
          this.flash(
            err.error?.message ?? this.translate.instant('courseEditor.translations.saveError'),
            true,
          );
        },
      });
  }

  deleteTranslation(): void {
    const c = this.course();
    const lang = this.selectedTranslationLang();
    if (!c || !lang) return;
    if (!confirm(this.translate.instant('courseEditor.translations.confirmDelete'))) return;

    this.api.deleteTranslation(c.id, lang).subscribe(() => {
      this.translationDraft.set(null);
      this.selectedTranslationLang.set('');
      this.loadTranslationLanguages(c.id);
    });
  }

  private loadTranslationLanguages(courseId: number): void {
    this.api.translationLanguages(courseId).subscribe({
      next: (list) => this.translationLanguages.set(list),
      error: () => undefined,
    });
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
      this.selectedTranslationLang.set('');
      this.translationDraft.set(null);
      this.loadTranslationLanguages(course.id);
    });
  }

  private flash(text: string, isError = false): void {
    this.message.set(text);
    this.messageIsError.set(isError);
    setTimeout(() => this.message.set(null), isError ? 4000 : 2500);
  }
}
