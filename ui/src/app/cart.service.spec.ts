import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { CartService } from './cart.service';

describe('CartService', () => {
  let cart: CartService;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({});
    cart = TestBed.inject(CartService);
  });

  it('starts with an empty cart', () => {
    expect(cart.items()).toEqual([]);
    expect(cart.count()).toBe(0);
    expect(cart.total()).toBe(0);
  });

  it('restores items from session storage', () => {
    sessionStorage.setItem(
      'cart',
      JSON.stringify([{ productId: 'p-1', name: 'Mug', price: 12, quantity: 2 }]),
    );

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({});
    cart = TestBed.inject(CartService);

    expect(cart.items()).toEqual([{ productId: 'p-1', name: 'Mug', price: 12, quantity: 2 }]);
    expect(cart.count()).toBe(2);
    expect(cart.total()).toBe(24);
  });

  it('adds products, combines matching products, and normalizes quantities', () => {
    const mug = { productId: 'p-1', name: 'Mug', price: 12 };

    cart.add(mug, 2.8);
    cart.add(mug, 0);

    expect(cart.items()).toEqual([{ ...mug, quantity: 3 }]);
    expect(cart.count()).toBe(3);
    expect(cart.total()).toBe(36);
  });

  it('updates quantities, removes products, and persists each change', () => {
    cart.add({ productId: 'p-1', name: 'Mug', price: 12 }, 1);
    cart.add({ productId: 'p-2', name: 'Book', price: 8 }, 1);

    cart.setQuantity('p-1', 3);

    expect(cart.items()[0].quantity).toBe(3);
    expect(JSON.parse(sessionStorage.getItem('cart') ?? '[]')).toEqual(cart.items());

    cart.remove('p-2');
    expect(cart.items().map(({ productId }) => productId)).toEqual(['p-1']);
  });

  it('clears all products and persisted state', () => {
    cart.add({ productId: 'p-1', name: 'Mug', price: 12 }, 2);

    cart.clear();

    expect(cart.items()).toEqual([]);
    expect(cart.count()).toBe(0);
    expect(cart.total()).toBe(0);
    expect(sessionStorage.getItem('cart')).toBe('[]');
  });

  it('recovers from invalid persisted JSON', () => {
    sessionStorage.setItem('cart', '{invalid');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({});

    expect(TestBed.inject(CartService).items()).toEqual([]);
  });
});
