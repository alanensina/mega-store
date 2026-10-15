import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { QuantityStepper } from './quantity-stepper';

describe('QuantityStepper', () => {
  it('disables decrementing at one and emits the next quantity', () => {
    TestBed.configureTestingModule({ imports: [QuantityStepper] });
    const fixture = TestBed.createComponent(QuantityStepper);
    fixture.componentRef.setInput('label', 'Mug');
    fixture.componentRef.setInput('value', 1);
    const emitted: number[] = [];
    fixture.componentInstance.valueChange.subscribe((value) => emitted.push(value));
    fixture.detectChanges();

    const buttons = fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;
    expect(buttons[0].disabled).toBe(true);
    expect(buttons[0].getAttribute('aria-label')).toBe('Decrease quantity of Mug');

    buttons[1].click();

    expect(emitted).toEqual([2]);
  });

  it('emits a manually entered quantity', () => {
    TestBed.configureTestingModule({ imports: [QuantityStepper] });
    const fixture = TestBed.createComponent(QuantityStepper);
    fixture.componentRef.setInput('label', 'Mug');
    fixture.componentRef.setInput('value', 1);
    const emitted: number[] = [];
    fixture.componentInstance.valueChange.subscribe((value) => emitted.push(value));
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = '4';
    input.dispatchEvent(new Event('change'));

    expect(emitted).toEqual([4]);
    expect(input.getAttribute('aria-label')).toBe('Quantity of Mug');
  });
});
