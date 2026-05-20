import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { LoginService } from '../../../services/login/login.service';
import { SmartschoolButtonComponent } from '../smartschool-button/smartschool-button.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, SmartschoolButtonComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent implements OnInit {
  private readonly isLoadingSignal = signal(false);
  private readonly schoolUnauthorizedSignal = signal(false);
  private readonly route = inject(ActivatedRoute);
  private readonly loginService = inject(LoginService);

  public readonly isLoading = this.isLoadingSignal.asReadonly();
  public readonly isSchoolUnauthorized = this.schoolUnauthorizedSignal.asReadonly();
  public readonly contactUrl = 'https://go-antwerpen.be/contact/';

  public ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      const error = params['error'];
      this.schoolUnauthorizedSignal.set(error === 'school_not_authorized');
    });
  }

  public initiateSmartschoolSSO(): void {
    this.isLoadingSignal.set(true);
    this.loginService.initiateSmartschoolSSO();
  }
}
