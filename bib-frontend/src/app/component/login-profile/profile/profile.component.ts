import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';

import { AuthService } from '../../../services/authorization/auth.service';
import { AuthUserDto, Role } from '../../../model/auth.model';
import { ProfileViewModel } from '../../../model/profileView.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileComponent {
  protected readonly authService = inject(AuthService);

  protected readonly profile = computed<AuthUserDto | null>(() =>
    this.authService.currentUser(),
  );

  protected readonly profileVm = computed<ProfileViewModel | null>(() => {
    const profile = this.profile();
    if (!profile) {
      return null;
    }

    return {
      firstName: profile.firstName ?? '-',
      lastName: profile.lastName ?? '-',
      fullName: profile.fullName ?? '-',
      roleLabel: this.getRoleLabel(profile.role),
      email: profile.email ?? 'Nog niet beschikbaar',
      smartschoolId: profile.smartschoolId ?? profile.id,
      platform: profile.platform ?? 'Nog niet beschikbaar',
      primaryGroupName: profile.primaryGroupName ?? 'Nog niet beschikbaar',
      parentGroupNames: profile.parentGroupNames ?? [],
    };
  });

  protected getRoleLabel(role: Role | undefined): string {
    switch (role) {
      case 'STUDENT':
        return 'Leerling';
      case 'TEACHER':
        return 'Leerkracht';
      case 'LIBRARIAN':
        return 'Bibliotheekbeheerder';
      case 'ADMIN':
        return 'Platformbeheerder';
      default:
        return 'Onbekende rol';
    }
  }
}
