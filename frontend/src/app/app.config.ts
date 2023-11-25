import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { TuiAlertModule, TuiRootModule } from '@taiga-ui/core';
import { jwtInterceptor } from './auth/jwt.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [provideRouter(routes), provideHttpClient(withInterceptors([jwtInterceptor])), provideAnimations(), importProvidersFrom(TuiRootModule), importProvidersFrom(TuiAlertModule)]
};
