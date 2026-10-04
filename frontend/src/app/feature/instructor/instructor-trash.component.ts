import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { CourseApiService } from '../../core/courses/course-api.service';
import { TrashedCourse } from '../../core/courses/course.models';
import { LanguageService } from '../../core/i18n/language.service';
import { LocalDatePipe } from '../../core/i18n/local-date.pipe';
import { fallbackCover } from '../../shared/course-cover';
import { ConfirmDeleteDialogComponent } from '../../shared/trash/confirm-delete-dialog.component';

/** Corbeille du formateur : ses cours supprimés, à restaurer ou à supprimer définitivement. */
@Component({
  selector: 'app-instructor-trash',
  imports: [TranslatePipe, LocalDatePipe, ConfirmDeleteDialogComponent],
  template: `
    <section class="page">
      <h1>🗑 {{ 'trash.title' | translate }}</h1>
      <p class="subtitle">{{ 'trash.instructorHint' | translate }}</p>

      @if (message(); as m) {
        <p class="flash" [class.error]="m.error">{{ m.text }}</p>
      }

      @if (loading()) {
        <p class="muted">{{ 'common.loading' | translate }}</p>
      } @else if (courses().length === 0) {
        <div class="empty">
          <span aria-hidden="true">🗑</span>
          <p>{{ 'trash.empty' | translate }}</p>
        </div>
      } @else {
        @for (c of courses(); track c.id) {
          <article class="item">
            <span class="thumb" [style.background]="c.coverImageUrl ? null : fallbackCover(c.id)">
              @if (api.coverSrc(c.coverImageUrl); as src) {
                <img [src]="src" alt="" />
              } @else {
                {{ c.title.charAt(0) }}
              }
            </span>
            <div class="info">
              <strong>{{ c.title }}</strong>
              <span class="muted">{{ 'trash.deletedOn' | translate: { date: (c.deletedAt | localDate: 'long' : lang.current()) } }}</span>
              @if (c.learnerCount > 0) {
                <span class="hint">🔒 {{ 'trash.hasLearners' | translate: { count: c.learnerCount } }}</span>
              }
            </div>
            <div class="actions">
              <button type="button" class="restore" (click)="restore(c)">↩ {{ 'trash.restore' | translate }}</button>
              <button type="button" class="danger" [disabled]="c.learnerCount > 0" (click)="toDelete.set(c)">
                {{ 'trash.deleteForever' | translate }}
              </button>
            </div>
          </article>
        }
      }
    </section>

    <app-confirm-delete-dialog
      [open]="toDelete() !== null"
      [itemName]="toDelete()?.title ?? ''"
      [busy]="deleting()"
      (cancel)="toDelete.set(null)"
      (confirmed)="deletePermanently()"
    />
  `,
  styles: `
    .page { max-width: 900px; margin: 0 auto; padding: 2rem 1.25rem; }
    h1 { margin: 0; font-size: 1.5rem; }
    .subtitle { margin: 0.3rem 0 1.25rem; color: var(--muted); font-size: 0.9rem; }
    .muted { color: var(--muted); font-size: 0.82rem; }
    .flash { padding: 0.55rem 0.8rem; border-radius: 8px; background: var(--success-soft, #d1fae5); color: var(--success-strong, #047857); font-size: 0.85rem; }
    .flash.error { background: #fee2e2; color: #991b1b; }
    .empty { display: flex; flex-direction: column; align-items: center; padding: 3rem 1rem; border: 1px dashed var(--border); border-radius: 14px; color: var(--muted); }
    .empty span { font-size: 2.2rem; }
    .item { display: flex; flex-wrap: wrap; align-items: center; gap: 1rem; margin-bottom: 0.75rem; padding: 0.9rem 1rem; border: 1px solid var(--border); border-radius: 12px; background: var(--surface); }
    .thumb { display: grid; place-items: center; flex-shrink: 0; width: 5rem; height: 3rem; overflow: hidden; border-radius: 8px; color: #fff; font-weight: 800; opacity: 0.7; }
    .thumb img { width: 100%; height: 100%; object-fit: cover; }
    .info { display: flex; flex: 1; flex-direction: column; gap: 0.15rem; min-width: 12rem; }
    .hint { color: #92400e; font-size: 0.8rem; }
    .actions { display: flex; gap: 0.4rem; }
    button { padding: 0.4rem 0.75rem; border-radius: 6px; font: inherit; font-size: 0.82rem; cursor: pointer; }
    .restore { border: 1px solid var(--accent, #2563eb); background: transparent; color: var(--accent, #2563eb); font-weight: 600; }
    .danger { border: 1px solid #fecaca; background: transparent; color: #b91c1c; }
    .danger:disabled { opacity: 0.45; cursor: not-allowed; }
  `,
})
export class InstructorTrashComponent implements OnInit {
  readonly api = inject(CourseApiService);
  private readonly translate = inject(TranslateService);
  readonly lang = inject(LanguageService);
  readonly fallbackCover = fallbackCover;

  readonly courses = signal<TrashedCourse[]>([]);
  readonly loading = signal(true);
  readonly toDelete = signal<TrashedCourse | null>(null);
  readonly deleting = signal(false);
  readonly message = signal<{ text: string; error: boolean } | null>(null);

  ngOnInit(): void {
    this.load();
  }

  restore(course: TrashedCourse): void {
    this.api.restoreCourse(course.id).subscribe({
      next: () => {
        this.flash(this.translate.instant('trash.restoredCourse', { name: course.title }));
        this.load();
      },
      error: (err: HttpErrorResponse) => this.flash(err.error?.message ?? this.translate.instant('trash.error'), true),
    });
  }

  deletePermanently(): void {
    const course = this.toDelete();
    if (!course) return;
    this.deleting.set(true);
    this.api.deleteCoursePermanently(course.id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.toDelete.set(null);
        this.flash(this.translate.instant('trash.deleted', { name: course.title }));
        this.load();
      },
      error: (err: HttpErrorResponse) => {
        this.deleting.set(false);
        this.toDelete.set(null);
        this.flash(err.error?.message ?? this.translate.instant('trash.error'), true);
      },
    });
  }

  private load(): void {
    this.api.trash().subscribe({
      next: (list) => {
        this.courses.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  private flash(text: string, error = false): void {
    this.message.set({ text, error });
    setTimeout(() => this.message.set(null), 4000);
  }
}
