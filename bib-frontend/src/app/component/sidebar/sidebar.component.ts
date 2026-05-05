import { Component, computed, inject, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/authorization/auth.service';
import { FormsModule } from '@angular/forms';
import { AuthUserDto } from '../../model/auth.model';
import { LESSON_TIP_PATHS } from '../../routes/lesson-tip.routes';
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
  School,
  Shield,
  FolderKanban,
  LibraryBig,
} from 'lucide-angular';
import { Router, RouterModule } from '@angular/router';
import { RoleService } from '../../services/role-service/role.service';
import { UserService } from '../../services/user/user.service';
import { SidebarActiveLoansComponent } from './sidebar-active-loans/sidebar-active-loans.component';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    CommonModule,
    LucideAngularModule,
    RouterModule,
    FormsModule,
    SidebarActiveLoansComponent,
  ],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css',
})
export class SidebarComponent {
  readonly PlusIcon = Plus;
  readonly UserIcon = User;
  readonly RotateCcwIcon = RotateCcw;
  readonly LogOutIcon = LogOut;
  readonly ShieldIcon = Shield;
  public readonly FolderKanbanIcon = FolderKanban;

  public readonly authService = inject(AuthService);
  public readonly roleService = inject(RoleService);
  private readonly userService = inject(UserService);

  public mobileOpen = input<boolean>(false);
  public sidebarClosed = output<void>();

  public constructor(private readonly router: Router) {}

  protected readonly currentUser = computed<AuthUserDto | null>(() =>
    this.authService.currentUser(),
  );

  protected readonly isAdminOverrideVisible = computed(
    () => this.authService.isAdminOverrideFeatureEnabled && !!this.currentUser(),
  );

  protected readonly isAdminOverrideEnabled = computed(
    () => this.authService.adminOverrideStatus()?.enabled ?? false,
  );

  protected readonly isStaff = computed(() => {
    const role = this.currentUser()?.role;
    return role === 'TEACHER' || role === 'LIBRARIAN' || role === 'ADMIN';
  });

  protected readonly adminOverrideLabel = computed(() =>
    this.isAdminOverrideEnabled() ? 'Admin uit (test)' : 'Admin aan (test)',
  );

  protected readonly userDisplayName = computed(() => {
    const user = this.currentUser();

    if (!user) {
      return 'Gebruiker';
    }

    const fullName = user.fullName?.trim();
    if (fullName) {
      return fullName;
    }

    const firstName = user.firstName?.trim();
    const lastName = user.lastName?.trim();

    const combinedName = [firstName, lastName].filter(Boolean).join(' ').trim();
    if (combinedName) {
      return combinedName;
    }

    const email = user.email?.trim();
    if (email) {
      return email;
    }

    return this.userService.getRoleLabel(user.role);
  });

  protected readonly userRoleLabel = computed(() =>
    this.userService.getRoleLabel(this.currentUser()?.role),
  );

  menuItems = [
    { icon: Book, label: 'Catalogus', route: '/catalog' },
    { icon: Heart, label: 'Favorietenlijst', route: '/reading-list' },
    { icon: History, label: 'Geschiedenis', route: '/history' },
    { icon: Star, label: 'Mijn reviews', route: '/reviews' },
    { icon: UserCircle, label: 'Mijn account', route: '/profile' },
    { icon: Settings, label: 'Instellingen', route: '/settings' },
    { icon: Package, label: 'BookBox', route: '/bookbox', staffOnly: true },
    {
      icon: FileText,
      label: 'Klasrapportages',
      route: '/class-report',
      staffOnly: true,
    },
    {
      icon: FolderKanban,
      label: 'Lestips',
      route: `/${LESSON_TIP_PATHS.root}`,
      staffOnly: true,
    },
    {
      icon: Flag,
      label: 'Reviewbeheer',
      route: '/review-dashboard',
      staffOnly: true,
    },
    {
      icon: LibraryBig,
      label: 'Genrebeheer',
      route: '/genre',
      adminOnly: true,
    },
    { icon: Tag, label: 'Labelbeheer', route: '/tag', adminOnly: true },
    { icon: School, label: 'Schoolbeheer', route: '/schools', adminOnly: true },
  ];

  protected readonly visibleMenuItems = computed(() => {
    const isAdmin = this.roleService.isAdmin();
    const canManageLessonTip = this.roleService.canManageLessonTip();

    return this.menuItems.filter((item) => {
      if (item.adminOnly && !isAdmin) {
        return false;
      }

      if (item.staffOnly && !canManageLessonTip) {
        return false;
      }

      return true;
    });
  });

  public goToAddBook(): void {
    this.router.navigate(['/add-book']);
    this.closeOnMobile();
  }

  public goToBookManagement(): void {
    this.router.navigate(['/books-management']);
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

  public toggleAdminOverride(): void {
    const enable = !this.isAdminOverrideEnabled();
    this.authService.setAdminOverride(enable);
  }
}
