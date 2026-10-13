import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { CurrencyPipe } from '@angular/common';
import { Component, HostListener, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { QuantityStepper } from '../quantity-stepper';
import { CartService } from '../cart.service';
import { clearSession, getCustomer } from '../session';

interface Product {
  id: string;
  name: string;
  price: number;
  imageUrl: string | null;
  active: boolean;
}

@Component({
  selector: 'app-home',
  imports: [CurrencyPipe, QuantityStepper],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  protected readonly cart = inject(CartService);
  protected readonly customer = getCustomer();

  protected readonly products = signal<Product[]>([]);
  protected readonly quantities = signal<Record<string, number>>({});
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly added = signal('');
  protected readonly preview = signal<{ url: string; name: string; top: number; left: number } | null>(null);

  ngOnInit(): void {
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
    this.cart.add({ productId: product.id, name: product.name, price: product.price, imageUrl: product.imageUrl }, this.quantity(product.id));
    this.setQuantity(product.id, 1);
    this.added.set(product.name);
    setTimeout(() => this.added() === product.name && this.added.set(''), 3000);
  }

  protected openImage(product: Product, event: Event): void {
    if (!product.imageUrl) return;
    const width = Math.min(520, window.innerWidth - 24);
    const height = Math.min(520, window.innerHeight - 24);
    const gap = 12;
    const rect = (event.target as HTMLElement).getBoundingClientRect();
    let left = rect.right + gap;
    if (left + width > window.innerWidth - gap) {
      left = rect.left - width - gap;
    }
    left = Math.max(gap, left);
    const top = Math.min(Math.max(gap, rect.top + rect.height / 2 - height / 2), window.innerHeight - height - gap);
    this.preview.set({ url: product.imageUrl, name: product.name, top, left });
  }

  @HostListener('document:keydown.escape')
  protected closeImage(): void {
    this.preview.set(null);
  }

  protected openCart(): void {
    this.router.navigate(['/cart']);
  }

  protected openMyOrders(): void {
    this.router.navigate(['/myOrders']);
  }

  protected logout(): void {
    this.cart.clear();
    clearSession();
    this.router.navigate(['/']);
  }
}
