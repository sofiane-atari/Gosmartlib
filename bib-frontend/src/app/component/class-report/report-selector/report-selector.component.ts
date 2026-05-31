import { Component, computed, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, Users, BarChart3 } from 'lucide-angular';
import { RoleService } from '../../../services/role-service/role.service';
import { fadeInUpAnimation } from '../../shared/animations/my-reviews.animations';

@Component({
  selector: 'app-report-selector',
  standalone: true,
  templateUrl: './report-selector.component.html',
  styleUrls: ['./report-selector.component.css'],
  imports: [CommonModule, LucideAngularModule, RouterModule],
  animations: [fadeInUpAnimation],
})
export class ReportSelectorComponent {
  readonly icons = {
    users: Users,
    system: BarChart3
  };

  private readonly roleService = inject(RoleService);

  protected readonly canViewSystemReport = computed(() =>
    this.roleService.isAdmin()
  );

  constructor(private router: Router) {}

  navigateToClassReport(): void {
    void this.router.navigate(['/reports/class']);
  }

  navigateToSystemReport(): void {
    void this.router.navigate(['/reports/system']);
  }
}





