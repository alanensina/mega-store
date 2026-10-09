import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { getCustomer } from '../session';

interface OrderItem {
  productId: string;
  quantity: number;
  price: number;
}

interface OrderRow {
  id: string;
  customerId: string;
  orderNumber: number;
  totalPrice: number;
  status: string;
  createdAt: string;
  items: OrderItem[];
}

@Component({
  selector: 'app-my-orders',
  imports: [CurrencyPipe, DatePipe],
  templateUrl: './my-orders.html',
  styleUrl: './my-orders.css',
})
export class MyOrders {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly customer = getCustomer();

  protected readonly orders = signal<OrderRow[]>([]);
  protected readonly names = signal<Record<string, string>>({});
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly cancelling = signal(false);
  protected readonly selected = signal<OrderRow | null>(null);

  constructor() {
    if (!this.customer) {
      this.router.navigate(['/']);
      return;
    }
    this.http.get<OrderRow[]>('/api/cart').subscribe({
      next: (orders) => {
        this.orders.set(orders.filter((o) => o.customerId === this.customer?.id));
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(err.status === 0 ? 'Unable to reach the server.' : 'Could not load orders.');
      },
    });
    this.http.get<{ id: string; name: string }[]>('/api/product').subscribe({
      next: (products) => this.names.set(Object.fromEntries(products.map((p) => [p.id, p.name]))),
    });
  }

  protected itemCount(order: OrderRow): number {
    return (order.items ?? []).reduce((n, i) => n + i.quantity, 0);
  }

  protected nameOf(productId: string): string {
    return this.names()[productId] ?? productId;
  }

  protected back(): void {
    this.router.navigate(['/home']);
  }

  protected open(order: OrderRow): void {
    this.selected.set(order);
  }

  protected canCancel(order: OrderRow): boolean {
    return order.status !== 'CANCELLED' && order.status !== 'DELIVERED';
  }

  protected cancel(order: OrderRow): void {
    this.cancelling.set(true);
    this.http.put<OrderRow>(`/api/cart/cancel/${order.id}`, null).subscribe({
      next: (updated) => {
        this.orders.update((list) => list.map((o) => (o.id === order.id ? { ...o, status: updated.status } : o)));
        this.cancelling.set(false);
        this.close();
      },
      error: () => {
        this.cancelling.set(false);
        this.close();
        this.error.set('Could not cancel the order.');
      },
    });
  }

  protected close(): void {
    this.selected.set(null);
  }
}
