import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';

import { AuthService } from '../../services/RoleAuthorization/auth.service';
import { ToastService } from '../../services/Toast/toast.service';
import { AuthUserDto, Role } from '../../model/auth.model';

interface ProfileStat {
  label: string;
  value: number;
  variant: 'borrowed' | 'active' | 'favorite' | 'review';
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileComponent implements OnInit {
  protected readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);

  protected readonly profile = computed<AuthUserDto | null>(() =>
    this.authService.currentUser(),
  );

  protected readonly stats = computed<ProfileStat[]>(() => [
    {
      label: 'Totaal Uitgeleend',
      value: 47,
      variant: 'borrowed',
    },
    {
      label: 'Nu Uitgeleend',
      value: 2,
      variant: 'active',
    },
    {
      label: 'Favorieten',
      value: 2,
      variant: 'favorite',
    },
    {
      label: 'Reviews Geschreven',
      value: 12,
      variant: 'review',
    },
  ]);

  public ngOnInit(): void {
    if (!this.authService.currentUser()) {
      this.loadProfile();
    }
  }

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

  protected getInitials(fullName: string | undefined): string {
    if (!fullName?.trim()) {
      return '--';
    }

    const parts: string[] = fullName.trim().split(/\s+/);
    const firstInitial: string = parts[0]?.charAt(0).toUpperCase() ?? '';
    const lastInitial: string =
      parts.length > 1
        ? parts[parts.length - 1].charAt(0).toUpperCase()
        : '';

    return `${firstInitial}${lastInitial}`;
  }

  protected getFirstName(fullName: string | undefined): string {
    if (!fullName?.trim()) {
      return '-';
    }

    return fullName.trim().split(/\s+/)[0] ?? '-';
  }

  protected getLastName(fullName: string | undefined): string {
    if (!fullName?.trim()) {
      return '-';
    }

    const parts: string[] = fullName.trim().split(/\s+/);

    if (parts.length < 2) {
      return '-';
    }

    return parts.slice(1).join(' ');
  }

  private loadProfile(): void {
    this.authService.fetchCurrentUser(() => {
      this.toastService.show('Het profiel kon niet worden geladen.', 'error');
    });
  }
}