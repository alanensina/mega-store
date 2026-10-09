import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CartService } from '../cart.service';
import { clearSession, getCustomer } from '../session';

interface Product {
  id: string;
  name: string;
  price: number;
  active: boolean;
}

@Component({
  selector: 'app-home',
  imports: [CurrencyPipe],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  protected readonly cart = inject(CartService);
  protected readonly customer = getCustomer();

  protected readonly products = signal<Product[]>([]);
  protected readonly quantities = signal<Record<string, number>>({});
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly added = signal('');

  constructor() {
    if (!this.customer) {
      this.router.navigate(['/']);
      return;
    }
    this.http.get<Product[]>('/api/product').subscribe({
      next: (products) => {
        this.products.set(products.filter((p) => p.active));
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(err.status === 0 ? 'Unable to reach the server.' : 'Could not load products.');
      },
    });
  }

  protected quantity(id: string): number {
    return this.quantities()[id] ?? 1;
  }

  protected setQuantity(id: string, value: number): void {
    const qty = Math.max(1, Math.floor(value) || 1);
    this.quantities.update((q) => ({ ...q, [id]: qty }));
  }

  protected addToCart(product: Product): void {
    this.cart.add({ productId: product.id, name: product.name, price: product.price }, this.quantity(product.id));
    this.setQuantity(product.id, 1);
    this.added.set(product.name);
    setTimeout(() => this.added() === product.name && this.added.set(''), 3000);
  }

  protected openCart(): void {
    this.router.navigate(['/cart']);
  }

  protected logout(): void {
    this.cart.clear();
    clearSession();
    this.router.navigate(['/']);
  }
}
