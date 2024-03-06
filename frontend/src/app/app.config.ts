import { ApplicationConfig, LOCALE_ID, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { TuiAlertModule, TuiRootModule } from '@taiga-ui/core';
import { jwtInterceptor } from './auth/jwt.interceptor';
import { errorInterceptor } from './auth/error.interceptor';
import localeRu from '@angular/common/locales/ru';
import { registerLocaleData } from '@angular/common';

registerLocaleData(localeRu, 'ru');

export const appConfig: ApplicationConfig = {
  providers: [provideRouter(routes), provideHttpClient(withInterceptors([jwtInterceptor, errorInterceptor])),
    provideAnimations(), importProvidersFrom(TuiRootModule), importProvidersFrom(TuiAlertModule), { provide: LOCALE_ID, useValue: 'ru' }]
};
