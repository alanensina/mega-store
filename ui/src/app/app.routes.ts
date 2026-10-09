import { Routes } from '@angular/router';
import { Login } from './login/login';
import { Signup } from './signup/signup';
import { Home } from './home/home';
import { MyOrders } from './my-orders/my-orders';
import { Cart } from './cart/cart';
import { Admin } from './admin/admin';
import { Products } from './admin/products/products';
import { Users } from './admin/users/users';
import { Orders } from './admin/orders/orders';
import { CreateUser } from './admin/create-user/create-user';
import { CreateProduct } from './admin/create-product/create-product';

export const routes: Routes = [
  { path: '', component: Login },
  { path: 'signup', component: Signup },
  { path: 'home', component: Home },
  { path: 'myOrders', component: MyOrders },
  { path: 'cart', component: Cart },
  {
    path: 'admin',
    component: Admin,
    children: [
      { path: 'products', component: Products },
      { path: 'users', component: Users },
      { path: 'orders', component: Orders },
      { path: 'createUser', component: CreateUser },
      { path: 'createUser/:id', component: CreateUser },
      { path: 'createProduct', component: CreateProduct },
      { path: 'createProduct/:id', component: CreateProduct },
    ],
  },
  { path: '**', redirectTo: '' },
];
