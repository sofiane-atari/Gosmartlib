import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, input, output } from '@angular/core';

import { AuthService } from '../../../services/authorization/auth.service';
import { UserService } from '../../../services/user/user.service';
import { LessonTip } from '../../../model/lesson-tip/lesson-tip.model';

@Component({
  selector: 'app-lesson-tip-card',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './lesson-tip-card.component.html',
  styleUrl: './lesson-tip-card.component.css',
})
export class LessonTipCardComponent {
  private readonly authService = inject(AuthService);
  private readonly userService = inject(UserService);

  public readonly lessonTip = input<LessonTip | undefined>(undefined);
  public readonly isLoading = input<boolean>(false);
  public readonly openRequested = output<number>();

  protected readonly currentUser = computed(() =>
    this.authService.currentUser(),
  );

  protected readonly isOwnLessonTip = computed(() => {
    const currentUser = this.currentUser();
    const currentLessonTip = this.lessonTip();

    return (
      !!currentUser &&
      !!currentLessonTip &&
      currentLessonTip.createdByUserId === currentUser.id
    );
  });

  protected readonly creatorDisplayName = computed(() =>
    this.userService.getDisplayName(this.currentUser()),
  );

  protected readonly creatorRoleLabel = computed(() => {
    const currentLessonTip = this.lessonTip();
    return currentLessonTip
      ? this.userService.getRoleLabel(currentLessonTip.createdByRole)
      : '';
  });

  public openDetail(): void {
    const currentLessonTip = this.lessonTip();
    if (currentLessonTip) {
      this.openRequested.emit(currentLessonTip.id);
    }
  }

  protected getLessonTipTypeLabel(): string {
    const currentLessonTip = this.lessonTip();

    if (!currentLessonTip) {
      return '';
    }

    switch (currentLessonTip.lessonTipType) {
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
}
