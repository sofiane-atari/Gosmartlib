import { Component, DestroyRef, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
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
export class LessonTipOverviewComponent implements OnInit {
  private readonly lessonTipService = inject(LessonTipService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly lessonTips = signal<LessonTip[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly filteredBookId = signal<number | null>(null);

  public ngOnInit(): void {
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const bookIdParam = params.get('bookId');
        const parsedBookId = bookIdParam ? Number(bookIdParam) : null;

        if (
          parsedBookId !== null &&
          (!Number.isInteger(parsedBookId) || parsedBookId <= 0)
        ) {
          this.filteredBookId.set(null);
          this.toastService.show('Ongeldig boekfilter.', 'error');
          this.loadLessonTips();
          return;
        }

        this.filteredBookId.set(parsedBookId);
        this.loadLessonTips(parsedBookId ?? undefined);
      });
  }

  public navigateToCreatePage(): void {
    void this.router.navigate([
      `/${LESSON_TIP_PATHS.root}/${LESSON_TIP_PATHS.create}`,
    ]);
  }

  public navigateToDetailPage(lessonTipId: number): void {
    void this.router.navigate([`/${LESSON_TIP_PATHS.root}/${lessonTipId}`]);
  }

  public clearBookFilter(): void {
    void this.router.navigate([`/${LESSON_TIP_PATHS.root}`]);
  }

  private loadLessonTips(bookId?: number): void {
    this.isLoading.set(true);

    this.lessonTipService
      .getAll(bookId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (lessonTips) => {
          this.lessonTips.set(lessonTips);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
          this.toastService.show(
            'Lestips konden niet worden geladen.',
            'error',
          );
        },
      });
  }
}
