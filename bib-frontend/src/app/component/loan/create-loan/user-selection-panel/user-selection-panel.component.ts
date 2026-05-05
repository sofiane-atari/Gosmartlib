import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthUserDto } from '../../../../model/auth.model';

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

  public readonly firstnameChanged = output<string>();
  public readonly lastnameChanged = output<string>();
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
}
