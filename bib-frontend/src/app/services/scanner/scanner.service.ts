import { Injectable, NgZone } from '@angular/core';
import { Subject } from 'rxjs';

export interface RapidTypingEvent {
  count: number;
  timestamp: number;
  characters: string;
  charArray: string[];
  lastCharacter: string;
}

@Injectable({
  providedIn: 'root'
})
export class ScannerService {

  private buffer: string[] = [];
  private bufferStartTime: number | null = null;
  private lastKeyTime: number | null = null;
  private flushTimeout: ReturnType<typeof setTimeout> | null = null;
  private threshold: number = 13;
  private windowSize: number = 1000; // 1 second window
  private idleResetMs: number = 120;
  private isListening = false;
  private keyDownHandler: ((event: KeyboardEvent) => void) | null = null;

  // This now emits the actual characters
  public rapidTypingDetected = new Subject<RapidTypingEvent>();

  constructor(private ngZone: NgZone) { }

  startListening(threshold?: number, windowSizeMs?: number, idleResetMs?: number): void {
    if (this.isListening) return;

    if (threshold) this.threshold = threshold;
    if (windowSizeMs) this.windowSize = windowSizeMs;
    if (idleResetMs) this.idleResetMs = idleResetMs;

    this.isListening = true;
    this.resetBuffer();

    this.keyDownHandler = this.handleKeyPress.bind(this);

    this.ngZone.runOutsideAngular(() => {
      window.addEventListener('keydown', this.keyDownHandler!, true);
    });
  }

  stopListening(): void {
    if (!this.isListening || !this.keyDownHandler) return;

    this.isListening = false;
    if (this.keyDownHandler) {
      window.removeEventListener('keydown', this.keyDownHandler, true);
    }
    this.keyDownHandler = null;
    this.resetBuffer();
  }

  private handleKeyPress(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      this.flushBuffer(Date.now());
      return;
    }

    const char = this.getCharacter(event);
    if (char === null) return;

    const now = Date.now();

    if (this.lastKeyTime !== null && now - this.lastKeyTime > this.idleResetMs) {
      this.resetBuffer();
    }

    if (this.bufferStartTime === null) {
      this.bufferStartTime = now;
    }

    this.buffer.push(char);
    this.lastKeyTime = now;

    const duration = now - (this.bufferStartTime ?? now);
    if (duration > this.windowSize) {
      this.resetBuffer();
      this.bufferStartTime = now;
      this.buffer.push(char);
      this.lastKeyTime = now;
    }

    this.scheduleFlush();
  }

  private flushBuffer(now: number): void {
    if (this.flushTimeout) {
      clearTimeout(this.flushTimeout);
      this.flushTimeout = null;
    }

    if (this.buffer.length < this.threshold) {
      this.resetBuffer();
      return;
    }

    const characters = [...this.buffer];
    const lastCharacter = characters[characters.length - 1] ?? '';

    this.ngZone.run(() => {
      this.rapidTypingDetected.next({
        count: characters.length,
        timestamp: now,
        characters: characters.join(''),
        charArray: characters,
        lastCharacter: lastCharacter,
      });
    });

    this.resetBuffer();
  }

  private getCharacter(event: KeyboardEvent): string | null {
    const key = event.key;

    // Only capture printable characters and space
    if (key.length === 1) {
      return key;
    }

    // Allow space as special case
    if (key === ' ') {
      return ' ';
    }

    // Ignore non-printable characters (arrows, modifiers, etc.)
    return null;
  }

  private resetBuffer(): void {
    this.buffer = [];
    this.bufferStartTime = null;
    this.lastKeyTime = null;
    if (this.flushTimeout) {
      clearTimeout(this.flushTimeout);
      this.flushTimeout = null;
    }
  }

  private scheduleFlush(): void {
    if (this.flushTimeout) {
      clearTimeout(this.flushTimeout);
    }

    this.flushTimeout = setTimeout(() => {
      this.flushBuffer(Date.now());
    }, this.idleResetMs);
  }

  ngOnDestroy(): void {
    this.stopListening();
    this.rapidTypingDetected.complete();
  }
}
