import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { Router } from '@angular/router';

interface ProductRow {
  id: string;
  name: string;
  price: number;
  active: boolean;
  quantity: number | null;
}

@Component({
  selector: 'app-products',
  imports: [CurrencyPipe],
  templateUrl: './products.html',
  styleUrl: './products.css',
})
export class Products {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  protected readonly products = signal<ProductRow[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly pendingDelete = signal<ProductRow | null>(null);
  protected readonly deleting = signal(false);
  private readonly deleteDialog = viewChild<ElementRef<HTMLDialogElement>>('deleteDialog');

  private readonly showDeleteDialog = effect(() => {
    const dialog = this.deleteDialog()?.nativeElement;
    if (dialog && !dialog.open) dialog.showModal();
  });

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.http.get<Omit<ProductRow, 'quantity'>[]>('/api/product').subscribe({
      next: (products) => {
        this.http.get<{ product: { id: string }; quantity: number }[]>('/api/stock').subscribe({
          next: (stocks) => this.show(products, stocks),
          error: () => this.show(products, []),
        });
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(err.status === 0 ? 'Unable to reach the server.' : 'Could not load products.');
      },
    });
  }

  private show(products: Omit<ProductRow, 'quantity'>[], stocks: { product: { id: string }; quantity: number }[]): void {
    const quantities = new Map(stocks.map((s) => [s.product.id, s.quantity]));
    this.products.set(products.map((p) => ({ ...p, quantity: quantities.get(p.id) ?? null })));
    this.error.set('');
    this.loading.set(false);
  }

  protected create(): void {
    this.router.navigate(['/admin/createProduct']);
  }

  protected edit(product: ProductRow): void {
    this.router.navigate(['/admin/createProduct', product.id]);
  }

  protected askDelete(product: ProductRow): void {
    this.pendingDelete.set(product);
  }

  protected cancelDelete(): void {
    if (!this.deleting()) this.pendingDelete.set(null);
  }

  protected confirmDelete(): void {
    const product = this.pendingDelete();
    if (!product) return;
    this.deleting.set(true);
    this.http.delete(`/api/product/${product.id}`).subscribe({
      next: () => {
        this.deleting.set(false);
        this.pendingDelete.set(null);
        this.products.update((list) => list.filter((p) => p.id !== product.id));
      },
      error: () => {
        this.deleting.set(false);
        this.pendingDelete.set(null);
        this.error.set('Could not delete the product.');
      },
    });
  }
}
