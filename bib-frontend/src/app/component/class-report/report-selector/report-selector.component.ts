import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, Users, Building2 } from 'lucide-angular';

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
    building: Building2
  };

  constructor(private router: Router) {
    console.log('ReportSelectorComponent initialized');
  }

  navigateToClassReport(): void {
    console.log(' navigateToClassReport() called');
    try {
      const result = this.router.navigate(['/reports/class']);
      console.log('Navigate result:', result);
      result.then(success => {
        console.log('Navigation successful:', success);
      }).catch(error => {
        console.error('Navigation error:', error);
      });
    } catch (error) {
      console.error('Exception during navigation:', error);
    }
  }

  navigateToCampusReport(): void {
    console.log('navigateToCampusReport() called');
    try {
      const result = this.router.navigate(['/reports/school']);
      console.log('Navigate result:', result);
      result.then(success => {
        console.log('Navigation successful:', success);
      }).catch(error => {
        console.error('Navigation error:', error);
      });
    } catch (error) {
      console.error('Exception during navigation:', error);
    }
  }
}





