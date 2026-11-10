import { Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { QuizApiService } from '../../core/quiz/quiz-api.service';
import { QuestionInput, QuestionType, QuizView } from '../../core/quiz/quiz.models';

@Component({
  selector: 'app-quiz-editor',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe],
  templateUrl: './quiz-editor.component.html',
  styleUrl: './quiz-editor.component.scss',
})
export class QuizEditorComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(QuizApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly translate = inject(TranslateService);

  readonly quiz = signal<QuizView | null>(null);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  /** Questions proposées par l'IA, en attente de relecture (rien n'est enregistré avant « Ajouter »). */
  readonly suggestions = signal<QuestionInput[]>([]);
  readonly suggesting = signal(false);
  readonly adding = signal(false);
  readonly suggestError = signal<string | null>(null);
  readonly suggestCounts = [3, 5, 10];
  readonly suggestCount = new FormControl(5, { nonNullable: true });

  readonly form = this.fb.nonNullable.group({
    statement: ['', [Validators.required]],
    type: ['SINGLE_CHOICE' as QuestionType, [Validators.required]],
    points: [1, [Validators.required, Validators.min(1)]],
    options: this.fb.array([this.optionGroup(), this.optionGroup()]),
  });

  get options(): FormArray {
    return this.form.controls.options;
  }

  ngOnInit(): void {
    this.reload();
  }

  addOption(): void {
    if (this.options.length < 6) this.options.push(this.optionGroup());
  }

  removeOption(i: number): void {
    if (this.options.length > 2) this.options.removeAt(i);
  }

  submit(): void {
    const quiz = this.quiz();
    if (!quiz || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.error.set(null);
    this.api
      .addQuestion(quiz.id, {
        statement: value.statement,
        type: value.type,
        points: value.points,
        position: this.nextPosition(quiz),
        options: value.options.map((o, idx) => ({
          label: o.label,
          correct: o.correct,
          position: idx + 1,
        })),
      })
      .subscribe({
        next: (updated) => {
          this.quiz.set(updated);
          this.form.reset({ statement: '', type: 'SINGLE_CHOICE', points: 1 });
          this.options.clear();
          this.options.push(this.optionGroup());
          this.options.push(this.optionGroup());
          this.flash(this.translate.instant('quizEditor.added'));
        },
        error: (err: { error?: { message?: string } }) =>
          this.error.set(err?.error?.message ?? this.translate.instant('quizEditor.addFailed')),
      });
  }

  suggest(): void {
    const quiz = this.quiz();
    if (!quiz || this.suggesting()) return;
    this.suggesting.set(true);
    this.suggestError.set(null);
    this.api.suggestQuestions(quiz.id, this.suggestCount.value).subscribe({
      next: (questions) => {
        this.suggestions.set(questions);
        if (questions.length === 0) this.suggestError.set(this.translate.instant('quizEditor.ai.none'));
        this.suggesting.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.suggestError.set(err?.error?.message ?? this.translate.instant('quizEditor.ai.failed'));
        this.suggesting.set(false);
      },
    });
  }

  /** Ajoute une suggestion au quiz, à la suite des questions existantes. */
  accept(suggestion: QuestionInput, then?: () => void): void {
    const quiz = this.quiz();
    if (!quiz) return;
    this.adding.set(true);
    this.suggestError.set(null);
    this.api.addQuestion(quiz.id, { ...suggestion, position: this.nextPosition(quiz) }).subscribe({
      next: (updated) => {
        this.quiz.set(updated);
        this.dismiss(suggestion);
        this.adding.set(false);
        then?.();
      },
      error: (err: { error?: { message?: string } }) => {
        this.suggestError.set(err?.error?.message ?? this.translate.instant('quizEditor.addFailed'));
        this.adding.set(false);
      },
    });
  }

  /** Ajoute les suggestions restantes une par une (chaque position dépend de la précédente). */
  acceptAll(): void {
    const next = this.suggestions()[0];
    if (next) this.accept(next, () => this.acceptAll());
    else this.flash(this.translate.instant('quizEditor.added'));
  }

  dismiss(suggestion: QuestionInput): void {
    this.suggestions.update((list) => list.filter((s) => s !== suggestion));
  }

  deleteQuestion(questionId: number): void {
    if (!confirm(this.translate.instant('quizEditor.confirmDeleteQuestion'))) return;
    this.api.deleteQuestion(questionId).subscribe(() => this.reload());
  }

  private reload(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.view(id).subscribe({
      next: (quiz) => this.quiz.set(quiz),
      error: () => this.error.set(this.translate.instant('quizEditor.notFound')),
    });
  }

  private nextPosition(quiz: QuizView): number {
    return quiz.questions.reduce((max, q) => Math.max(max, q.position), 0) + 1;
  }

  private optionGroup() {
    return this.fb.nonNullable.group({
      label: ['', [Validators.required]],
      correct: [false],
    });
  }

  private flash(text: string): void {
    this.message.set(text);
    setTimeout(() => this.message.set(null), 2000);
  }
}
