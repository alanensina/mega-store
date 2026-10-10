import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { CurrencyPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CartService } from '../cart.service';
import { getCustomer } from '../session';

@Component({
  selector: 'app-cart',
  imports: [CurrencyPipe],
  templateUrl: './cart.html',
  styleUrl: '../home/home.css',
})
export class Cart implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  protected readonly cart = inject(CartService);
  private readonly customer = getCustomer();

  protected readonly buying = signal(false);
  protected readonly error = signal('');
  protected readonly success = signal(false);

  ngOnInit(): void {
    if (!this.customer) this.router.navigate(['/']);
  }

  protected setQuantity(productId: string, value: number): void {
    this.cart.setQuantity(productId, value);
  }

  protected back(): void {
    this.router.navigate(['/home']);
  }

  protected buy(): void {
    if (!this.customer || this.buying() || this.cart.items().length === 0) return;
    this.buying.set(true);
    this.error.set('');
    const items = this.cart.items().map((i) => ({
      productId: i.productId,
      quantity: i.quantity,
      price: i.price,
    }));
    this.http.post('/api/cart', { customerId: this.customer.id, items }).subscribe({
      next: () => {
        this.buying.set(false);
        this.cart.clear();
        this.success.set(true);
      },
      error: (err: HttpErrorResponse) => {
        this.buying.set(false);
        this.error.set(err.status === 0 ? 'Unable to reach the server.' : 'Could not complete the purchase.');
      },
    });
  }
}
