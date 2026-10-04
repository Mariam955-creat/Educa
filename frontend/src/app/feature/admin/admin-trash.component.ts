import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';

import { AdminApiService } from '../../core/admin/admin-api.service';
import { ReviewRegistryEntry, TrashedUser } from '../../core/admin/admin.models';
import { CourseApiService } from '../../core/courses/course-api.service';
import { TrashedCourse } from '../../core/courses/course.models';
import { LanguageService } from '../../core/i18n/language.service';
import { LocalDatePipe } from '../../core/i18n/local-date.pipe';
import { StarRatingComponent } from '../../shared/star-rating/star-rating.component';
import { ConfirmDeleteDialogComponent } from '../../shared/trash/confirm-delete-dialog.component';

type TrashTab = 'courses' | 'users' | 'reviews';

/** Élément en attente de suppression définitive (boîte « tapez SUPPRIMER »). */
interface PendingDeletion {
  name: string;
  warningKey: string | null;
  run: () => Observable<void>;
}

/** Corbeille de l'administration : cours, comptes et avis supprimés — restaurer ou supprimer définitivement. */
@Component({
  selector: 'app-admin-trash',
  imports: [TranslatePipe, LocalDatePipe, StarRatingComponent, ConfirmDeleteDialogComponent],
  template: `
    <section class="page">
      <div class="head">
        <div>
          <h1>🗑 {{ 'trash.title' | translate }}</h1>
          <p class="subtitle">{{ 'trash.adminHint' | translate }}</p>
        </div>
      </div>

      @if (message(); as m) {
        <p class="flash" [class.ok-flash]="!m.error">{{ m.text }}</p>
      }

      <div class="toolbar">
        <div class="chips">
          @for (t of tabs; track t) {
            <button type="button" [class.active]="tab() === t" (click)="tab.set(t)">
              {{ 'trash.tabs.' + t | translate }} <span class="count">{{ countOf(t) }}</span>
            </button>
          }
        </div>
      </div>

      @if (loading()) {
        <p class="muted">{{ 'common.loading' | translate }}</p>
      } @else if (countOf(tab()) === 0) {
        <p class="muted">{{ 'trash.empty' | translate }}</p>
      } @else {
        <div class="table-wrap">
          <table>
            <tbody>
              @switch (tab()) {
                @case ('courses') {
                  @for (c of courses(); track c.id) {
                    <tr>
                      <td><span class="person-text">{{ c.title }}<small>{{ c.instructorName }}</small></span></td>
                      <td class="muted">{{ 'trash.deletedOn' | translate: { date: (c.deletedAt | localDate: 'short' : lang.current()) } }}</td>
                      <td>
                        @if (c.learnerCount > 0) {
                          <span class="badge warn">🔒 {{ 'trash.hasLearners' | translate: { count: c.learnerCount } }}</span>
                        }
                      </td>
                      <td>
                        <div class="actions">
                          <button type="button" (click)="restoreCourse(c)">↩ {{ 'trash.restore' | translate }}</button>
                          <button type="button" class="danger" [disabled]="c.learnerCount > 0"
                                  (click)="askDelete(c.title, null, courseApi.deleteCoursePermanently(c.id))">
                            {{ 'trash.deleteForever' | translate }}
                          </button>
                        </div>
                      </td>
                    </tr>
                  }
                }
                @case ('users') {
                  @for (u of users(); track u.id) {
                    <tr>
                      <td>
                        <span class="person">
                          <span class="avatar" aria-hidden="true">{{ u.fullName.charAt(0) }}</span>
                          <span class="person-text">{{ u.fullName }}<small>{{ u.email }}</small></span>
                        </span>
                      </td>
                      <td class="muted">{{ 'trash.deletedOn' | translate: { date: (u.deletedAt | localDate: 'short' : lang.current()) } }}</td>
                      <td>
                        @for (r of u.roles; track r) {
                          <span class="badge info">{{ 'admin.roles.' + r | translate }}</span>
                        }
                      </td>
                      <td>
                        <div class="actions">
                          <button type="button" (click)="restoreUser(u)">↩ {{ 'trash.restore' | translate }}</button>
                          <button type="button" class="danger"
                                  (click)="askDelete(u.fullName, 'trash.userWarning', api.deleteUserPermanently(u.id))">
                            {{ 'trash.deleteForever' | translate }}
                          </button>
                        </div>
                      </td>
                    </tr>
                  }
                }
                @case ('reviews') {
                  @for (r of reviews(); track r.id) {
                    <tr>
                      <td><span class="person-text">{{ r.authorName }}<small>{{ r.courseTitle }}</small></span></td>
                      <td class="muted">{{ 'trash.deletedOn' | translate: { date: (r.updatedAt | localDate: 'short' : lang.current()) } }}</td>
                      <td>
                        <app-star-rating [value]="r.stars" size="small" [label]="r.stars + ' / 5'" />
                        @if (r.comment) {
                          <div class="comment">{{ r.comment }}</div>
                        }
                      </td>
                      <td>
                        <div class="actions">
                          <button type="button" (click)="restoreReview(r)">↩ {{ 'trash.restore' | translate }}</button>
                          <button type="button" class="danger"
                                  (click)="askDelete(r.authorName + ' — ' + r.courseTitle, null, api.deleteReviewPermanently(r.id))">
                            {{ 'trash.deleteForever' | translate }}
                          </button>
                        </div>
                      </td>
                    </tr>
                  }
                }
              }
            </tbody>
          </table>
        </div>
      }
    </section>

    <app-confirm-delete-dialog
      [open]="pending() !== null"
      [itemName]="pending()?.name ?? ''"
      [warning]="pending()?.warningKey ? (pending()!.warningKey! | translate) : null"
      [busy]="deleting()"
      (cancel)="pending.set(null)"
      (confirmed)="confirmDelete()"
    />
  `,
  styleUrl: './admin-shared.scss',
  styles: `
    .count { margin-inline-start: 0.2rem; opacity: 0.75; }
    .comment { max-width: 26rem; margin-top: 0.25rem; font-size: 0.82rem; white-space: pre-line; }
    .flash.ok-flash { border-color: #a7f3d0; background: var(--success-soft); color: var(--success-strong); }
  `,
})
export class AdminTrashComponent implements OnInit {
  readonly api = inject(AdminApiService);
  readonly courseApi = inject(CourseApiService);
  private readonly translate = inject(TranslateService);
  readonly lang = inject(LanguageService);

