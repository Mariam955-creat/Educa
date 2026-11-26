import { Component, ElementRef, HostListener, inject, signal } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, filter, of } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { LanguageService } from '../i18n/language.service';
import { LocalDatePipe } from '../i18n/local-date.pipe';
import { AppNotification, NotificationApiService } from '../notifications/notification-api.service';

/**
 * Cloche de l'en-tête : pastille du nombre de notifications non lues et liste déroulante.
 * Rechargée à chaque navigation ; un clic marque la notification lue et ouvre son lien.
 */
@Component({
  selector: 'app-notification-bell',
  imports: [TranslatePipe, LocalDatePipe],
  template: `
    <button type="button" class="bell" [attr.aria-expanded]="open()" [attr.aria-label]="'notifications.title' | translate"
            (click)="toggle()">
      🔔
      @if (unread() > 0) {
        <span class="dot">{{ unread() > 9 ? '9+' : unread() }}</span>
      }
    </button>

    @if (open()) {
      <div class="panel" role="menu">
        <div class="panel-head">
          <strong>{{ 'notifications.title' | translate }}</strong>
          @if (unread() > 0) {
            <button type="button" class="link" (click)="markAllRead()">{{ 'notifications.markAllRead' | translate }}</button>
          }
        </div>
        @for (n of items(); track n.id) {
          <button type="button" class="item" [class.unread]="!n.read" role="menuitem" (click)="openItem(n)">
            <span class="text">{{ 'notifications.types.' + n.type | translate: { subject: n.subject, detail: n.detail } }}</span>
            @if (n.detail && n.type.endsWith('_REJECTED')) {
              <span class="detail">{{ 'notifications.detail' | translate }} : {{ n.detail }}</span>
            }
            <span class="date">{{ n.createdAt | localDate: 'long' : lang.current() }}</span>
          </button>
        } @empty {
          <p class="empty">{{ 'notifications.empty' | translate }}</p>
        }
      </div>
    }
  `,
  styles: `
    :host { position: relative; }
    .bell { position: relative; padding: 0.35rem 0.5rem; border: 1px solid var(--border, #dbe3ef); border-radius: 8px; background: var(--surface, #fff); font-size: 1.05rem; line-height: 1; cursor: pointer; }
    .bell:hover { border-color: var(--accent, #2563eb); }
    .dot { position: absolute; top: -0.45rem; right: -0.45rem; min-width: 1.15rem; padding: 0.05rem 0.3rem; border-radius: 999px; background: var(--danger, #b91c1c); color: #fff; font-size: 0.68rem; font-weight: 700; text-align: center; }
    .panel { position: absolute; top: calc(100% + 0.5rem); right: 0; z-index: 50; width: min(360px, calc(100vw - 2rem)); max-height: 70vh; overflow-y: auto; border: 1px solid var(--border, #dbe3ef); border-radius: 12px; background: var(--surface, #fff); box-shadow: 0 16px 40px rgba(17, 24, 39, 0.18); }
    .panel-head { display: flex; justify-content: space-between; align-items: center; padding: 0.75rem 1rem; border-bottom: 1px solid var(--border, #dbe3ef); }
    .link { border: 0; background: transparent; color: var(--accent, #2563eb); font: inherit; font-size: 0.8rem; cursor: pointer; }
    .item { display: flex; flex-direction: column; gap: 0.25rem; width: 100%; padding: 0.75rem 1rem; border: 0; border-bottom: 1px solid var(--border, #dbe3ef); background: transparent; color: inherit; font: inherit; text-align: start; cursor: pointer; }
    .item:hover { background: var(--bg, #eff6ff); }
    .item.unread { background: var(--accent-soft, #eff6ff); }
    .item.unread .text { font-weight: 700; }
    .text { font-size: 0.88rem; }
    .detail { font-size: 0.82rem; color: var(--danger, #b91c1c); white-space: pre-line; }
    .date { font-size: 0.75rem; color: var(--muted, #6b7280); }
    .empty { margin: 0; padding: 1rem; color: var(--muted, #6b7280); font-size: 0.88rem; }
  `,
})
export class NotificationBellComponent {
  private readonly api = inject(NotificationApiService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly host = inject(ElementRef<HTMLElement>);
  readonly lang = inject(LanguageService);

  readonly open = signal(false);
  readonly items = signal<AppNotification[]>([]);
  readonly unread = signal(0);

  constructor() {
    this.load();
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe(() => {
      this.open.set(false);
      this.load();
    });
  }

  toggle(): void {
    this.open.update((v) => !v);
    if (this.open()) this.load();
  }

  markAllRead(): void {
    this.api.markAllRead().subscribe(() => this.load());
  }

  /**
   * Marque lue et ouvre le lien. Une demande de formateur acceptée change les rôles : la session est d'abord
   * rafraîchie pour que l'espace formateur soit accessible sans se reconnecter.
   */
  openItem(n: AppNotification): void {
    const read$: Observable<void> = n.read ? of(undefined) : this.api.markRead(n.id);
    read$.subscribe(() => {
      this.open.set(false);
      const go = () => (n.link ? this.router.navigateByUrl(n.link) : this.load());
      if (n.type === 'INSTRUCTOR_REQUEST_APPROVED') {
        this.auth.refresh().subscribe({ next: go, error: go });
      } else {
        go();
      }
    });
  }

  private load(): void {
    if (!this.auth.isAuthenticated()) return;
    this.api.list().subscribe({
      next: (res) => {
        this.items.set(res.items);
        this.unread.set(res.unreadCount);
      },
      error: () => undefined,
    });
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.open() && !this.host.nativeElement.contains(event.target as Node)) {
      this.open.set(false);
    }
  }
}
