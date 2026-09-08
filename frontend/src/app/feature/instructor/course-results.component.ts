import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { CourseApiService } from '../../core/courses/course-api.service';
import { QuizApiService } from '../../core/quiz/quiz-api.service';
import { LearnerResult } from '../../core/quiz/quiz.models';

@Component({
  selector: 'app-course-results',
  imports: [RouterLink],
  template: `
    <section class="page">
      <a class="back" routerLink="/instructor">‹ Mes cours</a>
      <h1>Résultats — {{ title() }}</h1>

      @if (loading()) {
        <p class="muted">Chargement…</p>
      } @else if (rows().length === 0) {
        <p class="muted">Aucun apprenant inscrit.</p>
      } @else {
        <table>
          <thead>
            <tr>
              <th>Apprenant</th>
              <th>Moy. contrôles</th>
              <th>Examen final</th>
              <th>Note finale</th>
              <th>Certificat</th>
            </tr>
          </thead>
          <tbody>
            @for (r of rows(); track r.userId) {
              <tr>
                <td>{{ r.learnerName }}</td>
                <td>{{ r.controlsAverage ?? '—' }}</td>
                <td>{{ r.finalExamBestScore ?? '—' }}</td>
                <td>{{ r.finalGrade ?? '—' }}</td>
                <td>{{ r.certified ? '✅' : '—' }}</td>
              </tr>
            }
          </tbody>
        </table>
      }
    </section>
  `,
  styles: [
    `
      .page {
        max-width: 820px;
        margin: 0 auto;
        padding: 2rem 1rem;
      }
      .back {
        font-size: 0.85rem;
        text-decoration: none;
      }
      h1 {
        margin: 0.75rem 0 1.25rem;
        font-size: 1.4rem;
      }
      .muted {
        color: var(--muted);
      }
      table {
        width: 100%;
        border-collapse: collapse;
        background: var(--surface);
        border: 1px solid var(--border);
        border-radius: 12px;
        overflow: hidden;
      }
      th,
      td {
        text-align: left;
        padding: 0.65rem 0.9rem;
        border-bottom: 1px solid var(--border);
        font-size: 0.9rem;
      }
      th {
        background: #fafafa;
        font-size: 0.75rem;
        text-transform: uppercase;
        color: var(--muted);
      }
      tr:last-child td {
        border-bottom: 0;
      }
    `,
  ],
})
export class CourseResultsComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(CourseApiService);
  private readonly quizApi = inject(QuizApiService);

  readonly rows = signal<LearnerResult[]>([]);
  readonly title = signal('');
  readonly loading = signal(true);

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug')!;
    this.api.detail(slug).subscribe((course) => {
      this.title.set(course.title);
      this.quizApi.courseResults(course.id).subscribe({
        next: (list) => {
          this.rows.set(list);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
    });
  }
}
