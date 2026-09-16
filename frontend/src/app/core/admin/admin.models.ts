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
  provider: 'STRIPE' | 'ORANGE_MONEY';
  plan: 'MONTHLY' | 'ANNUAL';
  amount: number;
  currency: string;
  status: 'PENDING' | 'SUCCEEDED' | 'FAILED';
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
