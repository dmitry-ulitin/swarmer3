import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { EMPTY, catchError, of } from 'rxjs';
import { DataService } from '../services/data.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const data = inject(DataService);
  return next(req).pipe(catchError((err) => {
    if (err.status === 403) {
      // auto logout if 403 response returned from api
      data.reset();
      return EMPTY;
    }
    throw err;
  }));;
};
