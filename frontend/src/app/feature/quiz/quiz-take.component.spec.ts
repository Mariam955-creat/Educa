import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

import { QuizTakeComponent } from './quiz-take.component';
import { QuizApiService } from '../../core/quiz/quiz-api.service';
import { AttemptResult, QuizView } from '../../core/quiz/quiz.models';

const QUIZ: QuizView = {
  id: 42,
  type: 'CONTROL',
  title: 'Contrôle chapitre 1',
  passThreshold: 50,
  maxAttempts: null,
  answersVisible: false,
  questions: [
    {
      id: 1,
      statement: 'Q simple',
      type: 'SINGLE_CHOICE',
      points: 1,
      position: 1,
      options: [
        { id: 11, label: 'A', position: 1, correct: null },
        { id: 12, label: 'B', position: 2, correct: null },
      ],
    },
    {
      id: 2,
      statement: 'Q multiple',
      type: 'MULTIPLE_CHOICE',
      points: 2,
      position: 2,
      options: [
        { id: 21, label: 'X', position: 1, correct: null },
        { id: 22, label: 'Y', position: 2, correct: null },
      ],
    },
  ],
};

describe('QuizTakeComponent', () => {
  let apiStub: { view: jasmine.Spy; submit: jasmine.Spy };
  const routerStub = { navigate: jasmine.createSpy('navigate') };

  beforeEach(async () => {
    apiStub = {
      view: jasmine.createSpy('view').and.returnValue(of(QUIZ)),
      submit: jasmine.createSpy('submit').and.returnValue(
        of({ attemptId: 1, score: 100, passed: true, correctCount: 2, total: 2 } as AttemptResult),
      ),
    };
    routerStub.navigate.calls.reset();

    await TestBed.configureTestingModule({
      imports: [QuizTakeComponent],
      providers: [
        { provide: QuizApiService, useValue: apiStub },
        { provide: Router, useValue: routerStub },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map([['id', '42']]) } } },
      ],
    }).compileComponents();
  });

  // Pas de detectChanges() : on ne rend pas le template (RouterLink),
  // on déclenche ngOnInit manuellement pour tester la logique.
  function create() {
    const cmp = TestBed.createComponent(QuizTakeComponent).componentInstance;
    cmp.ngOnInit();
    return cmp;
  }

  it('charge le quiz depuis l’id de route', () => {
    const cmp = create();
    expect(apiStub.view).toHaveBeenCalledWith(42);
    expect(cmp.quiz()?.title).toBe('Contrôle chapitre 1');
    expect(cmp.loading()).toBe(false);
  });

  it('choix unique : une seule option cochée à la fois', () => {
    const cmp = create();
    const q = QUIZ.questions[0];
    cmp.toggle(q, 11);
    expect(cmp.isChecked(q, 11)).toBe(true);
    cmp.toggle(q, 12);
    expect(cmp.isChecked(q, 11)).toBe(false);
    expect(cmp.isChecked(q, 12)).toBe(true);
  });

  it('choix multiple : les options s’accumulent', () => {
    const cmp = create();
    const q = QUIZ.questions[1];
    cmp.toggle(q, 21);
    cmp.toggle(q, 22);
    expect(cmp.isChecked(q, 21)).toBe(true);
    expect(cmp.isChecked(q, 22)).toBe(true);
    cmp.toggle(q, 21);
    expect(cmp.isChecked(q, 21)).toBe(false);
  });

  it('envoie les réponses sélectionnées et affiche le résultat', () => {
    const cmp = create();
    cmp.toggle(QUIZ.questions[0], 12);
    cmp.toggle(QUIZ.questions[1], 21);
    cmp.toggle(QUIZ.questions[1], 22);

    cmp.submit();

    expect(apiStub.submit).toHaveBeenCalledWith(42, [
      { questionId: 1, selectedOptionIds: [12] },
      { questionId: 2, selectedOptionIds: [21, 22] },
    ]);
    expect(cmp.result()?.passed).toBe(true);
    expect(cmp.submitting()).toBe(false);
  });
});
