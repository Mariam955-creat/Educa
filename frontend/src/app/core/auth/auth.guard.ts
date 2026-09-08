import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { RoleName } from './auth.models';
import { AuthService } from './auth.service';

/** Exige un utilisateur authentifié. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isAuthenticated() ? true : router.createUrlTree(['/login']);
};

/** Exige l'un des rôles fournis, sinon renvoie vers l'accueil du rôle courant. */
export const roleGuard =
  (...roles: RoleName[]): CanActivateFn =>
  () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (!auth.isAuthenticated()) return router.createUrlTree(['/login']);
    return auth.hasAnyRole(roles) ? true : router.createUrlTree([auth.homePathForRole()]);
  };
