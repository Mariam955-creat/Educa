import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';

import { AdminApiService } from '../../core/admin/admin-api.service';
import { AdminUser } from '../../core/admin/admin.models';
import { RoleName } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { LanguageService } from '../../core/i18n/language.service';
import { LocalDatePipe } from '../../core/i18n/local-date.pipe';
import { PagerComponent } from '../../shared/pager/pager.component';

const PAGE_SIZE = 20;

/** Utilisateurs : recherche nom/email, rôle principal, activation, date d'inscription. */
@Component({
  selector: 'app-admin-users',
  imports: [TranslatePipe, LocalDatePipe, PagerComponent],
  template: `
    <section class="page">
      <div class="head">
        <div>
          <h1>{{ 'admin.nav.users' | translate }}</h1>
          <p class="subtitle">{{ 'admin.usersPage.subtitle' | translate: { count: total() } }}</p>
        </div>
      </div>

      @if (message()) {
        <p class="flash">{{ message() }}</p>
      }

      <div class="toolbar">
        <input class="search" type="search" [value]="query()" (input)="onSearch($any($event.target).value)"
               [placeholder]="'admin.users.searchPlaceholder' | translate" />
      </div>

      @if (loading()) {
        <p class="muted">{{ 'common.loading' | translate }}</p>
      } @else if (users().length === 0) {
        <p class="muted">{{ 'admin.users.empty' | translate }}</p>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'admin.users.headerName' | translate }}</th>
                <th>{{ 'admin.users.headerRole' | translate }}</th>
                <th>{{ 'admin.usersPage.joined' | translate }}</th>
                <th>{{ 'admin.users.headerStatus' | translate }}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (user of users(); track user.id) {
                <tr>
                  <td>
                    <span class="person">
                      <span class="avatar" aria-hidden="true">{{ user.fullName.charAt(0) }}</span>
                      <span class="person-text">
                        {{ user.fullName }}
                        @if (user.id === currentUserId()) {
                          <span class="badge info">{{ 'admin.usersPage.you' | translate }}</span>
                        }
                        <small>{{ user.email }}</small>
                      </span>
                    </span>
                  </td>
                  <td>
                    <select [value]="primaryRole(user)" [disabled]="user.id === currentUserId()"
                            (change)="changeRole(user, $any($event.target).value)">
                      @for (role of roleOptions; track role) {
                        <option [value]="role">{{ 'admin.roles.' + role | translate }}</option>
                      }
                    </select>
                  </td>
                  <td class="muted">{{ user.createdAt | localDate: 'long' : lang.current() }}</td>
                  <td>
                    <span class="badge" [class.ok]="user.enabled" [class.ko]="!user.enabled">
                      {{ (user.enabled ? 'admin.users.enabled' : 'admin.users.disabled') | translate }}
                    </span>
                  </td>
                  <td>
                    <div class="actions">
                      <button type="button" [class.danger]="user.enabled" [disabled]="user.id === currentUserId()" (click)="toggleEnabled(user)">
                        {{ (user.enabled ? 'admin.users.disable' : 'admin.users.enable') | translate }}
                      </button>
                      <button type="button" class="danger" [disabled]="user.id === currentUserId()" (click)="moveToTrash(user)">
                        🗑 {{ 'trash.moveToTrash' | translate }}
                      </button>
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <app-pager [page]="page()" [totalPages]="totalPages()" (pageChange)="load($event)" />
      }
    </section>
  `,
  styleUrl: './admin-shared.scss',
})
export class AdminUsersComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly translate = inject(TranslateService);
  private readonly auth = inject(AuthService);
  readonly lang = inject(LanguageService);

  readonly roleOptions: RoleName[] = ['LEARNER', 'INSTRUCTOR', 'ADMIN'];
  readonly currentUserId = computed(() => this.auth.user()?.id ?? null);

  readonly users = signal<AdminUser[]>([]);
  readonly loading = signal(true);
  readonly query = signal('');
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly total = signal(0);
  readonly message = signal<string | null>(null);

  private readonly search$ = new Subject<string>();

  ngOnInit(): void {
    this.search$.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => this.load(0));
    this.load(0);
  }

  onSearch(value: string): void {
    this.query.set(value);
    this.search$.next(value);
  }

  load(page: number): void {
    this.loading.set(true);
    this.api.users(this.query().trim() || undefined, page, PAGE_SIZE).subscribe({
      next: (res) => {
        this.users.set(res.content);
        this.page.set(res.page);
        this.totalPages.set(res.totalPages);
        this.total.set(res.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  primaryRole(user: AdminUser): RoleName {
    if (user.roles.includes('ADMIN')) return 'ADMIN';
    if (user.roles.includes('INSTRUCTOR')) return 'INSTRUCTOR';
    return 'LEARNER';
  }

  changeRole(user: AdminUser, role: string): void {
    this.api.updateRoles(user.id, [role as RoleName]).subscribe({
      next: (updated) => this.replace(updated),
      error: (err: HttpErrorResponse) => this.fail(err),
    });
  }

  toggleEnabled(user: AdminUser): void {
    const enable = !user.enabled;
    const key = enable ? 'admin.users.confirmEnable' : 'admin.users.confirmDisable';
    if (!confirm(this.translate.instant(key, { name: user.fullName }))) return;
    this.api.updateStatus(user.id, enable).subscribe({
      next: (updated) => this.replace(updated),
      error: (err: HttpErrorResponse) => this.fail(err),
    });
  }

  /** Met le compte à la corbeille : connexion bloquée, restaurable depuis « Corbeille ». */
  moveToTrash(user: AdminUser): void {
    if (!confirm(this.translate.instant('trash.confirmMoveUser', { name: user.fullName }))) return;
    this.api.deleteUser(user.id).subscribe({
      next: () => {
        this.message.set(this.translate.instant('trash.movedUser', { name: user.fullName }));
        setTimeout(() => this.message.set(null), 4000);
        this.load(this.page());
      },
      error: (err: HttpErrorResponse) => this.fail(err),
    });
  }

  private replace(updated: AdminUser): void {
    this.users.update((list) => list.map((u) => (u.id === updated.id ? updated : u)));
  }

  private fail(err: HttpErrorResponse): void {
    this.message.set(err.error?.message ?? this.translate.instant('admin.users.updateError'));
    setTimeout(() => this.message.set(null), 3500);
    this.load(this.page());
  }
}
