import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { LucideAngularModule, X, RefreshCw, ShieldAlert } from 'lucide-angular';
import { AuthUserDto } from '../../../model/auth.model';
import { UserService } from '../../../services/user/user.service';

@Component({
  selector: 'app-review-blocked-list',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './review-blocked-list.component.html',
  styleUrl: './review-blocked-list.component.css',
})
export class ReviewBlockedListComponent {
  protected readonly CloseIcon = X;
  protected readonly RefreshIcon = RefreshCw;
  protected readonly ShieldAlertIcon = ShieldAlert;

  public readonly isVisible = input<boolean>(false);
  public readonly blockedUsers = input<AuthUserDto[]>([]);
  public readonly isLoading = input<boolean>(false);
  public readonly errorMessage = input<string | null>(null);

  public readonly unblockUser = output<string>();
  public readonly close = output<void>();
  public readonly retry = output<void>();

  public constructor(protected readonly userService: UserService) {}

  protected onUnblock(userId: string): void {
    this.unblockUser.emit(userId);
  }

  protected onClose(): void {
    this.close.emit();
  }

  protected onRetry(): void {
    this.retry.emit();
  }

  protected getDisplayName(user: AuthUserDto): string {
    return this.userService.getDisplayName(user);
  }
}
