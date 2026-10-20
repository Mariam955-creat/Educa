import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { CourseApiService } from '../../core/courses/course-api.service';
import { CourseSummary } from '../../core/courses/course.models';
import { CourseCardComponent } from '../../shared/course-card/course-card.component';

/** Page d'accueil publique : présentation de la plateforme et aperçu du catalogue. */
@Component({
  selector: 'app-home',
  imports: [RouterLink, TranslatePipe, CourseCardComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit {
  private readonly api = inject(CourseApiService);

  readonly courses = signal<CourseSummary[]>([]);
  readonly features = ['courses', 'quiz', 'certificate', 'ai'] as const;
  readonly featureIcons: Record<string, string> = { courses: '📚', quiz: '✅', certificate: '🎓', ai: '🤖' };
  readonly steps = ['one', 'two', 'three', 'four'] as const;

  ngOnInit(): void {
    // L'aperçu est facultatif : en cas d'erreur, la section reste simplement masquée
    this.api.catalog().subscribe({
      next: (page) => this.courses.set(page.content.slice(0, 4)),
      error: () => this.courses.set([]),
    });
  }
}
