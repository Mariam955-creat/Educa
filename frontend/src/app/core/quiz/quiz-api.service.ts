import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api';
import {
  AnswerInput,
  AttemptResult,
  AttemptSummary,
  CourseGrade,
  CourseQuizzes,
  LearnerResult,
  QuestionInput,
  QuizView,
} from './quiz.models';

@Injectable({ providedIn: 'root' })
export class QuizApiService {
  private readonly http = inject(HttpClient);
  private readonly base = API_BASE_URL;

  courseQuizzes(courseId: number): Observable<CourseQuizzes> {
    return this.http.get<CourseQuizzes>(`${this.base}/courses/${courseId}/quizzes`);
  }

  view(quizId: number): Observable<QuizView> {
    return this.http.get<QuizView>(`${this.base}/quizzes/${quizId}`);
  }

  submit(quizId: number, answers: AnswerInput[]): Observable<AttemptResult> {
    return this.http.post<AttemptResult>(`${this.base}/quizzes/${quizId}/attempts`, { answers });
  }

  myAttempts(quizId: number): Observable<AttemptSummary[]> {
    return this.http.get<AttemptSummary[]>(`${this.base}/quizzes/${quizId}/attempts/me`);
  }

  courseGrade(courseId: number): Observable<CourseGrade> {
    return this.http.get<CourseGrade>(`${this.base}/courses/${courseId}/grade`);
  }

  // ----- formateur -----

  createControl(chapterId: number, title: string): Observable<QuizView> {
    return this.http.post<QuizView>(`${this.base}/chapters/${chapterId}/control-quiz`, { title });
  }

  createFinalExam(courseId: number, title: string, maxAttempts = 3): Observable<QuizView> {
    return this.http.post<QuizView>(`${this.base}/courses/${courseId}/final-exam`, { title, maxAttempts });
  }

  addQuestion(quizId: number, question: QuestionInput): Observable<QuizView> {
    return this.http.post<QuizView>(`${this.base}/quizzes/${quizId}/questions`, question);
  }

  deleteQuestion(questionId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/questions/${questionId}`);
  }

  deleteQuiz(quizId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/quizzes/${quizId}`);
  }

  courseResults(courseId: number): Observable<LearnerResult[]> {
    return this.http.get<LearnerResult[]>(`${this.base}/instructor/courses/${courseId}/results`);
  }
}
