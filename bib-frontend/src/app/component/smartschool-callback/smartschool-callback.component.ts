import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/RoleAuthorization/auth.service';

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

  ngOnInit(): void {
    const queryParams = new URLSearchParams(window.location.search);
    const token = queryParams.get('token');

    if (!token || !this.authService.storeToken(token)) {
      this.router.navigate(['/login']);
      return;
    }

    this.authService.fetchCurrentUser();
    this.router.navigate(['/catalog']);
  }
}
