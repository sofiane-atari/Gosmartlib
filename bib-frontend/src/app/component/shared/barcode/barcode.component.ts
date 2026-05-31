import {
  AfterViewInit,
  Component,
  ElementRef,
  input,
  OnChanges,
  viewChild,
} from '@angular/core';
import JsBarcode from 'jsbarcode';

@Component({
  selector: 'app-barcode',
  standalone: true,
  template: `
    @if (value()) {
      <svg #barcodeSvg class="barcode-svg" role="img" [attr.aria-label]="ariaLabel()"></svg>
    }
  `,
  styles: [
    `
      :host {
        display: block;
        width: 100%;
      }

      .barcode-svg {
        display: block;
        width: 100%;
        max-width: 220px;
        height: auto;
        margin: 0 auto;
      }
    `,
  ],
})
export class BarcodeComponent implements AfterViewInit, OnChanges {
  public readonly value = input<string | null | undefined>(null);
  public readonly label = input<string>('Barcode');

  private readonly barcodeSvg = viewChild<ElementRef<SVGSVGElement>>('barcodeSvg');
  private hasViewInitialized = false;

  public ngAfterViewInit(): void {
    this.hasViewInitialized = true;
    this.renderBarcode();
  }

  public ngOnChanges(): void {
    if (!this.hasViewInitialized) {
      return;
    }

    queueMicrotask(() => this.renderBarcode());
  }

  protected ariaLabel(): string {
    return `${this.label()} ${this.value() ?? ''}`.trim();
  }

  private renderBarcode(): void {
    const barcodeElement = this.barcodeSvg()?.nativeElement;
    const barcodeValue = this.normalizeBarcodeValue(this.value());

    if (!barcodeElement || !barcodeValue) {
      return;
    }

    try {
      JsBarcode(barcodeElement, barcodeValue, {
        format: barcodeValue.length === 13 ? 'EAN13' : 'CODE128',
        displayValue: true,
        fontSize: 14,
        height: 52,
        margin: 0,
        width: 1.8,
      });
    } catch {
      JsBarcode(barcodeElement, barcodeValue, {
        format: 'CODE128',
        displayValue: true,
        fontSize: 14,
        height: 52,
        margin: 0,
        width: 1.8,
      });
    }
  }

  private normalizeBarcodeValue(value: string | null | undefined): string | null {
    const trimmedValue = value?.trim();

    if (!trimmedValue) {
      return null;
    }

    const numericValue = trimmedValue.replace(/[-\s]/g, '');
    return /^\d+$/.test(numericValue) ? numericValue : trimmedValue;
  }
}
