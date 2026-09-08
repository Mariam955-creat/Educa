export type ContentType = 'VIDEO' | 'DOCUMENT' | 'TEXT';

export interface CourseSummary {
  id: number;
  slug: string;
  title: string;
  description?: string;
  language: string;
  published: boolean;
  instructorName: string;
  chapterCount: number;
}

export interface ContentItem {
  id: number;
  type: ContentType;
  title: string;
  position: number;
  textBody?: string;
  fileName?: string;
  mimeType?: string;
  hasFile: boolean;
}

export interface ChapterItem {
  id: number;
  title: string;
  position: number;
  contents: ContentItem[];
}

export interface CourseDetail {
  id: number;
  slug: string;
  title: string;
  description?: string;
  language: string;
  published: boolean;
  instructorName: string;
  controlWeight: number;
  examWeight: number;
  passThreshold: number;
  contentsVisible: boolean;
  chapters: ChapterItem[];
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Enrollment {
  courseId: number;
  courseSlug: string;
  courseTitle: string;
  status: string;
  totalContents: number;
  completedContents: number;
  progressPercent: number;
  enrolledAt: string;
}

export interface CourseProgress {
  courseId: number;
  totalContents: number;
  completedContents: number;
  progressPercent: number;
  completedContentIds: number[];
}

export interface CourseFormValue {
  title: string;
  description?: string;
  language: string;
}

export interface ChapterFormValue {
  title: string;
  position: number;
}

export interface ContentFormValue {
  type: ContentType;
  title: string;
  position: number;
  textBody?: string;
}
