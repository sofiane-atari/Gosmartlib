import { Component, computed, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, Users, Building2, BarChart3 } from 'lucide-angular';
import { RoleService } from '../../../services/role-service/role.service';

@Component({
  selector: 'app-report-selector',
  standalone: true,
  templateUrl: './report-selector.component.html',
  styleUrls: ['./report-selector.component.css'],
  imports: [CommonModule, LucideAngularModule, RouterModule]
})
export class ReportSelectorComponent {
  readonly icons = {
    users: Users,
    building: Building2,
    system: BarChart3
  };

  private readonly roleService = inject(RoleService);

  protected readonly canViewSchoolReport = computed(() =>
    this.roleService.isLibrarian() || this.roleService.isAdmin()
  );

  protected readonly canViewSystemReport = computed(() =>
    this.roleService.isAdmin()
  );

  constructor(private router: Router) {}

  navigateToClassReport(): void {
    void this.router.navigate(['/reports/class']);
  }

  navigateToCampusReport(): void {
    void this.router.navigate(['/reports/school']);
  }

  navigateToSystemReport(): void {
    void this.router.navigate(['/reports/system']);
  }
}





