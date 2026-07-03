import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';
import { LoginComponent } from './features/auth/login/login.component';
import { PropertiesComponent } from './features/landlord/properties/properties.component';
import { MyLeaseComponent } from './features/tenant/my-lease/my-lease.component';
import { UsersComponent } from './features/admin/users/users.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: 'properties',
    component: PropertiesComponent,
    canActivate: [authGuard, roleGuard(['LANDLORD', 'PROPERTY_MANAGER'])]
  },
  {
    path: 'my-lease',
    component: MyLeaseComponent,
    canActivate: [authGuard, roleGuard(['TENANT'])]
  },
  {
    path: 'admin/users',
    component: UsersComponent,
    canActivate: [authGuard, roleGuard(['ADMIN'])]
  },
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: '**', redirectTo: 'login' }
];
