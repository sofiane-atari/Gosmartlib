import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { LucideAngularModule, Bell, ExternalLink } from 'lucide-angular';

import { AuthService } from '../../../services/authorization/auth.service';
import { AuthUserDto, Role } from '../../../model/auth.model';
import { ProfileViewModel } from '../../../model/profileView.model';
import { MailSubscriptionService } from '../../../services/mail-subscription/mail-subscription.service';
import { MailSubscriptionDTO } from '../../../model/mail-subscription/mail-subscription.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, RouterLink],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileComponent implements OnInit {
  readonly Bell = Bell;
  readonly ExternalLink = ExternalLink;

  protected readonly authService = inject(AuthService);
  private readonly mailSubscriptionService = inject(MailSubscriptionService);

  protected readonly subscriptions = signal<MailSubscriptionDTO[]>([]);
  protected readonly subscriptionsLoading = signal(false);

  ngOnInit(): void {
    this.loadSubscriptions();
  }

  private loadSubscriptions(): void {
    this.subscriptionsLoading.set(true);
    this.mailSubscriptionService.getMySubscriptions().subscribe({
      next: (data) => {
        this.subscriptions.set(data);
        this.subscriptionsLoading.set(false);
      },
      error: () => {
        this.subscriptions.set([]);
        this.subscriptionsLoading.set(false);
      },
    });
  }

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
      schoolName: profile.schoolName ?? 'Nog niet beschikbaar',
      campusName: profile.campusName ?? 'Nog niet beschikbaar',
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
