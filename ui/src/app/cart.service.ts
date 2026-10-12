import { Injectable, computed, signal } from '@angular/core';

export interface CartItem {
  productId: string;
  name: string;
  price: number;
  imageUrl?: string | null;
  quantity: number;
}

const KEY = 'cart';

@Injectable({ providedIn: 'root' })
export class CartService {
  readonly items = signal<CartItem[]>(this.read());
  readonly count = computed(() => this.items().reduce((n, i) => n + i.quantity, 0));
  readonly total = computed(() => this.items().reduce((sum, i) => sum + i.price * i.quantity, 0));

  add(item: Omit<CartItem, 'quantity'>, quantity: number): void {
    const qty = Math.max(1, Math.floor(quantity) || 1);
    this.update((list) => {
      const existing = list.find((i) => i.productId === item.productId);
      return existing
        ? list.map((i) => (i === existing ? { ...i, quantity: i.quantity + qty } : i))
        : [...list, { ...item, quantity: qty }];
    });
  }

  setQuantity(productId: string, quantity: number): void {
    const qty = Math.max(1, Math.floor(quantity) || 1);
    this.update((list) => list.map((i) => (i.productId === productId ? { ...i, quantity: qty } : i)));
  }

  remove(productId: string): void {
    this.update((list) => list.filter((i) => i.productId !== productId));
  }

  clear(): void {
    this.update(() => []);
  }

  private update(fn: (list: CartItem[]) => CartItem[]): void {
    this.items.update(fn);
    sessionStorage.setItem(KEY, JSON.stringify(this.items()));
  }

  private read(): CartItem[] {
    try {
      return JSON.parse(sessionStorage.getItem(KEY) ?? '[]');
    } catch {
      return [];
    }
  }
}
