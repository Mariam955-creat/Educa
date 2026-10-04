import { Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { CourseDetail } from '../../core/courses/course.models';

/** Page cours : « Ce que vous apprendrez », prérequis et public visé (affichés seulement s'ils sont renseignés). */
@Component({
  selector: 'app-course-presentation',
  imports: [TranslatePipe],
  template: `
    @if (course().objectives.length) {
      <section class="learn">
        <h2>{{ 'coursePage.objectives' | translate }}</h2>
        <ul>
          @for (item of course().objectives; track $index) {
            <li><span aria-hidden="true">✓</span>{{ item }}</li>
          }
        </ul>
      </section>
    }

    @if (course().prerequisites.length || course().targetAudience) {
      <div class="columns">
        @if (course().prerequisites.length) {
          <section>
            <h2>{{ 'coursePage.prerequisites' | translate }}</h2>
            <ul class="plain">
              @for (item of course().prerequisites; track $index) {
                <li>{{ item }}</li>
              }
            </ul>
          </section>
        }
        @if (course().targetAudience) {
          <section>
            <h2>{{ 'coursePage.audience' | translate }}</h2>
            <p>{{ course().targetAudience }}</p>
          </section>
        }
      </div>
    }
  `,
  styles: `
    :host { display: block; margin-bottom: 1.5rem; }
    h2 { margin: 0 0 0.75rem; font-size: 1.1rem; }
    .learn { margin-bottom: 1.25rem; padding: 1.1rem 1.25rem; border: 1px solid var(--border); border-radius: 12px; background: var(--surface); }
    .learn ul { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(260px, 100%), 1fr)); gap: 0.5rem 1.5rem; margin: 0; padding: 0; list-style: none; }
    .learn li { display: flex; gap: 0.6rem; font-size: 0.92rem; line-height: 1.45; }
    .learn li span { color: var(--accent); font-weight: 800; }
    .columns { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(280px, 100%), 1fr)); gap: 1.25rem; }
    .plain { margin: 0; padding-inline-start: 1.1rem; font-size: 0.92rem; line-height: 1.6; }
    p { margin: 0; font-size: 0.92rem; line-height: 1.55; white-space: pre-line; }
  `,
})
export class CoursePresentationComponent {
  readonly course = input.required<CourseDetail>();
}
