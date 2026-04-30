import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class DraftStorageService {
  private readonly prefix = 'formDraft:';

  public saveDraft<T>(key: string, value: T): void {
    sessionStorage.setItem(this.prefix + key, JSON.stringify(value));
  }

  public getDraft<T>(key: string): T | null {
    const rawValue = sessionStorage.getItem(this.prefix + key);

    if (!rawValue) {
      return null;
    }

    try {
      return JSON.parse(rawValue) as T;
    } catch {
      return null;
    }
  }

  public removeDraft(key: string): void {
    sessionStorage.removeItem(this.prefix + key);
  }
}