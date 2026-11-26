import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { RegisterComponent } from './register.component';

describe('RegisterComponent — inscription formateur en deux étapes', () => {
  let fixture: ComponentFixture<RegisterComponent>;
  let authStub: { register: jasmine.Spy; login: jasmine.Spy; homePathForRole: jasmine.Spy };

  beforeEach(async () => {
    authStub = {
      register: jasmine.createSpy('register').and.returnValue(of({})),
      login: jasmine.createSpy('login').and.returnValue(of({})),
      homePathForRole: jasmine.createSpy('homePathForRole').and.returnValue('/account'),
    };
    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        provideRouter([]),
        provideTranslateService({}),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authStub },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(RegisterComponent);
    fixture.detectChanges();
  });

  /** Remplit l'étape 1 comme un utilisateur, coche (ou non) « Je veux enseigner », puis clique sur le bouton. */
  function fillAndClick(teach: boolean): HTMLElement {
    const el: HTMLElement = fixture.nativeElement;
    const type = (selector: string, value: string) => {
      const input = el.querySelector<HTMLInputElement>(selector)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    };
    type('input[formControlName="fullName"]', 'Lola Prof');
    type('input[formControlName="email"]', 'lola@example.org');
    const passwords = el.querySelectorAll<HTMLInputElement>('app-password-field input');
    for (const input of Array.from(passwords)) {
      input.value = 'motdepasse1';
      input.dispatchEvent(new Event('input'));
    }
    if (teach) {
      el.querySelector<HTMLInputElement>('input[formControlName="wantsToTeach"]')!.click();
    }
    fixture.detectChanges();
    el.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();
    return el;
  }

  it('case cochée : « Suivant » ouvre la candidature sans créer le compte', () => {
    const el = fillAndClick(true);
    const application = el.querySelector('app-instructor-application')!;
    expect(application.closest<HTMLElement>('.step')!.hidden).toBeFalse();
    expect(getComputedStyle(application).display).not.toBe('none');
    expect(el.querySelectorAll('app-instructor-application input[type="file"]').length).toBe(3);
    expect(authStub.register).not.toHaveBeenCalled();
  });

  it('case non cochée : le compte est créé directement', () => {
    fillAndClick(false);
    expect(authStub.register).toHaveBeenCalled();
  });
});
