import { Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { QuizApiService } from '../../core/quiz/quiz-api.service';
import { QuestionType, QuizView } from '../../core/quiz/quiz.models';

@Component({
  selector: 'app-quiz-editor',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './quiz-editor.component.html',
  styleUrl: './quiz-editor.component.scss',
})
export class QuizEditorComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(QuizApiService);
  private readonly route = inject(ActivatedRoute);

  readonly quiz = signal<QuizView | null>(null);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);

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
        position: quiz.questions.length + 1,
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
          this.flash('Question ajoutée.');
        },
        error: (err: { error?: { message?: string } }) =>
          this.error.set(err?.error?.message ?? "Échec de l'ajout."),
      });
  }

  deleteQuestion(questionId: number): void {
    if (!confirm('Supprimer cette question ?')) return;
    this.api.deleteQuestion(questionId).subscribe(() => this.reload());
  }

  private reload(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.view(id).subscribe({
      next: (quiz) => this.quiz.set(quiz),
      error: () => this.error.set('Quiz introuvable ou accès refusé.'),
    });
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
