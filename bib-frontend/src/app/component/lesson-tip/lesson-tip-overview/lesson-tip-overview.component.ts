import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
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

import { BookService } from '../../../services/bookService/book.service';
import { Book } from '../../../model/book/book.model';

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
  private readonly bookService = inject(BookService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly lessonTips = signal<LessonTip[]>([]);
  protected readonly isLoading = signal(false);

  protected readonly filteredBookId = signal<number | null>(null);
  protected readonly filteredBook = signal<Book | null>(null);
  protected readonly isLoadingBookContext = signal(false);

  public ngOnInit(): void {
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const bookId = this.parseBookId(params.get('bookId'));

        if (bookId === null && params.has('bookId')) {
          this.handleInvalidBookFilter();
          return;
        }

        this.filteredBookId.set(bookId);
        this.loadBookContext(bookId);
        this.loadLessonTips(bookId ?? undefined);
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

  private parseBookId(bookIdParam: string | null): number | null {
    if (!bookIdParam) {
      return null;
    }

    const parsedBookId = Number(bookIdParam);

    if (!Number.isInteger(parsedBookId) || parsedBookId <= 0) {
      return null;
    }

    return parsedBookId;
  }

  private handleInvalidBookFilter(): void {
    this.filteredBookId.set(null);
    this.filteredBook.set(null);
    this.toastService.show('Ongeldig boekfilter.', 'error');

    void this.router.navigate([`/${LESSON_TIP_PATHS.root}`], {
      replaceUrl: true,
    });
  }

  private loadBookContext(bookId: number | null): void {
    this.filteredBook.set(null);

    if (!bookId) {
      return;
    }

    this.isLoadingBookContext.set(true);

    this.bookService
      .getBookById(bookId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (book) => {
          this.filteredBook.set(book);
          this.isLoadingBookContext.set(false);
        },
        error: () => {
          this.filteredBook.set(null);
          this.isLoadingBookContext.set(false);

          this.toastService.show(
            'De boekgegevens konden niet worden geladen.',
            'error',
          );
        },
      });
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
          this.lessonTips.set([]);
          this.isLoading.set(false);

          this.toastService.show(
            'Lestips konden niet worden geladen.',
            'error',
          );
        },
      });
  }
}
