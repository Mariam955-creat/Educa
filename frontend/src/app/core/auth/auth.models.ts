export type RoleName = 'LEARNER' | 'INSTRUCTOR' | 'ADMIN';

export interface User {
  id: number;
  email: string;
  fullName: string;
  preferredLanguage: string;
  roles: RoleName[];
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: User;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
  preferredLanguage?: string;
}
