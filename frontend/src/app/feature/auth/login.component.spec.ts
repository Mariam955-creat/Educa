import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { TranslateService, provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';

import { LoginComponent } from './login.component';
import { AuthService } from '../../core/auth/auth.service';

describe('LoginComponent', () => {
  let authStub: { login: jasmine.Spy; homePathForRole: jasmine.Spy };
  let navigateByUrl: jasmine.Spy;

  beforeEach(async () => {
    authStub = {
      login: jasmine.createSpy('login').and.returnValue(of({})),
      homePathForRole: jasmine.createSpy('homePathForRole').and.returnValue('/dashboard'),
    };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        provideTranslateService({}),
        { provide: AuthService, useValue: authStub },
      ],
    }).compileComponents();

    navigateByUrl = spyOn(TestBed.inject(Router), 'navigateByUrl').and.resolveTo(true);
  });

  function create() {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  it('n’appelle pas l’API si le formulaire est invalide', () => {
    const cmp = create();
    cmp.submit();
    expect(authStub.login).not.toHaveBeenCalled();
    expect(cmp.form.controls.email.touched).toBe(true);
  });

  it('connecte puis redirige vers la route d’accueil du rôle', () => {
    const cmp = create();
    cmp.form.setValue({ email: 'a@educa.dev', password: 'password123' });

    cmp.submit();

    expect(authStub.login).toHaveBeenCalledWith('a@educa.dev', 'password123');
    expect(navigateByUrl).toHaveBeenCalledWith('/dashboard');
    expect(cmp.error()).toBeNull();
  });

  it('affiche l’erreur d’identifiants sur un 401', () => {
    authStub.login.and.returnValue(throwError(() => ({ status: 401 })));
    spyOn(TestBed.inject(TranslateService), 'instant').and.callFake((k: string | string[]) => `T:${k}`);

    const cmp = create();
    cmp.form.setValue({ email: 'a@educa.dev', password: 'bad' });
    cmp.submit();

    expect(cmp.error()).toBe('T:auth.login.errorCredentials');
    expect(cmp.loading()).toBe(false);
    expect(navigateByUrl).not.toHaveBeenCalled();
  });
});
