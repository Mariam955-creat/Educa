import { RoleName } from '../auth/auth.models';

export interface AdminUser {
  id: number;
  email: string;
  fullName: string;
  roles: RoleName[];
  enabled: boolean;
  createdAt: string;
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
  users: { total: number; learners: number; instructors: number; admins: number; disabled: number; recent: number };
  courses: { total: number; published: number; drafts: number };
  enrollments: { total: number; recent: number };
  revenue: { total: number; sales: number; recent: number; recentSales: number; currency: string };
  certificates: number;
  reviews: { count: number; average: number | null };
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
