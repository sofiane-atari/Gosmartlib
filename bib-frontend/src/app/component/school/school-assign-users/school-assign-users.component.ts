import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, Info, ArrowLeft } from 'lucide-angular';

import { UserRolePromotionComponent } from '../user-role-promotion/user-role-promotion.component';
import { ToastService } from '../../../services/toast/toast.service';
import { fadeInUpAnimation } from '../../shared/animations/my-reviews.animations';

@Component({
  selector: 'app-school-assign-users',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, UserRolePromotionComponent],
  templateUrl: './school-assign-users.component.html',
  styleUrl: './school-assign-users.component.css',
  animations: [fadeInUpAnimation],
})
export class SchoolAssignUsersComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);

  protected readonly Info = Info;
  protected readonly ArrowLeft = ArrowLeft;

  protected readonly schoolId = signal<number | null>(null);
  protected readonly librariansCount = signal(0);

  protected readonly canFinish = computed(() => this.librariansCount() > 0);

  public ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = Number(idParam);

    if (!Number.isInteger(id) || id <= 0) {
      this.toastService.show('Ongeldige school.', 'error');
      void this.router.navigate(['/schools']);
      return;
    }

    this.schoolId.set(id);
  }

  protected onLibrariansCountChange(count: number): void {
    this.librariansCount.set(count);
  }

  protected navigateBack(): void {
    void this.router.navigate(['/schools']);
  }
}
