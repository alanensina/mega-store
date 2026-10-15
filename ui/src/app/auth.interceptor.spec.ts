import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { authInterceptor } from './auth.interceptor';
import { saveSession } from './session';

describe('authInterceptor', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('adds the session token to API requests', () => {
    saveSession({ id: 'c-1', name: 'Alex', admin: false }, 'jwt-token');

    TestBed.inject(HttpClient).get('/api/products').subscribe();

    const request = httpTesting.expectOne('/api/products');
    expect(request.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    request.flush([]);
  });

  it('does not add a token to authentication requests', () => {
    saveSession({ id: 'c-1', name: 'Alex', admin: false }, 'jwt-token');

    TestBed.inject(HttpClient).post('/api/auth', {}).subscribe();

    const request = httpTesting.expectOne('/api/auth');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });

  it('preserves an explicit authorization header', () => {
    saveSession({ id: 'c-1', name: 'Alex', admin: false }, 'jwt-token');

    TestBed.inject(HttpClient)
      .get('/api/products', { headers: { Authorization: 'Custom token' } })
      .subscribe();

    const request = httpTesting.expectOne('/api/products');
    expect(request.request.headers.get('Authorization')).toBe('Custom token');
    request.flush([]);
  });
});
