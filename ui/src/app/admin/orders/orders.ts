import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';

interface OrderItem {
  productId: string;
  quantity: number;
  price: number;
}

interface OrderRow {
  id: string;
  orderNumber: number;
  totalPrice: number;
  status: string;
  createdAt: string;
  items: OrderItem[];
}

@Component({
  selector: 'app-orders',
  imports: [CurrencyPipe, DatePipe],
  templateUrl: './orders.html',
  styleUrl: './orders.css',
})
export class Orders {
  private readonly http = inject(HttpClient);

  protected readonly orders = signal<OrderRow[]>([]);
  protected readonly names = signal<Record<string, string>>({});
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly selected = signal<OrderRow | null>(null);
  private readonly orderDialog = viewChild<ElementRef<HTMLDialogElement>>('orderDialog');

  private readonly showSelectedDialog = effect(() => {
    const dialog = this.orderDialog()?.nativeElement;
    if (dialog && !dialog.open) dialog.showModal();
  });

  constructor() {
    this.http.get<OrderRow[]>('/api/cart').subscribe({
      next: (orders) => {
        this.orders.set(orders);
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

  protected open(order: OrderRow): void {
    this.selected.set(order);
  }

  protected close(): void {
    this.selected.set(null);
  }
}
