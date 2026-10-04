import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateService, provideTranslateService } from '@ngx-translate/core';

import { ConfirmDeleteDialogComponent } from './confirm-delete-dialog.component';

describe('ConfirmDeleteDialogComponent', () => {
  let fixture: ComponentFixture<ConfirmDeleteDialogComponent>;
  let component: ConfirmDeleteDialogComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfirmDeleteDialogComponent],
      providers: [provideTranslateService({})],
    }).compileComponents();
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('fr', { trash: { confirmWord: 'SUPPRIMER' } });
    translate.use('fr');

    fixture = TestBed.createComponent(ConfirmDeleteDialogComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('open', true);
    fixture.componentRef.setInput('itemName', 'Cours de test');
    fixture.detectChanges();
  });

  function deleteButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('button.danger');
  }

  it('garde le bouton désactivé tant que le mot n\'est pas tapé', () => {
    expect(deleteButton().disabled).toBeTrue();
    component.typed.set('SUPPRIME');
    fixture.detectChanges();
    expect(deleteButton().disabled).toBeTrue();
  });

  it('active le bouton avec le bon mot (casse indifférente) et émet la confirmation', () => {
    const confirmed = jasmine.createSpy('confirmed');
    component.confirmed.subscribe(confirmed);
    component.typed.set('supprimer');
    fixture.detectChanges();
    expect(deleteButton().disabled).toBeFalse();
    deleteButton().click();
    expect(confirmed).toHaveBeenCalledTimes(1);
  });
});
