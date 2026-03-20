import { Component, inject, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/RoleAuthorization/auth.service';
import { FormsModule } from '@angular/forms';
import { Role } from '../../model/auth.model';
import {
  LucideAngularModule,
  Book,
  Heart,
  History,
  Star,
  UserCircle,
  Settings,
  Package,
  Flag,
  FileText,
  Tag,
  Plus,
  RotateCcw,
  LogOut,
  User,
} from 'lucide-angular';
import { School } from 'lucide-angular/src/icons';
import { Router, RouterModule } from '@angular/router';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, RouterModule, FormsModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css',
})
export class SidebarComponent {
  readonly PlusIcon = Plus;
  readonly UserIcon = User;
  readonly RotateCcwIcon = RotateCcw;
  readonly LogOutIcon = LogOut;

  public authService = inject(AuthService);

  // Input for mobile state
  public mobileOpen = input<boolean>(false);

  // Output for closing sidebar on mobile
  public sidebarClosed = output<void>();

  public constructor(private readonly router: Router) {}

  public onRoleChange(newRole: string): void {
    this.authService.switchRole(newRole as Role);
  }

  menuItems = [
    { icon: Book, label: 'Catalogus', route: '/catalog' },
    { icon: Heart, label: 'Favorietenlijst', route: '/reading-list' },
    { icon: History, label: 'Geschiedenis', route: '/historiek' },
    { icon: Star, label: 'Mijn Reviews', route: '/reviews' },
    { icon: UserCircle, label: 'Mijn Account', route: '/account' },
    { icon: Settings, label: 'Instellingen', route: '/instellingen' },
    { icon: Package, label: 'BookBox', route: '/bookbox' },
    { icon: Flag, label: 'Rapporteer', route: '/gerapporteerd' },
    { icon: FileText, label: 'Klasrapportages', route: '/klasraportage' },
    { icon: Tag, label: 'Labelbeheer', route: '/labels' },
    { icon: School, label: 'Schoolbeheer', route: '/scholen' },
  ];

  public goToAddBook(): void {
    this.router.navigate(['/add-book']);
    this.closeOnMobile();
  }

  public logout(): void {
    this.authService.clearToken();
    this.closeOnMobile();
    this.router.navigate(['/login']);
  }

  public closeOnMobile(): void {
    if (this.mobileOpen()) {
      this.sidebarClosed.emit();
    }
  }
}
