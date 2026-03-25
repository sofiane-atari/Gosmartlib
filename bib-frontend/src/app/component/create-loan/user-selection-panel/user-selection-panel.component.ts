import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { AuthUserDto } from '../../../model/auth.model';

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
  public readonly searchFirstname = input.required<string>();
  public readonly searchLastname = input.required<string>();
  public readonly searchId = input.required<string>();

  public readonly firstnameSearchChanged = output<Event>();
  public readonly lastnameSearchChanged = output<Event>();
  public readonly searchIdChanged = output<Event>();
  public readonly searchRequested = output<void>();
  public readonly userSelected = output<AuthUserDto>();
  public readonly clearSelectedUserRequested = output<void>();
}
