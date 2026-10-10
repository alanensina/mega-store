import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-create-user',
  imports: [ReactiveFormsModule],
  templateUrl: './create-user.html',
  styleUrl: './create-user.css',
})
export class CreateUser {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);

  protected readonly editingId = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly success = signal(false);
  protected readonly errors = signal<string[]>([]);

  protected readonly form = this.fb.nonNullable.group({
    name: [''],
    email: [''],
    username: [''],
    password: [''],
  });

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editingId.set(id);
      this.http.get<{ name: string; email: string; username: string }>(`/api/customer/${id}`).subscribe({
        next: (u) => this.form.patchValue({ name: u.name, email: u.email, username: u.username }),
        error: (err: HttpErrorResponse) => this.errors.set([this.extractMessage(err)]),
      });
    }
  }

  protected save(): void {
    const { name, email, username, password } = this.form.getRawValue();
    const id = this.editingId();
    const errors: string[] = [];
    this.success.set(false);

    if (!name.trim()) errors.push('Name is required.');
    if (!/^[^\s@]+@[^\s@.]+(?:\.[^\s@.]+)+$/.test(email.trim())) errors.push('A valid email is required.');
    if (!username.trim()) errors.push('Username is required.');
    if ((!id || password) && (password.length < 8 || password.length > 16)) {
      errors.push('Password must be between 8 and 16 characters.');
    }

    this.errors.set(errors);
    if (errors.length) return;

    const body = { name: name.trim(), email: email.trim(), username: username.trim(), password: password || null };
    this.submitting.set(true);
    const request = id ? this.http.put('/api/customer', { id, ...body }) : this.http.post('/api/customer', body);
    request.subscribe({
      next: () => {
        this.submitting.set(false);
        if (id) {
          this.router.navigate(['/admin/users']);
          return;
        }
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
    this.form.reset({ name: '', email: '', username: '', password: '' });
    this.errors.set([]);
    this.success.set(false);
  }

  protected cancel(): void {
    this.router.navigate(['/admin/users']);
  }

  private extractMessage(err: HttpErrorResponse): string {
    if (err.status === 0) return 'Unable to reach the server.';
    const body = err.error;
    if (typeof body === 'string' && body) return body;
    if (body?.message) return body.message;
    return 'Could not save the user.';
  }
}
