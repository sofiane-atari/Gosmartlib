import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class SelectedSchoolService {
  private readonly STORAGE_KEY = 'selectedSchool';
  private readonly selectedSchoolId = signal<number | null>(null);
  private readonly selectedSchoolName = signal<string>('');

  public readonly schoolId = this.selectedSchoolId.asReadonly();
  public readonly schoolName = this.selectedSchoolName.asReadonly();

  constructor() {
    const saved = localStorage.getItem(this.STORAGE_KEY);
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        this.selectedSchoolId.set(parsed.id ?? null);
        this.selectedSchoolName.set(parsed.name ?? '');
      } catch {
        localStorage.removeItem(this.STORAGE_KEY);
      }
    }
  }

  public selectSchool(id: number | null, name: string): void {
    this.selectedSchoolId.set(id);
    this.selectedSchoolName.set(name);
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify({ id, name }));
  }

  public resetToMySchool(): void {
    this.selectedSchoolId.set(null);
    this.selectedSchoolName.set('');
    localStorage.removeItem(this.STORAGE_KEY);
  }
}
