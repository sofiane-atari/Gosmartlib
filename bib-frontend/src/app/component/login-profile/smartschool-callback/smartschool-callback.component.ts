import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/authorization/auth.service';

@Component({
  selector: 'app-smartschool-callback',
  standalone: true,
  imports: [],
  templateUrl: './smartschool-callback.component.html',
  styleUrl: './smartschool-callback.component.css',
})
export class SmartschoolCallbackComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  public ngOnInit(): void {
    const token = this.resolveTokenFromUrl();

    if (!token || !this.authService.storeToken(token)) {
      this.router.navigate(['/login']);
      return;
    }

    this.authService.fetchCurrentUser();
    this.router.navigate(['/catalog']);
  }

  private resolveTokenFromUrl(): string | null {
    const queryParams = new URLSearchParams(window.location.search);
    const hashParams = new URLSearchParams(
      window.location.hash.replace(/^#/, ''),
    );

    return (
      queryParams.get('token') ??
      queryParams.get('access_token') ??
      hashParams.get('token') ??
      hashParams.get('access_token')
    );
  }
}
