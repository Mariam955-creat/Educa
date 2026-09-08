import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api';
import { CourseProgress, Enrollment } from '../courses/course.models';

@Injectable({ providedIn: 'root' })
export class EnrollmentApiService {
  private readonly http = inject(HttpClient);
  private readonly base = API_BASE_URL;

  enroll(courseId: number): Observable<Enrollment> {
    return this.http.post<Enrollment>(`${this.base}/courses/${courseId}/enroll`, {});
  }

  myEnrollments(): Observable<Enrollment[]> {
    return this.http.get<Enrollment[]>(`${this.base}/enrollments/me`);
  }

  completeContent(contentId: number): Observable<CourseProgress> {
    return this.http.post<CourseProgress>(`${this.base}/contents/${contentId}/complete`, {});
  }

  courseProgress(courseId: number): Observable<CourseProgress> {
    return this.http.get<CourseProgress>(`${this.base}/courses/${courseId}/progress`);
  }
}
