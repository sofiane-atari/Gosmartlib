import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { SidebarComponent } from './component/sidebar/sidebar.component';
import { ToastComponent } from './component/toast/toast.component';
import { AuthService } from './services/RoleAuthorization/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    SidebarComponent,
    RouterOutlet,
    ToastComponent,
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  protected readonly authService = inject(AuthService);

  // Signal to track sidebar visibility on mobile
  protected isSidebarOpen = signal(false);

  cards = [
    'Catalogus',
    'Favorietenlijst',
    'Mijn Account',
    'Instellingen',
    'BookBox',
    'Rapporteer',
  ];

  toggleSidebar() {
    this.isSidebarOpen.update((v) => !v);
  }

  closeSidebar() {
    this.isSidebarOpen.set(false);
  }

  public constructor() {
    if (this.authService.isAuthenticated()) {
      this.authService.fetchCurrentUser();
    }
  }
}
