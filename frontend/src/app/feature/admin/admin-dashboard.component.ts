import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { AdminApiService } from '../../core/admin/admin-api.service';
import { AdminUser, CertificateRegistryEntry } from '../../core/admin/admin.models';
import { AuthService } from '../../core/auth/auth.service';
import { RoleName } from '../../core/auth/auth.models';
import { CourseLanguage } from '../../core/language/language-api.service';

type Tab = 'users' | 'certificates' | 'languages';

@Component({
  selector: 'app-admin-dashboard',
  imports: [DatePipe, FormsModule, TranslatePipe],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly translate = inject(TranslateService);
  readonly auth = inject(AuthService);

  readonly roleOptions: RoleName[] = ['LEARNER', 'INSTRUCTOR', 'ADMIN'];

  readonly tab = signal<Tab>('users');
  readonly currentUserId = computed(() => this.auth.user()?.id ?? null);
  readonly message = signal<string | null>(null);

  readonly q = signal('');
  readonly users = signal<AdminUser[]>([]);
  readonly usersLoading = signal(true);

  readonly certificates = signal<CertificateRegistryEntry[]>([]);
  readonly certificatesLoading = signal(true);
  private certificatesLoaded = false;

  readonly languages = signal<CourseLanguage[]>([]);
  readonly languagesLoading = signal(true);
  private languagesLoaded = false;

  ngOnInit(): void {
    this.loadUsers();
  }

  selectTab(tab: Tab): void {
    this.tab.set(tab);
    if (tab === 'certificates' && !this.certificatesLoaded) {
      this.loadCertificates();
    }
    if (tab === 'languages' && !this.languagesLoaded) {
      this.loadLanguages();
    }
  }

  search(): void {
    this.loadUsers();
  }

  primaryRole(user: AdminUser): RoleName {
    if (user.roles.includes('ADMIN')) return 'ADMIN';
    if (user.roles.includes('INSTRUCTOR')) return 'INSTRUCTOR';
    return 'LEARNER';
  }

  changeRole(user: AdminUser, role: string): void {
    this.api.updateRoles(user.id, [role as RoleName]).subscribe({
      next: (updated) => this.replaceUser(updated),
      error: (err: HttpErrorResponse) => {
        this.flash(err.error?.message ?? this.translate.instant('admin.users.updateError'));
        this.loadUsers();
      },
    });
  }

  toggleEnabled(user: AdminUser): void {
    const nextEnabled = !user.enabled;
    const key = nextEnabled ? 'admin.users.confirmEnable' : 'admin.users.confirmDisable';
    if (!confirm(this.translate.instant(key, { name: user.fullName }))) return;

    this.api.updateStatus(user.id, nextEnabled).subscribe({
      next: (updated) => this.replaceUser(updated),
      error: (err: HttpErrorResponse) => {
        this.flash(err.error?.message ?? this.translate.instant('admin.users.updateError'));
        this.loadUsers();
      },
    });
  }

  toggleLanguage(lang: CourseLanguage): void {
    this.api.setLanguageActive(lang.code, !lang.active).subscribe({
      next: (updated) => {
        this.languages.update((list) => list.map((l) => (l.code === updated.code ? updated : l)));
      },
      error: (err: HttpErrorResponse) => {
        this.flash(err.error?.message ?? this.translate.instant('admin.users.updateError'));
      },
    });
  }

  private replaceUser(updated: AdminUser): void {
    this.users.update((list) => list.map((u) => (u.id === updated.id ? updated : u)));
  }

  private loadUsers(): void {
    this.usersLoading.set(true);
    this.api.users(this.q().trim() || undefined).subscribe({
      next: (page) => {
        this.users.set(page.content);
        this.usersLoading.set(false);
      },
      error: () => this.usersLoading.set(false),
    });
  }

  private loadCertificates(): void {
    this.certificatesLoading.set(true);
    this.api.certificateRegistry().subscribe({
      next: (page) => {
        this.certificates.set(page.content);
        this.certificatesLoading.set(false);
        this.certificatesLoaded = true;
      },
      error: () => this.certificatesLoading.set(false),
    });
  }

  private loadLanguages(): void {
    this.languagesLoading.set(true);
    this.api.languages().subscribe({
      next: (list) => {
        this.languages.set(list);
        this.languagesLoading.set(false);
        this.languagesLoaded = true;
      },
      error: () => this.languagesLoading.set(false),
    });
  }

  private flash(text: string): void {
    this.message.set(text);
    setTimeout(() => this.message.set(null), 3500);
  }
}
