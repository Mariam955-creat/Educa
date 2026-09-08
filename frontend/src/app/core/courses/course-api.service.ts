import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../api';
import {
  ChapterFormValue,
  ChapterItem,
  ContentFormValue,
  ContentItem,
  CourseDetail,
  CourseFormValue,
  CourseSummary,
  Page,
} from './course.models';

@Injectable({ providedIn: 'root' })
export class CourseApiService {
  private readonly http = inject(HttpClient);
  private readonly base = API_BASE_URL;

  catalog(q?: string, language?: string, page = 0, size = 20): Observable<Page<CourseSummary>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q) params = params.set('q', q);
    if (language) params = params.set('language', language);
    return this.http.get<Page<CourseSummary>>(`${this.base}/courses`, { params });
  }

  detail(slug: string): Observable<CourseDetail> {
    return this.http.get<CourseDetail>(`${this.base}/courses/${slug}`);
  }

  myCourses(): Observable<CourseSummary[]> {
    return this.http.get<CourseSummary[]>(`${this.base}/instructor/courses`);
  }

  createCourse(body: CourseFormValue): Observable<CourseSummary> {
    return this.http.post<CourseSummary>(`${this.base}/courses`, body);
  }

  updateCourse(id: number, body: CourseFormValue): Observable<CourseSummary> {
    return this.http.put<CourseSummary>(`${this.base}/courses/${id}`, body);
  }

  deleteCourse(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/courses/${id}`);
  }

  setPublished(id: number, published: boolean): Observable<CourseSummary> {
    return this.http.post<CourseSummary>(
      `${this.base}/courses/${id}/${published ? 'publish' : 'unpublish'}`,
      {},
    );
  }

  addChapter(courseId: number, body: ChapterFormValue): Observable<ChapterItem> {
    return this.http.post<ChapterItem>(`${this.base}/courses/${courseId}/chapters`, body);
  }

  deleteChapter(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/chapters/${id}`);
  }

  addContent(chapterId: number, body: ContentFormValue): Observable<ContentItem> {
    return this.http.post<ContentItem>(`${this.base}/chapters/${chapterId}/contents`, body);
  }

  deleteContent(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/contents/${id}`);
  }

  uploadFile(contentId: number, file: File): Observable<ContentItem> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ContentItem>(`${this.base}/contents/${contentId}/file`, form);
  }

  downloadFile(contentId: number): Observable<Blob> {
    return this.http.get(`${this.base}/contents/${contentId}/file`, { responseType: 'blob' });
  }
}
