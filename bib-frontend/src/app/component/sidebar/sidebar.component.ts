import { Component, computed, inject, input, output, signal } from '@angular/core';
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
  Trophy,
  UserCog,
} from 'lucide-angular';
import { Router, RouterModule } from '@angular/router';
import { RoleService } from '../../services/role-service/role.service';
import { UserService } from '../../services/user/user.service';
import { SchoolService } from '../../services/school/school.service';
import { SchoolOverrideService, ClassOption } from '../../services/school-override/school-override.service';
import { SidebarActiveLoansComponent } from './sidebar-active-loans/sidebar-active-loans.component';
import { DevFeatureWrapperComponent } from '../shared/dev-feature-wrapper/dev-feature-wrapper.component';
import { School as SchoolModel } from '../../model/school.model';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    CommonModule,
    LucideAngularModule,
    RouterModule,
    FormsModule,
    SidebarActiveLoansComponent,
    DevFeatureWrapperComponent,
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
  readonly SchoolSwitchIcon = School;
  public readonly FolderKanbanIcon = FolderKanban;

  public readonly authService = inject(AuthService);
  public readonly roleService = inject(RoleService);
  private readonly userService = inject(UserService);
  private readonly schoolService = inject(SchoolService);
  public readonly schoolOverrideService = inject(SchoolOverrideService);

  public mobileOpen = input<boolean>(false);
  public sidebarClosed = output<void>();

  protected readonly devModeEnabled = signal(false);
  protected readonly isSchoolSwitchModalOpen = signal(false);
  protected readonly modalStep = signal<'school' | 'class'>('school');
  protected readonly selectedSchoolForClasses = signal<SchoolModel | null>(null);
  protected readonly schoolsForSwitch = signal<SchoolModel[]>([]);
  protected readonly classesForSwitch = signal<ClassOption[]>([]);
  protected readonly isLoadingSchools = signal(false);
  protected readonly isLoadingClasses = signal(false);
  protected readonly schoolSwitchError = signal<string | null>(null);
  protected readonly classSwitchError = signal<string | null>(null);

  protected readonly isSchoolOverrideActive = computed(
    () => this.schoolOverrideService.isActive(),
  );

  protected readonly isClassOverrideActive = computed(
    () => this.schoolOverrideService.isClassOverrideActive(),
  );

  protected readonly activeSchoolDisplayName = computed(() => {
    const name = this.schoolOverrideService.activeSchoolName();
    return name ?? 'Onbekende school';
  });

  protected readonly activeClassDisplayName = computed(() => {
    const names = this.schoolOverrideService.activeClassNames();
    const count = names.length;
    if (count === 0) {
      return null;
    }
    if (count === 1) {
      return names[0];
    }
    if (count === 2) {
      return `${names[0]} en ${names[1]}`;
    }
    return `${names[0]} en ${count - 1} andere`;
  });

  protected readonly canSwitchClass = computed(
    () => this.roleService.isAdmin() || this.roleService.isLibrarian(),
  );

  protected readonly switchButtonLabel = computed(() =>
    this.roleService.isAdmin() && !this.roleService.isLibrarian() ? 'School wisselen' : 'Klas wisselen'
  );

  public constructor(private readonly router: Router) {
    this.schoolService.isDevModeEnabled().subscribe({
      next: (enabled) => this.devModeEnabled.set(enabled),
      error: () => this.devModeEnabled.set(false),
    });
  }

  protected readonly currentUser = computed<AuthUserDto | null>(() =>
    this.authService.currentUser(),
  );

  protected readonly isAdminOverrideVisible = computed(
    () => this.devModeEnabled() && !!this.currentUser(),
  );

  protected readonly isAdminOverrideEnabled = computed(
    () => this.authService.adminOverrideStatus()?.enabled ?? false,
  );

  protected readonly canManageBooksAndLoans = computed(() => {
    return this.roleService.canManageBooks();
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
    { icon: Trophy, label: 'Prestaties', route: '/achievements' },
    { icon: UserCircle, label: 'Mijn account', route: '/profile' },
    { icon: Settings, label: 'Instellingen', route: '/settings' },
    {
      icon: FileText,
      label: 'Rapportages',
      route: '/reports',
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
      librarianOrAdmin: true,
    },
    {
      icon: LibraryBig,
      label: 'Genrebeheer',
      route: '/genre',
      adminOnly: true,
    },
    { icon: Tag, label: 'Tagbeheer', route: '/tags', librarianOrAdmin: true },
    { icon: School, label: 'Schoolbeheer', route: '/schools', adminOnly: true },
    {
      icon: UserCog,
      label: 'Gebruikersbeheer',
      route: '/user-management',
      librarianOrAdmin: true,
    },
  ];

  protected readonly visibleMenuItems = computed(() => {
    const isAdmin = this.roleService.isAdmin();
    const canManageLessonTip = this.roleService.canManageLessonTip();
    const canManageGenres = this.roleService.canManageGenres();

    return this.menuItems.filter((item) => {
      if (item.adminOnly && !isAdmin) {
        return false;
      }

      if (item.staffOnly && !canManageLessonTip) {
        return false;
      }

      if ((item as { librarianOrAdmin?: boolean }).librarianOrAdmin && !canManageGenres) {
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

  public openSchoolSwitchModal(): void {
    this.schoolSwitchError.set(null);
    this.classSwitchError.set(null);
    this.isSchoolSwitchModalOpen.set(true);

    const isAdminOnly = this.roleService.isAdmin() && !this.roleService.isLibrarian();

    if (!isAdminOnly) {
      const userSchool = this.authService.currentUser()?.school;

      if (!userSchool) {
        this.classSwitchError.set('Kon eigen school niet laden.');
        return;
      }

      this.isLoadingClasses.set(true);
      this.modalStep.set('class');

      const ownSchool: SchoolModel = {
        id: userSchool.id,
        name: userSchool.name,
        domainName: userSchool.domainName,
      };

      this.schoolOverrideService.captureOriginalSchool(userSchool.id);
      this.selectedSchoolForClasses.set(ownSchool);

      this.schoolService.getOwnSchoolClasses().subscribe({
        next: (classes) => {
          this.classesForSwitch.set(classes);
          this.isLoadingClasses.set(false);
        },
        error: () => {
          this.classSwitchError.set('De klassenlijst kon niet worden opgehaald. Probeer het opnieuw.');
          this.isLoadingClasses.set(false);
        },
      });
      return;
    }

    this.isLoadingSchools.set(true);
    this.modalStep.set('school');
    this.selectedSchoolForClasses.set(null);

    this.schoolService.getAll().subscribe({
      next: (schools) => {
        const activeSchools = schools.filter((s) => s.status === 'ACTIVE');
        this.schoolsForSwitch.set(activeSchools);
        this.isLoadingSchools.set(false);
      },
      error: () => {
        this.schoolSwitchError.set('De lijst met scholen kon niet worden opgehaald. Probeer het opnieuw.');
        this.isLoadingSchools.set(false);
      },
    });
  }

  public onSchoolSelected(school: SchoolModel): void {
    const currentUserId = this.authService.currentUser()?.schoolId;
    if (currentUserId) {
      this.schoolOverrideService.captureOriginalSchool(currentUserId);
    }

    this.selectedSchoolForClasses.set(school);
    this.classSwitchError.set(null);
    this.isLoadingClasses.set(true);
    this.modalStep.set('class');

    this.schoolService.getClasses(school.id).subscribe({
      next: (classes) => {
        this.classesForSwitch.set(classes);
        this.isLoadingClasses.set(false);
      },
      error: () => {
        this.classSwitchError.set('De klassenlijst kon niet worden opgehaald. Probeer het opnieuw.');
        this.isLoadingClasses.set(false);
      },
    });
  }

  public onClassesConfirmed(options: ClassOption[]): void {
    const school = this.selectedSchoolForClasses();
    if (!school) return;

    this.schoolOverrideService.setActiveSchool(school);
    this.schoolOverrideService.setActiveClasses(options);
    this.isSchoolSwitchModalOpen.set(false);
    window.location.reload();
  }

  public onBackToSchools(): void {
    if (this.roleService.isAdmin() && !this.roleService.isLibrarian()) {
      this.modalStep.set('school');
    } else {
      this.isSchoolSwitchModalOpen.set(false);
    }
  }

  public resetSchoolOverride(): void {
    this.schoolOverrideService.clearActiveSchool();
    window.location.reload();
  }

  public closeSchoolSwitchModal(): void {
    this.isSchoolSwitchModalOpen.set(false);
  }
}
