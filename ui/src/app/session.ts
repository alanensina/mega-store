export interface SessionCustomer {
  id: string;
  name: string;
  admin: boolean;
}

const KEY = 'session';

export function saveSession(customer: SessionCustomer, token: string): void {
  sessionStorage.setItem(KEY, JSON.stringify({ customer, token }));
}

export function getCustomer(): SessionCustomer | null {
  try {
    return JSON.parse(sessionStorage.getItem(KEY) ?? 'null')?.customer ?? null;
  } catch {
    return null;
  }
}

export function clearSession(): void {
  sessionStorage.removeItem(KEY);
}

export function getToken(): string | null {
  try {
    return JSON.parse(sessionStorage.getItem(KEY) ?? 'null')?.token ?? null;
  } catch {
    return null;
  }
}
