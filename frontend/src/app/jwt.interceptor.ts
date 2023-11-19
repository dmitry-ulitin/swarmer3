import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './services/auth.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const nextReq = auth.isAuthenticated() ? req.clone({
    headers: req.headers.set('Authorization', `Bearer ${auth.token()}`),
  }) : req;
  return next(nextReq);
};
