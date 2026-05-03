import { Component, computed, inject, signal } from '@angular/core';
import { AuthService } from '../../../services/authorization/auth.service';
import { LoginService } from '../../../services/login/login.service';
import { SmartschoolButtonComponent } from "../../login-profile/smartschool-button/smartschool-button.component";

@Component({
  selector: 'app-session-expired-modal',
  standalone: true,
  imports: [],
  templateUrl: './session-expired-modal.component.html',
  styleUrl: './session-expired-modal.component.css'
})
export class SessionExpiryModalComponent {
  private readonly authService = inject(AuthService);
  private readonly loginService = inject(LoginService);

  private readonly isLoadingSignal = signal(false);

  public readonly isVisible = this.authService.isSessionWarningVisible;
  public readonly secondsRemaining = this.authService.sessionSecondsRemaining;

  public readonly formattedTime = computed(() => {
    const totalSeconds = this.secondsRemaining() ?? 0;
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;

    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  });
  
  public readonly isLoading = this.authService.isSessionRefreshLoading;

  public refreshSession(): void {
    this.authService.refreshSessionFromWarning();
  }

  public logout(): void {
    this.authService.dismissSessionWarningAndLogout();
  }
}