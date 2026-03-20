import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LoginService } from '../../services/login.service';
import { SmartschoolButtonComponent } from '../smartschool-button/smartschool-button.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, SmartschoolButtonComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  // Signal to track loading state during OAuth redirect
  private readonly isLoadingSignal = signal(false);
  public readonly isLoading = this.isLoadingSignal.asReadonly();

  private readonly loginService = inject(LoginService);

  /**
   * Initiates Smartschool OAuth2 flow
   * User will be redirected to Smartschool login, then back to app
   */
  public initiateSmartschoolSSO(): void {
    this.isLoadingSignal.set(true);
    this.loginService.initiateSmartschoolSSO();
  }
}
