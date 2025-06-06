import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { LoginComponent } from './auth/login.component';
import { RegistrationComponent } from './auth/registration.component';
import { ChangePasswordComponent } from './auth/change-password.component';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
    { path: 'login', component: LoginComponent },
    { path: 'register', component: RegistrationComponent },
    { path: 'change-password', component: ChangePasswordComponent, canActivate: [authGuard] },
    { path: '', pathMatch: 'full', component: HomeComponent, canActivate: [authGuard] },
];
