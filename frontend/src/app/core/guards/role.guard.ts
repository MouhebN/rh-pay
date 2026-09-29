import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { Role } from '../models/role.model';
import { TokenService } from '../services/token.service';

export const roleGuard: CanActivateFn = (route) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);
  const expectedRoles = (route.data['roles'] ?? []) as Role[];
  const currentUser = tokenService.currentUser();

  if (currentUser && expectedRoles.includes(currentUser.role)) {
    return true;
  }

  if (currentUser?.role === 'ADMIN_RH') {
    return router.createUrlTree(['/admin/dashboard']);
  }

  if (currentUser?.role === 'EMPLOYE') {
    return router.createUrlTree(['/employee/dashboard']);
  }

  return router.createUrlTree(['/login']);
};
