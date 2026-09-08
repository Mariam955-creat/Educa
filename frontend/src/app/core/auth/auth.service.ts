import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { API_BASE_URL } from '../api';
import { RegisterRequest, RoleName, TokenResponse, User } from './auth.models';

const ACCESS_KEY = 'educa.accessToken';
const REFRESH_KEY = 'educa.refreshToken';
const USER_KEY = 'educa.user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _user = signal<User | null>(readJson<User>(USER_KEY));
  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._user() !== null && this.accessToken !== null);

  get accessToken(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  }

  get refreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  }

  register(body: RegisterRequest): Observable<User> {
    return this.http.post<User>(`${API_BASE_URL}/auth/register`, body);
  }

  login(email: string, password: string): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>(`${API_BASE_URL}/auth/login`, { email, password })
      .pipe(tap((res) => this.store(res)));
  }

  refresh(): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>(`${API_BASE_URL}/auth/refresh`, { refreshToken: this.refreshToken })
      .pipe(tap((res) => this.store(res)));
  }

  logout(): void {
    const token = this.refreshToken;
    if (token) {
      this.http.post(`${API_BASE_URL}/auth/logout`, { refreshToken: token }).subscribe({ error: () => undefined });
    }
    this.clear();
    void this.router.navigate(['/login']);
  }

  hasRole(role: RoleName): boolean {
    return this._user()?.roles.includes(role) ?? false;
  }

  hasAnyRole(roles: RoleName[]): boolean {
    return roles.some((r) => this.hasRole(r));
  }

  /** Route d'accueil selon le rôle principal. */
  homePathForRole(): string {
    if (this.hasRole('ADMIN')) return '/admin';
    if (this.hasRole('INSTRUCTOR')) return '/instructor';
    return '/dashboard';
  }

  private store(res: TokenResponse): void {
    localStorage.setItem(ACCESS_KEY, res.accessToken);
    localStorage.setItem(REFRESH_KEY, res.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(res.user));
    this._user.set(res.user);
  }

  private clear(): void {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
    this._user.set(null);
  }
}

function readJson<T>(key: string): T | null {
  const raw = localStorage.getItem(key);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}
