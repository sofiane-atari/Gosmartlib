import { CommonModule } from '@angular/common';
import { Component, DestroyRef, ElementRef, inject, output, signal, viewChild } from '@angular/core';
import { ScannerService } from '../../../../services/scanner/scanner.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-scan-isbn',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './scan-isbn.component.html',
  styleUrl: './scan-isbn.component.css',
})
export class ScanIsbnComponent {
  private readonly scanCard = viewChild.required<ElementRef<HTMLElement>>('scanCard');
  private readonly scannerService = inject(ScannerService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly focusTrap = viewChild.required<ElementRef<HTMLInputElement>>('focusTrap');
  private focusLockInterval: ReturnType<typeof setInterval> | null = null;

  public readonly scannedIsbn = output<string>();
  public readonly isArmed = signal(false);
  public readonly lastScan = signal('');

  public armScanner(): void {
    if (this.isArmed()) {
      return;
    }

    this.isArmed.set(true);
    this.lastScan.set('');
    this.scanCard().nativeElement.focus();
    this.startFocusLock();
    this.scannerService.startListening(10, 1000);

    this.scannerService.rapidTypingDetected
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((event) => {
        const value = event.characters.trim();
        if (!value) {
          return;
        }

        this.lastScan.set(value);
        console.log('[Scan] Gescand ISBN:', value);
        this.scannedIsbn.emit(value);
      });
  }

  public disarmScanner(): void {
    if (!this.isArmed()) {
      return;
    }

    this.isArmed.set(false);
    this.stopFocusLock();
    this.scannerService.stopListening();
  }

  public onFocusTrapBlur(): void {
    if (!this.isArmed()) {
      return;
    }

    this.focusTrap().nativeElement.focus();
  }

  public ngOnDestroy(): void {
    this.stopFocusLock();
    if (this.isArmed()) {
      this.isArmed.set(false);
      this.scannerService.stopListening();
    }
  }

  private startFocusLock(): void {
    this.focusTrap().nativeElement.focus();
    this.focusLockInterval = setInterval(() => {
      if (!this.isArmed()) {
        return;
      }

      if (document.activeElement !== this.focusTrap().nativeElement) {
        this.focusTrap().nativeElement.focus();
      }
    }, 250);
  }

  private stopFocusLock(): void {
    if (this.focusLockInterval) {
      clearInterval(this.focusLockInterval);
      this.focusLockInterval = null;
    }
  }
}
