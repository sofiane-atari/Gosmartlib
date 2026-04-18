import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, School as SchoolIcon, Building } from 'lucide-angular';
import { School } from '../../../model/school.model';
import { SchoolService } from '../../../services/school/school.service';

@Component({
  selector: 'app-school-dashboard',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './school-dashboard.component.html',
  styleUrl: './school-dashboard.component.css',
})
export class SchoolDashboardComponent {
  private readonly schoolService = inject(SchoolService);

  protected readonly SchoolIcon = SchoolIcon;
  protected readonly Building = Building;

  protected readonly schools = signal<School[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly totalSchools = computed(() => this.schools().length);

  public ngOnInit(): void {
    this.loadSchools();
  }

  private loadSchools(): void {
    this.isLoading.set(true);

    this.schoolService.getAll().subscribe({
      next: (schools) => {
        this.schools.set([...schools].sort((a, b) => a.name.localeCompare(b.name)));
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Scholen konden niet geladen worden.');
        this.isLoading.set(false);
      },
    });
  }
}
