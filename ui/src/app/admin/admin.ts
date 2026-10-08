import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { clearSession, getCustomer } from '../session';

@Component({
  selector: 'app-admin',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin.html',
  styleUrl: './admin.css',
})
export class Admin {
  private readonly router = inject(Router);
  protected readonly customer = getCustomer();

  constructor() {
    if (!this.customer?.admin) this.router.navigate(['/']);
  }

  protected logout(): void {
    clearSession();
    this.router.navigate(['/']);
  }
}
