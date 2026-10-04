export type RoleName = 'LEARNER' | 'INSTRUCTOR' | 'ADMIN';

export interface User {
  id: number;
  email: string;
  fullName: string;
  preferredLanguage: string;
  roles: RoleName[];
  headline?: string;
  bio?: string;
  /** Code pays ISO 3166-1 alpha-2. */
  country?: string;
  phone?: string;
}

/** Mise à jour partielle du profil : un champ absent reste inchangé, une chaîne vide l'efface. */
export interface ProfileUpdate {
  fullName?: string;
  preferredLanguage?: string;
  headline?: string;
  bio?: string;
  country?: string;
  phone?: string;
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
  country?: string;
}
