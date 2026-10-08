import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { getCustomer } from '../session';

@Component({
  selector: 'app-home',
  templateUrl: './home.html',
  styleUrl: '../login/login.css',
})
export class Home {
  protected readonly customer = getCustomer();

  constructor() {
    if (!this.customer) inject(Router).navigate(['/']);
  }
}
