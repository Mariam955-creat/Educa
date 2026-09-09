import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

import { AuthService } from './auth.service';
import { API_BASE_URL } from '../api';
import { TokenResponse } from './auth.models';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;
  const routerStub = { navigate: jasmine.createSpy('navigate') };

  const tokenResponse = (roles: string[]): TokenResponse => ({
    accessToken: 'access-abc',
    refreshToken: 'refresh-xyz',
    user: { id: 1, email: 'a@educa.dev', fullName: 'Alice', roles, preferredLanguage: 'fr' },
  }) as TokenResponse;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerStub },
      ],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('démarre non authentifié', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.user()).toBeNull();
  });

  it('login stocke les jetons + l’utilisateur et passe authentifié', () => {
    let emitted: TokenResponse | undefined;
    service.login('a@educa.dev', 'password123').subscribe((r) => (emitted = r));

    const req = http.expectOne(`${API_BASE_URL}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'a@educa.dev', password: 'password123' });
    req.flush(tokenResponse(['LEARNER']));

    expect(emitted?.accessToken).toBe('access-abc');
    expect(service.isAuthenticated()).toBe(true);
    expect(service.accessToken).toBe('access-abc');
    expect(localStorage.getItem('educa.refreshToken')).toBe('refresh-xyz');
    expect(service.user()?.fullName).toBe('Alice');
  });

  it('homePathForRole dépend du rôle', () => {
    service.login('a@educa.dev', 'x').subscribe();
    http.expectOne(`${API_BASE_URL}/auth/login`).flush(tokenResponse(['ADMIN']));
    expect(service.homePathForRole()).toBe('/admin');
    expect(service.hasAnyRole(['INSTRUCTOR', 'ADMIN'])).toBe(true);
  });

  it('logout appelle l’API, purge le stockage et redirige vers /login', () => {
    service.login('a@educa.dev', 'x').subscribe();
    http.expectOne(`${API_BASE_URL}/auth/login`).flush(tokenResponse(['LEARNER']));

    service.logout();
    http.expectOne(`${API_BASE_URL}/auth/logout`).flush({});

    expect(service.isAuthenticated()).toBe(false);
    expect(localStorage.getItem('educa.accessToken')).toBeNull();
    expect(routerStub.navigate).toHaveBeenCalledWith(['/login']);
  });
});
