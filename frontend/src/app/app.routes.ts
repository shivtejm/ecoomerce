import { Routes } from '@angular/router';

import { Auth } from './auth/auth';
import { ResetPasswordComponent } from './auth/reset-password/reset-password.component';
import { Home } from './home/home';
import { Dashboard } from './home/dashboard/dashboard';
import { Profile } from './home/profile/profile';
import { authGuard, guestGuard } from './auth.guard';

import { StoreComponent } from './home/store/store';
import { ProductDetailComponent } from './home/product-detail/product-detail';
import { AdminComponent } from './home/admin/admin';
import { adminGuard } from './admin.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'home',
    pathMatch: 'full'
  },
  {
    path: 'auth',
    component: Auth,
    canActivate: [guestGuard]
  },
  {
    path: 'reset-password',
    component: ResetPasswordComponent
  },
  {
    path: 'home',
    component: Home,
    children: [
      {
        path: '',
        component: Dashboard
      },
      {
        path: 'dashboard',
        component: Dashboard
      },
      {
        path: 'store',
        component: StoreComponent
      },
      {
        path: 'product/:id',
        component: ProductDetailComponent
      },
      {
        path: 'profile',
        component: Profile,
        canActivate: [authGuard]
      },
      {
        path: 'admin',
        component: AdminComponent,
        canActivate: [adminGuard]
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'home'
  }
];