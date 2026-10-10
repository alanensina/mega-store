import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { Router } from '@angular/router';
import { getCustomer } from '../../session';

interface UserRow {
  id: string;
  name: string;
  email: string;
  username: string;
  admin: boolean;
}

@Component({
  selector: 'app-users',
  templateUrl: './users.html',
  styleUrl: './users.css',
})
export class Users {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  protected readonly currentId = getCustomer()?.id;
  protected readonly users = signal<UserRow[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly pendingDelete = signal<UserRow | null>(null);
  protected readonly deleting = signal(false);
  private readonly deleteDialog = viewChild<ElementRef<HTMLDialogElement>>('deleteDialog');

  private readonly showDeleteDialog = effect(() => {
    const dialog = this.deleteDialog()?.nativeElement;
    if (dialog && !dialog.open) dialog.showModal();
  });

  constructor() {
    this.http.get<UserRow[]>('/api/customer').subscribe({
      next: (users) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(err.status === 0 ? 'Unable to reach the server.' : 'Could not load users.');
      },
    });
  }

  protected create(): void {
    this.router.navigate(['/admin/createUser']);
  }

  protected edit(user: UserRow): void {
    this.router.navigate(['/admin/createUser', user.id]);
  }

  protected askDelete(user: UserRow): void {
    this.pendingDelete.set(user);
  }

  protected cancelDelete(): void {
    if (!this.deleting()) this.pendingDelete.set(null);
  }

  protected confirmDelete(): void {
    const user = this.pendingDelete();
    if (!user) return;
    this.deleting.set(true);
    this.http.delete('/api/customer', { params: { id: user.id } }).subscribe({
      next: () => {
        this.deleting.set(false);
        this.pendingDelete.set(null);
        this.users.update((list) => list.filter((u) => u.id !== user.id));
      },
      error: () => {
        this.deleting.set(false);
        this.pendingDelete.set(null);
        this.error.set('Could not delete the user.');
      },
    });
  }
}
