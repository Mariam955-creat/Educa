import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { QuizApiService } from '../../core/quiz/quiz-api.service';
import { AnswerInput, AttemptResult, QuestionView, QuizView } from '../../core/quiz/quiz.models';

@Component({
  selector: 'app-quiz-take',
  imports: [RouterLink],
  templateUrl: './quiz-take.component.html',
  styleUrl: './quiz-take.component.scss',
})
export class QuizTakeComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(QuizApiService);

  readonly quiz = signal<QuizView | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly result = signal<AttemptResult | null>(null);
  readonly submitting = signal(false);

  /** questionId -> Set des optionIds sélectionnés */
  private readonly selection = signal<Map<number, Set<number>>>(new Map());

  readonly isFinalExam = computed(() => this.quiz()?.type === 'FINAL_EXAM');

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.view(id).subscribe({
      next: (quiz) => {
        this.quiz.set(quiz);
        this.loading.set(false);
      },
      error: (err: { status?: number; error?: { message?: string } }) => {
        this.error.set(err?.error?.message ?? (err?.status === 403 ? 'Accès refusé.' : 'Erreur de chargement.'));
        this.loading.set(false);
      },
    });
  }

  isChecked(question: QuestionView, optionId: number): boolean {
    return this.selection().get(question.id)?.has(optionId) ?? false;
  }

  toggle(question: QuestionView, optionId: number): void {
    const map = new Map(this.selection());
    const single = question.type !== 'MULTIPLE_CHOICE';
    const current = single ? new Set<number>() : new Set(map.get(question.id) ?? []);
    if (current.has(optionId)) {
      current.delete(optionId);
    } else {
      current.add(optionId);
    }
    map.set(question.id, current);
    this.selection.set(map);
  }

  submit(): void {
    const quiz = this.quiz();
    if (!quiz) return;
    this.submitting.set(true);
    this.error.set(null);
    const answers: AnswerInput[] = quiz.questions.map((q) => ({
      questionId: q.id,
      selectedOptionIds: Array.from(this.selection().get(q.id) ?? []),
    }));
    this.api.submit(quiz.id, answers).subscribe({
      next: (res) => {
        this.result.set(res);
        this.submitting.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.error.set(err?.error?.message ?? "Échec de l'envoi.");
        this.submitting.set(false);
      },
    });
  }

  retry(): void {
    this.result.set(null);
    this.selection.set(new Map());
  }

  backToCourse(): void {
    void this.router.navigate(['/dashboard']);
  }
}
