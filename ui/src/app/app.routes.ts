import { Routes } from '@angular/router';
import { Login } from './login/login';
import { Signup } from './signup/signup';
import { Home } from './home/home';
import { Admin } from './admin/admin';
import { CreateProduct } from './admin/create-product/create-product';

export const routes: Routes = [
  { path: '', component: Login },
  { path: 'signup', component: Signup },
  { path: 'home', component: Home },
  {
    path: 'admin',
    component: Admin,
    children: [{ path: 'createProduct', component: CreateProduct }],
  },
  { path: '**', redirectTo: '' },
];
