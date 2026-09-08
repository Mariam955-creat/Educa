import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';

import { AppComponent } from './app.component';
import { routes } from './app.routes';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideRouter(routes), provideHttpClient()],
    }).compileComponents();
  });

  it('se crée', () => {
    const fixture = TestBed.createComponent(AppComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('affiche la marque educa', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const brand = fixture.nativeElement.querySelector('.brand');
    expect(brand?.textContent).toContain('educa');
  });
});
