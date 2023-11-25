import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { LoginComponent } from './auth/login.component';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
    { path: 'login', component: LoginComponent },  
    { path: '', pathMatch: 'full', component: HomeComponent, canActivate: [authGuard] },
];
