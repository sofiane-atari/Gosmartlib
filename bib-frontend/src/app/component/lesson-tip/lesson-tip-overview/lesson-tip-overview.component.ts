import { Component, DestroyRef, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ToastService } from '../../../services/toast/toast.service';
import { LESSON_TIP_PATHS } from '../../../routes/lesson-tip.routes';

import {
  reviewCardAnimation,
  fadeInUpAnimation,
} from '../../shared/animations/my-reviews.animations';
import { LessonTipService } from '../../../services/lesson-tip/lesson-tip.service';
import { LessonTip } from '../../../model/lesson-tip/lesson-tip.model';
import { LessonTipCardComponent } from '../lesson-tip-card/lesson-tip-card.component';

@Component({
  selector: 'app-lesson-tip-overview',
  standalone: true,
  imports: [CommonModule, LessonTipCardComponent],
  animations: [reviewCardAnimation, fadeInUpAnimation],
  templateUrl: './lesson-tip-overview.component.html',
  styleUrl: './lesson-tip-overview.component.css',
})
export class LessonTipOverviewComponent {
  private readonly lessonTipService = inject(LessonTipService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly lessonTips = signal<LessonTip[]>([]);
  protected readonly isLoading = signal(false);

  public ngOnInit(): void {
    this.loadLessonTips();
  }

  public navigateToCreatePage(): void {
    void this.router.navigate([
      `/${LESSON_TIP_PATHS.root}/${LESSON_TIP_PATHS.create}`,
    ]);
  }

  public navigateToDetailPage(lessonTipId: number): void {
    void this.router.navigate([`/${LESSON_TIP_PATHS.root}/${lessonTipId}`]);
  }

  public getlessonTipTypeLabel(
    lessonTipType: LessonTip['lessonTipType'],
  ): string {
    switch (lessonTipType) {
      case 'LESSON_METHOD':
        return 'Lesmethode';
      case 'DIDACTIC_HANDBOOK':
        return 'Didactisch handboek';
      case 'EVALUATION_MATERIAL':
        return 'Evaluatiemateriaal';
      case 'REFERENCE_WORK':
        return 'Naslagwerk';
      case 'LESSON_IDEA':
        return 'Lesidee';
      case 'WORKSHEET':
        return 'Werkblad';
      case 'PRESENTATION':
        return 'Presentatie';
      case 'OTHER':
        return 'Overig';
      default:
        return 'Onbekend';
    }
  }

  private loadLessonTips(): void {
    this.isLoading.set(true);

    this.lessonTipService
      .getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (lessonTips) => {
          this.lessonTips.set(lessonTips);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
          this.toastService.show('Lestip kon niet worden geladen.', 'error');
        },
      });
  }
}
