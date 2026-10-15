import { beforeEach, describe, expect, it } from 'vitest';
import { clearSession, getCustomer, getToken, saveSession } from './session';

describe('session storage', () => {
  beforeEach(() => sessionStorage.clear());

  it('stores and reads the customer and token', () => {
    const customer = { id: 'c-1', name: 'Alex', admin: false };

    saveSession(customer, 'jwt-token');

    expect(getCustomer()).toEqual(customer);
    expect(getToken()).toBe('jwt-token');
  });

  it('returns null when no session exists', () => {
    expect(getCustomer()).toBeNull();
    expect(getToken()).toBeNull();
  });

  it('clears the stored session', () => {
    saveSession({ id: 'c-1', name: 'Alex', admin: false }, 'jwt-token');

    clearSession();

    expect(sessionStorage.getItem('session')).toBeNull();
    expect(getCustomer()).toBeNull();
    expect(getToken()).toBeNull();
  });

  it('returns null for malformed persisted JSON', () => {
    sessionStorage.setItem('session', '{invalid');

    expect(getCustomer()).toBeNull();
    expect(getToken()).toBeNull();
  });
});
