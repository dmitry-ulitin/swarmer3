import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { TuiAlertModule, TuiRootModule } from '@taiga-ui/core';
import { jwtInterceptor } from './auth/jwt.interceptor';
import { errorInterceptor } from './auth/error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [provideRouter(routes), provideHttpClient(withInterceptors([jwtInterceptor, errorInterceptor])), provideAnimations(), importProvidersFrom(TuiRootModule), importProvidersFrom(TuiAlertModule)]
};
