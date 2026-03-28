import { Injectable } from '@angular/core';

export interface BarcodeScanKeydownResult {
  readonly preventDefault: boolean;
  readonly normalizedIsbn: string | null;
  readonly isValidIsbn: boolean;
  readonly isLikelyScanner: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class BarcodeScannerRecognizerService {
  private readonly scannerSuffixKey = 'Enter';
  private readonly scannerConfiguredPrefixes = ['SCN:', 'ISBN:'];
  private readonly scannerMinLength = 10;
  private readonly scannerMaxDurationMs = 1200;
  private readonly scannerMaxInterKeyDelayMs = 120;

  private scanStartTimestamp = 0;
  private scanLastTimestamp = 0;
  private scanKeystrokeCount = 0;

  public normalizeIsbnInput(value: string): string {
    const scannerPayload = this.extractScannerPayload(value).payload;
    return scannerPayload.replace(/[\s-]/g, '').trim();
  }

  public resetTracking(): void {
    this.scanStartTimestamp = 0;
    this.scanLastTimestamp = 0;
    this.scanKeystrokeCount = 0;
  }

  public processKeydown(event: KeyboardEvent, currentInput: string): BarcodeScanKeydownResult {
    if (event.key === this.scannerSuffixKey) {
      const rawInput = currentInput.trim();
      if (rawInput.length === 0) {
        return {
          preventDefault: true,
          normalizedIsbn: null,
          isValidIsbn: false,
          isLikelyScanner: false,
        };
      }

      const scannerPayload = this.extractScannerPayload(rawInput);
      const normalizedIsbn = this.normalizeIsbnInput(scannerPayload.payload);
      const isValidIsbn = this.isValidIsbnFormat(normalizedIsbn);
      const isLikelyScanner = scannerPayload.hasConfiguredPrefix || this.isLikelyScannerInput(normalizedIsbn);

      this.resetTracking();

      return {
        preventDefault: true,
        normalizedIsbn,
        isValidIsbn,
        isLikelyScanner,
      };
    }

    if (event.key === 'Backspace' || event.key === 'Delete') {
      this.resetTracking();
      return {
        preventDefault: false,
        normalizedIsbn: null,
        isValidIsbn: false,
        isLikelyScanner: false,
      };
    }

    if (event.key.length !== 1 || event.ctrlKey || event.altKey || event.metaKey) {
      return {
        preventDefault: false,
        normalizedIsbn: null,
        isValidIsbn: false,
        isLikelyScanner: false,
      };
    }

    const now = Date.now();
    const isFirstKeystroke = this.scanKeystrokeCount === 0;
    const isNewSequence = now - this.scanLastTimestamp > this.scannerMaxInterKeyDelayMs;

    if (isFirstKeystroke || isNewSequence) {
      this.scanStartTimestamp = now;
      this.scanKeystrokeCount = 1;
    } else {
      this.scanKeystrokeCount += 1;
    }

    this.scanLastTimestamp = now;

    return {
      preventDefault: false,
      normalizedIsbn: null,
      isValidIsbn: false,
      isLikelyScanner: false,
    };
  }

  private isLikelyScannerInput(isbn: string): boolean {
    const keyPressCount = this.scanKeystrokeCount;
    const scanDuration = this.scanLastTimestamp - this.scanStartTimestamp;

    return (
      keyPressCount >= Math.min(this.scannerMinLength, isbn.length) &&
      scanDuration > 0 &&
      scanDuration <= this.scannerMaxDurationMs
    );
  }

  private extractScannerPayload(rawInput: string): { payload: string; hasConfiguredPrefix: boolean } {
    const trimmedInput = rawInput.trim();
    const upperInput = trimmedInput.toUpperCase();

    for (const prefix of this.scannerConfiguredPrefixes) {
      if (upperInput.startsWith(prefix)) {
        return {
          payload: trimmedInput.slice(prefix.length).trim(),
          hasConfiguredPrefix: true,
        };
      }
    }

    return {
      payload: trimmedInput,
      hasConfiguredPrefix: false,
    };
  }

  private isValidIsbnFormat(isbn: string): boolean {
    const isIsbn13 = /^\d{13}$/.test(isbn);
    const isIsbn10 = /^\d{9}[\dXx]$/.test(isbn);
    return isIsbn10 || isIsbn13;
  }
}
