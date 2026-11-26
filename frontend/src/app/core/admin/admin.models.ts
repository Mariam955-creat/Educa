import { RoleName } from '../auth/auth.models';

export interface AdminUser {
  id: number;
  email: string;
  fullName: string;
  roles: RoleName[];
  enabled: boolean;
  createdAt: string;
}

export type InstructorRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

/** Demande de rôle formateur (vue admin, ou par son auteur dans l'espace compte). */
export interface InstructorRequest {
  id: number;
  userId: number;
  fullName: string | null;
  email: string | null;
  headline: string | null;
  motivation: string | null;
  status: InstructorRequestStatus;
  rejectionReason: string | null;
  createdAt: string;
  decidedAt: string | null;
  documents: InstructorRequestDocument[];
}

/** Pièce jointe d'une demande : dernier diplôme, CV ou pièce d'identité. */
export interface InstructorRequestDocument {
  id: number;
  kind: 'DIPLOMA' | 'CV' | 'ID_DOCUMENT';
  originalName: string;
  mimeType: string;
  sizeBytes: number;
}

export interface CertificateRegistryEntry {
  id: number;
  serialNumber: string;
  verificationCode: string;
  courseId: number;
  courseTitle: string;
  holderName: string;
  controlsAverage: number;
  finalExamScore: number;
  finalGrade: number;
  issuedAt: string;
}

export interface PaymentRegistryEntry {
  id: number;
  userName: string;
  courseTitle: string;
  provider: 'STRIPE' | 'ORANGE_MONEY';
  amount: number;
  currency: string;
  status: 'PENDING' | 'SUCCEEDED' | 'FAILED';
  invoiceNumber: string | null;
  createdAt: string;
}

export interface ReviewRegistryEntry {
  id: number;
  courseId: number;
  courseTitle: string;
  courseSlug: string;
  authorName: string;
  stars: number;
  /** Absent pour une note sans avis écrit. */
  comment: string | null;
  updatedAt: string;
}

/** Indicateurs du tableau de bord admin ; « recent » = 30 derniers jours. */
export interface AdminStats {
  users: {
    total: number;
    learners: number;
    instructors: number;
    admins: number;
    disabled: number;
    recent: number;
    instructorRequests: number;
  };
  courses: { total: number; published: number; drafts: number };
  enrollments: { total: number; recent: number };
  revenue: { total: number; sales: number; recent: number; recentSales: number; currency: string };
  /** `revenue` : part des abonnements, déjà comprise dans `revenue.total`. */
  subscriptions: { active: number; revenue: number };
  certificates: number;
  reviews: { count: number; average: number | null };
}

/** Compte à la corbeille. */
export interface TrashedUser {
  id: number;
  email: string;
  fullName: string;
  roles: RoleName[];
  deletedAt: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
