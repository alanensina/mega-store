import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-create-product',
  imports: [ReactiveFormsModule],
  templateUrl: './create-product.html',
  styleUrl: './create-product.css',
})
export class CreateProduct {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);

  protected readonly submitting = signal(false);
  protected readonly success = signal(false);
  protected readonly errors = signal<string[]>([]);

  protected readonly form = this.fb.nonNullable.group({
    name: [''],
    price: [null as number | null],
    active: [true],
    quantity: [null as number | null],
  });

  protected create(): void {
    const { name, price, active, quantity } = this.form.getRawValue();
    const errors: string[] = [];
    this.success.set(false);

    if (!name.trim()) errors.push('Name is required.');
    if (price === null || price < 0) errors.push('Price must be zero or greater.');
    if (quantity === null || quantity < 0 || !Number.isInteger(quantity)) {
      errors.push('Quantity must be a whole number zero or greater.');
    }

    this.errors.set(errors);
    if (errors.length) return;

    this.submitting.set(true);
    this.http.post('/api/product', { name: name.trim(), price, active, quantity }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.clean();
        this.success.set(true);
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        this.errors.set([this.extractMessage(err)]);
      },
    });
  }

  protected clean(): void {
    this.form.reset({ name: '', price: null, active: true, quantity: null });
    this.errors.set([]);
    this.success.set(false);
  }

  protected cancel(): void {
    this.router.navigate(['/admin']);
  }

  private extractMessage(err: HttpErrorResponse): string {
    if (err.status === 0) return 'Unable to reach the server.';
    const body = err.error;
    if (typeof body === 'string' && body) return body;
    if (body?.message) return body.message;
    return 'Could not create the product.';
  }
}
