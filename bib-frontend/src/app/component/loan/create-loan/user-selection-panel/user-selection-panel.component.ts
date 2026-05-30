import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthUserDto, Role } from '../../../../model/auth.model';
import { School } from '../../../../model/school.model';

@Component({
  selector: 'app-user-selection-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-selection-panel.component.html',
  styleUrl: './user-selection-panel.component.css',
})
export class UserSelectionPanelComponent {
  public readonly isLoadingUsers = input.required<boolean>();
  public readonly users = input.required<AuthUserDto[]>();
  public readonly selectedUser = input<AuthUserDto | null>(null);
  public readonly searchFirstname = input<string>('');
  public readonly searchLastname = input<string>('');
  public readonly isExternalMode = input<boolean>(false);
  public readonly schools = input<School[]>([]);
  public readonly selectedExternalSchoolId = input<number | null>(null);
  public readonly isLoadingSchools = input<boolean>(false);

  public readonly firstnameChanged = output<string>();
  public readonly lastnameChanged = output<string>();
  public readonly externalModeChanged = output<boolean>();
  public readonly externalSchoolChanged = output<number | null>();
  public readonly userSelected = output<AuthUserDto>();
  public readonly clearSelectedUserRequested = output<void>();
  public readonly searchTriggered = output<void>();

  private readonly searchSubject = new Subject<void>();

  public constructor() {
    this.searchSubject
      .pipe(debounceTime(300), takeUntilDestroyed())
      .subscribe(() => this.searchTriggered.emit());
  }

  public onFirstnameChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.firstnameChanged.emit(target.value);
    this.searchSubject.next();
  }

  public onLastnameChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.lastnameChanged.emit(target.value);
    this.searchSubject.next();
  }

  public onExternalModeChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.externalModeChanged.emit(target.checked);
  }

  public onExternalSchoolChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    const schoolId = Number(target.value);
    this.externalSchoolChanged.emit(Number.isFinite(schoolId) && schoolId > 0 ? schoolId : null);
    this.searchSubject.next();
  }

  public getRoleLabel(role: Role | null | undefined): string {
    switch (role) {
      case 'STUDENT':
        return 'Leerling';
      case 'TEACHER':
        return 'Leerkracht';
      case 'LIBRARIAN':
        return 'Bibbeheerder';
      case 'ADMIN':
        return 'Admin';
      default:
        return 'Gebruiker';
    }
  }
}
