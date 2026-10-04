export type ContentType = 'VIDEO' | 'DOCUMENT' | 'TEXT';

export type CourseLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'ALL_LEVELS';
export const COURSE_LEVELS: CourseLevel[] = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'ALL_LEVELS'];

export type CourseCategory =
  | 'DEVELOPMENT'
  | 'DATA'
  | 'NETWORK_SECURITY'
  | 'OFFICE'
  | 'LANGUAGES'
  | 'DESIGN'
  | 'BUSINESS'
  | 'OTHER';
export const COURSE_CATEGORIES: CourseCategory[] = [
  'DEVELOPMENT',
  'DATA',
  'NETWORK_SECURITY',
  'OFFICE',
  'LANGUAGES',
  'DESIGN',
  'BUSINESS',
  'OTHER',
];

/** Champs de présentation communs au résumé et au détail d'un cours. */
export interface CoursePresentation {
  subtitle?: string;
  category?: CourseCategory;
  level?: CourseLevel;
  durationHours?: number;
}

export interface CourseSummary extends CoursePresentation {
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

export interface CourseDetail extends CoursePresentation {
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
  objectives: string[];
  prerequisites: string[];
  targetAudience?: string;
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

export interface CourseFormValue extends CoursePresentation {
  title: string;
  description?: string;
  language: string;
  price: number;
  objectives: string[];
  prerequisites: string[];
  targetAudience?: string;
  controlWeight: number;
  examWeight: number;
  passThreshold: number;
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
