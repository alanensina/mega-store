import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-quantity-stepper',
  template: `
    <div class="stepper">
      <button type="button" [attr.aria-label]="'Decrease quantity of ' + label()" [disabled]="value() <= 1" (click)="valueChange.emit(value() - 1)">−</button>
      <input
        type="number"
        min="1"
        step="1"
        [attr.aria-label]="'Quantity of ' + label()"
        [value]="value()"
        (change)="valueChange.emit(+$any($event.target).value)"
      />
      <button type="button" [attr.aria-label]="'Increase quantity of ' + label()" (click)="valueChange.emit(value() + 1)">+</button>
    </div>
  `,
  styles: `
    .stepper {
      display: inline-flex;
      align-items: center;
      border: 1px solid var(--border);
      border-radius: 999px;
      overflow: hidden;
    }
    button {
      width: 2rem;
      height: 2.25rem;
      padding: 0;
      border: none;
      border-radius: 0;
      background: transparent;
      color: inherit;
      font-size: 1.1rem;
      line-height: 1;
      cursor: pointer;
    }
    button:hover:not(:disabled) { background: rgba(148, 163, 184, 0.18); }
    button:disabled { opacity: 0.4; cursor: not-allowed; }
    input {
      width: 2.5rem;
      padding: 0;
      border: none;
      background: transparent;
      color: inherit;
      text-align: center;
      font: inherit;
      appearance: textfield;
      -moz-appearance: textfield;
    }
    input::-webkit-inner-spin-button,
    input::-webkit-outer-spin-button { appearance: none; margin: 0; }
    input:focus { outline: none; }
  `,
})
export class QuantityStepper {
  readonly label = input.required<string>();
  readonly value = input.required<number>();
  readonly valueChange = output<number>();
}
