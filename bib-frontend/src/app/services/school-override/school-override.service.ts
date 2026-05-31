import { computed, Injectable, signal } from '@angular/core';
import { School } from '../../model/school.model';

@Injectable({
  providedIn: 'root',
})
export class SchoolOverrideService {
  private static readonly ACTIVE_SCHOOL_ID_KEY = 'activeSchoolId';
  private static readonly ACTIVE_SCHOOL_NAME_KEY = 'activeSchoolName';
  private static readonly ACTIVE_SCHOOL_EXPIRY_KEY = 'activeSchoolExpiry';
  private static readonly ORIGINAL_SCHOOL_ID_KEY = 'activeSchoolOriginalId';

  public readonly activeSchoolId = signal<number | null>(null);
  public readonly activeSchoolName = signal<string | null>(null);
  public readonly originalSchoolId = signal<number | null>(null);

  public readonly isActive = computed(() => {
    const id = this.activeSchoolId();
    const name = this.activeSchoolName();
    return id !== null && id !== undefined && !!name;
  });

  public constructor() {
    this.loadActiveSchool();
  }

  public setActiveSchool(school: School): void {
    const tokenExpiry = localStorage.getItem('authTokenExpiry');
    const expiry = tokenExpiry ? Number(tokenExpiry) : Date.now() + 60 * 60 * 1000;

    localStorage.setItem(SchoolOverrideService.ACTIVE_SCHOOL_ID_KEY, school.id.toString());
    localStorage.setItem(SchoolOverrideService.ACTIVE_SCHOOL_NAME_KEY, school.name);
    localStorage.setItem(SchoolOverrideService.ACTIVE_SCHOOL_EXPIRY_KEY, expiry.toString());

    this.activeSchoolId.set(school.id);
    this.activeSchoolName.set(school.name);
  }

  public clearActiveSchool(): void {
    localStorage.removeItem(SchoolOverrideService.ACTIVE_SCHOOL_ID_KEY);
    localStorage.removeItem(SchoolOverrideService.ACTIVE_SCHOOL_NAME_KEY);
    localStorage.removeItem(SchoolOverrideService.ACTIVE_SCHOOL_EXPIRY_KEY);
    localStorage.removeItem(SchoolOverrideService.ORIGINAL_SCHOOL_ID_KEY);

    this.activeSchoolId.set(null);
    this.activeSchoolName.set(null);
    this.originalSchoolId.set(null);
  }

  public loadActiveSchool(): void {
    this.loadOriginalSchool();

    const expiryStr = localStorage.getItem(SchoolOverrideService.ACTIVE_SCHOOL_EXPIRY_KEY);
    if (!expiryStr) {
      this.clearActiveSchool();
      return;
    }

    const expiry = Number(expiryStr);
    if (!Number.isFinite(expiry) || Date.now() >= expiry) {
      this.clearActiveSchool();
      return;
    }

    const idStr = localStorage.getItem(SchoolOverrideService.ACTIVE_SCHOOL_ID_KEY);
    const name = localStorage.getItem(SchoolOverrideService.ACTIVE_SCHOOL_NAME_KEY);

    if (!idStr || !name) {
      this.clearActiveSchool();
      return;
    }

    const id = Number(idStr);
    if (!Number.isFinite(id)) {
      this.clearActiveSchool();
      return;
    }

    this.activeSchoolId.set(id);
    this.activeSchoolName.set(name);
  }

  public captureOriginalSchool(schoolId: number): void {
    if (this.originalSchoolId() !== null) {
      return;
    }
    localStorage.setItem(SchoolOverrideService.ORIGINAL_SCHOOL_ID_KEY, schoolId.toString());
    this.originalSchoolId.set(schoolId);
  }

  private loadOriginalSchool(): void {
    const idStr = localStorage.getItem(SchoolOverrideService.ORIGINAL_SCHOOL_ID_KEY);
    if (!idStr) {
      return;
    }
    const id = Number(idStr);
    if (Number.isFinite(id)) {
      this.originalSchoolId.set(id);
    }
  }
}
