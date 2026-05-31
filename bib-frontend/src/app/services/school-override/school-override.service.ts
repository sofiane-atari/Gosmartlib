import { computed, Injectable, signal } from '@angular/core';
import { School } from '../../model/school.model';

export interface ClassOption {
  sourcedId: string;
  title: string;
  classCode: string;
}

@Injectable({
  providedIn: 'root',
})
export class SchoolOverrideService {
  private static readonly ACTIVE_SCHOOL_ID_KEY = 'activeSchoolId';
  private static readonly ACTIVE_SCHOOL_NAME_KEY = 'activeSchoolName';
  private static readonly ACTIVE_SCHOOL_EXPIRY_KEY = 'activeSchoolExpiry';
  private static readonly ORIGINAL_SCHOOL_ID_KEY = 'activeSchoolOriginalId';
  private static readonly ACTIVE_CLASS_CODES_KEY = 'activeClassCodes';
  private static readonly ACTIVE_CLASS_NAMES_KEY = 'activeClassNames';
  private static readonly ACTIVE_PRIMARY_GROUPS_KEY = 'activePrimaryGroupNames';

  public readonly activeSchoolId = signal<number | null>(null);
  public readonly activeSchoolName = signal<string | null>(null);
  public readonly originalSchoolId = signal<number | null>(null);
  public readonly activeClassCodes = signal<string[]>([]);
  public readonly activeClassNames = signal<string[]>([]);
  public readonly activePrimaryGroupNames = signal<string[]>([]);

  public readonly isActive = computed(() => {
    const id = this.activeSchoolId();
    const name = this.activeSchoolName();
    return id !== null && id !== undefined && !!name;
  });

  public readonly isClassOverrideActive = computed(() => {
    return this.activeClassCodes().length > 0;
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

  public setActiveClasses(options: ClassOption[]): void {
    const tokenExpiry = localStorage.getItem('authTokenExpiry');
    const expiry = tokenExpiry ? Number(tokenExpiry) : Date.now() + 60 * 60 * 1000;

    const codes = options.map((o) => o.classCode);
    const names = options.map((o) => o.title);

    localStorage.setItem(SchoolOverrideService.ACTIVE_CLASS_CODES_KEY, JSON.stringify(codes));
    localStorage.setItem(SchoolOverrideService.ACTIVE_CLASS_NAMES_KEY, JSON.stringify(names));
    localStorage.setItem(SchoolOverrideService.ACTIVE_PRIMARY_GROUPS_KEY, JSON.stringify(names));
    localStorage.setItem(SchoolOverrideService.ACTIVE_SCHOOL_EXPIRY_KEY, expiry.toString());

    this.activeClassCodes.set(codes);
    this.activeClassNames.set(names);
    this.activePrimaryGroupNames.set(names);
  }

  public clearActiveSchool(): void {
    localStorage.removeItem(SchoolOverrideService.ACTIVE_SCHOOL_ID_KEY);
    localStorage.removeItem(SchoolOverrideService.ACTIVE_SCHOOL_NAME_KEY);
    localStorage.removeItem(SchoolOverrideService.ACTIVE_SCHOOL_EXPIRY_KEY);
    localStorage.removeItem(SchoolOverrideService.ORIGINAL_SCHOOL_ID_KEY);
    localStorage.removeItem(SchoolOverrideService.ACTIVE_CLASS_CODES_KEY);
    localStorage.removeItem(SchoolOverrideService.ACTIVE_CLASS_NAMES_KEY);
    localStorage.removeItem(SchoolOverrideService.ACTIVE_PRIMARY_GROUPS_KEY);

    this.activeSchoolId.set(null);
    this.activeSchoolName.set(null);
    this.originalSchoolId.set(null);
    this.activeClassCodes.set([]);
    this.activeClassNames.set([]);
    this.activePrimaryGroupNames.set([]);
  }

  public loadActiveSchool(): void {
    this.loadOriginalSchool();
    this.loadClassOverrides();

    const expiryStr = localStorage.getItem(SchoolOverrideService.ACTIVE_SCHOOL_EXPIRY_KEY);
    if (!expiryStr) {
      this.clearAllOverrides();
      return;
    }

    const expiry = Number(expiryStr);
    if (!Number.isFinite(expiry) || Date.now() >= expiry) {
      this.clearAllOverrides();
      return;
    }

    const idStr = localStorage.getItem(SchoolOverrideService.ACTIVE_SCHOOL_ID_KEY);
    const name = localStorage.getItem(SchoolOverrideService.ACTIVE_SCHOOL_NAME_KEY);

    if (!idStr || !name) {
      this.clearAllOverrides();
      return;
    }

    const id = Number(idStr);
    if (!Number.isFinite(id)) {
      this.clearAllOverrides();
      return;
    }

    this.activeSchoolId.set(id);
    this.activeSchoolName.set(name);
  }

  private clearAllOverrides(): void {
    localStorage.removeItem(SchoolOverrideService.ACTIVE_SCHOOL_ID_KEY);
    localStorage.removeItem(SchoolOverrideService.ACTIVE_SCHOOL_NAME_KEY);
    localStorage.removeItem(SchoolOverrideService.ACTIVE_SCHOOL_EXPIRY_KEY);
    localStorage.removeItem(SchoolOverrideService.ORIGINAL_SCHOOL_ID_KEY);
    localStorage.removeItem(SchoolOverrideService.ACTIVE_CLASS_CODES_KEY);
    localStorage.removeItem(SchoolOverrideService.ACTIVE_CLASS_NAMES_KEY);
    localStorage.removeItem(SchoolOverrideService.ACTIVE_PRIMARY_GROUPS_KEY);

    this.activeSchoolId.set(null);
    this.activeSchoolName.set(null);
    this.originalSchoolId.set(null);
    this.activeClassCodes.set([]);
    this.activeClassNames.set([]);
    this.activePrimaryGroupNames.set([]);
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

  private loadClassOverrides(): void {
    const codesJson = localStorage.getItem(SchoolOverrideService.ACTIVE_CLASS_CODES_KEY);
    const namesJson = localStorage.getItem(SchoolOverrideService.ACTIVE_CLASS_NAMES_KEY);
    const groupsJson = localStorage.getItem(SchoolOverrideService.ACTIVE_PRIMARY_GROUPS_KEY);

    const codes = this.parseStringList(codesJson);
    const names = this.parseStringList(namesJson);
    const groups = this.parseStringList(groupsJson);

    if (codes.length > 0) {
      this.activeClassCodes.set(codes);
    }
    if (names.length > 0) {
      this.activeClassNames.set(names);
    }
    if (groups.length > 0) {
      this.activePrimaryGroupNames.set(groups);
    }
  }

  private parseStringList(raw: string | null): string[] {
    if (!raw) {
      return [];
    }
    try {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        return parsed.filter((item): item is string => typeof item === 'string');
      }
    } catch {
      // backward compatibility: plain string stored before refactor
      if (typeof raw === 'string' && raw.length > 0) {
        return [raw];
      }
    }
    return [];
  }
}
