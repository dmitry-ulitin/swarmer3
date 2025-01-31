import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { EMPTY, catchError, of } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  return next(req).pipe(catchError((err) => {
    if (err.status === 401) {
      // auto logout if 401 response returned from api
      auth.logout();
      return EMPTY;
    }
    throw err;
  }));;
};
