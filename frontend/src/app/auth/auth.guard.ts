import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { inject } from '@angular/core';
import { of } from 'rxjs';

export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  if (auth.isAuthenticated()) {
    return of(true);
  }
  const router = inject(Router);
  router.navigate(['login']);
  return of(false);
};