  readonly tabs: TrashTab[] = ['courses', 'users', 'reviews'];
  readonly tab = signal<TrashTab>('courses');
  readonly courses = signal<TrashedCourse[]>([]);
  readonly users = signal<TrashedUser[]>([]);
  readonly reviews = signal<ReviewRegistryEntry[]>([]);
  readonly loading = signal(true);
  readonly pending = signal<PendingDeletion | null>(null);
  readonly deleting = signal(false);
  readonly message = signal<{ text: string; error: boolean } | null>(null);

  ngOnInit(): void {
    this.reload();
  }

  countOf(tab: TrashTab): number {
    return tab === 'courses' ? this.courses().length : tab === 'users' ? this.users().length : this.reviews().length;
  }

  restoreCourse(course: TrashedCourse): void {
    this.handle(this.courseApi.restoreCourse(course.id), 'trash.restoredCourse', course.title);
  }

  restoreUser(user: TrashedUser): void {
    this.handle(this.api.restoreUser(user.id), 'trash.restoredUser', user.fullName);
  }

  restoreReview(review: ReviewRegistryEntry): void {
    this.handle(this.api.restoreReview(review.id), 'trash.restoredReview', review.authorName);
  }

  /** Ouvre la boîte de confirmation ; la requête n'est envoyée qu'après saisie du mot demandé. */
  askDelete(name: string, warningKey: string | null, request: Observable<void>): void {
    this.pending.set({ name, warningKey, run: () => request });
  }

  confirmDelete(): void {
    const pending = this.pending();
    if (!pending) return;
    this.deleting.set(true);
    pending.run().subscribe({
      next: () => {
        this.deleting.set(false);
        this.pending.set(null);
        this.flash(this.translate.instant('trash.deleted', { name: pending.name }));
        this.reload();
      },
      error: (err: HttpErrorResponse) => {
        this.deleting.set(false);
        this.pending.set(null);
        this.flash(err.error?.message ?? this.translate.instant('trash.error'), true);
      },
    });
  }

  private handle(request: Observable<unknown>, successKey: string, name: string): void {
    request.subscribe({
      next: () => {
        this.flash(this.translate.instant(successKey, { name }));
        this.reload();
      },
      error: (err: HttpErrorResponse) => this.flash(err.error?.message ?? this.translate.instant('trash.error'), true),
    });
  }

  private reload(): void {
    let remaining = 3;
    const done = () => --remaining === 0 && this.loading.set(false);
    this.api.courseTrash().subscribe({ next: (l) => this.courses.set(l), complete: done, error: done });
    this.api.userTrash().subscribe({ next: (l) => this.users.set(l), complete: done, error: done });
    this.api.reviewTrash().subscribe({ next: (l) => this.reviews.set(l), complete: done, error: done });
  }

  private flash(text: string, error = false): void {
    this.message.set({ text, error });
    setTimeout(() => this.message.set(null), 4000);
  }
}
