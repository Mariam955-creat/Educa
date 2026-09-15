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

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
