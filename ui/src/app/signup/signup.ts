import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

@Component({
  selector: 'app-signup',
  imports: [ReactiveFormsModule],
  templateUrl: './signup.html',
  styleUrl: '../login/login.css',
})
export class Signup {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);

  protected readonly submitting = signal(false);
  protected readonly errors = signal<string[]>([]);

  protected readonly form = this.fb.nonNullable.group({
    name: [''],
    username: [''],
    password: [''],
    email: [''],
  });

  protected signUp(): void {
    const { name, username, password, email } = this.form.getRawValue();
    const errors: string[] = [];

    if (!name.trim()) errors.push('Name is required.');
    if (!username.trim()) errors.push('Username is required.');
    if (!EMAIL_PATTERN.test(email.trim())) errors.push('Email is invalid.');
    if (password.length < 8 || password.length > 16) {
      errors.push('Password must be between 8 and 16 characters.');
    }

    this.errors.set(errors);
    if (errors.length) return;

    this.submitting.set(true);
    this.http.post('/api/customer', { name, username, password, email }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.router.navigate(['/'], { state: { message: 'New user registered' } });
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        this.errors.set([this.extractMessage(err)]);
      },
    });
  }

  protected cancel(): void {
    this.router.navigate(['/']);
  }

  private extractMessage(err: HttpErrorResponse): string {
    if (err.status === 0) return 'Unable to reach the server.';
    const body = err.error;
    if (typeof body === 'string' && body) return body;
    if (body?.message) return body.message;
    return 'Could not create the user.';
  }
}
