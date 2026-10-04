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
  price: number;
  /** URL de l'image de couverture (chemin API), absente si le formateur n'en a pas mis. */
  coverImageUrl?: string;
  learnerCount: number;
  /** Note moyenne sur 5, absente tant que personne n'a noté. */
  averageRating?: number;
  ratingCount: number;
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
  price: number;
  /** URL de l'image de couverture (chemin API), absente si le formateur n'en a pas mis. */
  coverImageUrl?: string;
  learnerCount: number;
  /** Note moyenne sur 5, absente tant que personne n'a noté. */
  averageRating?: number;
  ratingCount: number;
}

export interface CourseRating {
  average?: number;
  count: number;
  myStars?: number;
  myComment?: string;
  enrolled: boolean;
  progressPercent: number;
  /** Progression minimale (%) pour noter le cours. */
  requiredProgress: number;
  canRate: boolean;
}

export interface CourseReview {
  id: number;
  authorName: string;
  stars: number;
  comment: string;
  updatedAt: string;
}

export interface MyReview {
  id: number;
  courseId: number;
  courseTitle: string;
  courseSlug: string;
  stars: number;
  /** Absent pour une note sans avis écrit. */
  comment: string | null;
  updatedAt: string;
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
  price: number;
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

export interface ChapterTranslationItem {
  chapterId: number;
  originalTitle: string;
  translatedTitle?: string;
}

export interface CourseTranslationEdit {
  languageCode: string;
  courseTitle?: string;
  courseDescription?: string;
  chapters: ChapterTranslationItem[];
}

export interface ChapterTranslationInput {
  chapterId: number;
  title?: string;
}

export interface UpdateCourseTranslationRequest {
  courseTitle: string;
  courseDescription?: string;
  chapters: ChapterTranslationInput[];
}
