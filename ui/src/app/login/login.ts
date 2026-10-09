import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CartService } from '../cart.service';
import { SessionCustomer, clearSession, saveSession } from '../session';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);
  private readonly cart = inject(CartService);

  protected readonly message: string | undefined = history.state?.message;
  protected readonly error = signal<string | null>(null);
  protected readonly submitting = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  constructor() {
    clearSession();
    this.cart.clear();
  }

  protected login(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.error.set(null);
    this.submitting.set(true);
    this.http
      .post<{ customer: SessionCustomer; token: string }>('/api/auth', this.form.getRawValue())
      .subscribe({
      next: ({ customer, token }) => {
        this.submitting.set(false);
        saveSession(customer, token);
        this.router.navigate([customer.admin ? '/admin' : '/home']);
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        this.error.set(this.extractMessage(err));
      },
    });
  }

  protected newUser(): void {
    this.router.navigate(['/signup']);
  }

  private extractMessage(err: HttpErrorResponse): string {
    if (err.status === 0) return 'Unable to reach the server.';
    const body = err.error;
    if (typeof body === 'string' && body) return body;
    if (typeof body?.detail === 'string' && body.detail) return body.detail;
    if (typeof body?.message === 'string' && body.message) return body.message;
    return 'Could not log in.';
  }
}
