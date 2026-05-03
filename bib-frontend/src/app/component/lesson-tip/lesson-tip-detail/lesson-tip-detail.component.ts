import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ToastService } from '../../../services/toast/toast.service';
import { LessonTipService } from '../../../services/lesson-tip/lesson-tip.service';
import { AuthService } from '../../../services/authorization/auth.service';
import { UserService } from '../../../services/user/user.service';
import { RoleService } from '../../../services/role-service/role.service';
import { LESSON_TIP_PATHS } from '../../../routes/lesson-tip.routes';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LessonTipAttachmentsPanelComponent } from '../lesson-tip-attachments-panel/lesson-tip-attachments-panel.component';
import { Trash2, Pencil, LucideAngularModule } from 'lucide-angular';
import { LessonTip } from '../../../model/lesson-tip/lesson-tip.model';

@Component({
  selector: 'app-lesson-tip-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    DatePipe,
    LessonTipAttachmentsPanelComponent,
    LucideAngularModule,
  ],
  templateUrl: './lesson-tip-detail.component.html',
  styleUrl: './lesson-tip-detail.component.css',
})
export class LessonTipDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly lessonTipService = inject(LessonTipService);
  private readonly toastService = inject(ToastService);
  private readonly authService = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly roleService = inject(RoleService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly lessonTip = signal<LessonTip | null>(null);
  protected readonly isLoading = signal(false);
  protected readonly isDeleting = signal(false);

  protected readonly Trash2 = Trash2;
  protected readonly Pencil = Pencil;
  protected readonly currentUser = computed(() =>
    this.authService.currentUser(),
  );

  protected readonly isOwnLessonTip = computed(() => {
    const lessonTip = this.lessonTip();

    return this.userService.isOwnResource(
      this.currentUser()?.id,
      lessonTip?.createdByUserId,
    );
  });

  protected readonly canModifyLessonTip = computed(() =>
    this.roleService.canModifyLessonTip(this.lessonTip()?.createdByUserId),
  );

  protected readonly creatorDisplayName = computed(() =>
    this.userService.getDisplayName(this.currentUser()),
  );

  protected readonly creatorRoleLabel = computed(() =>
    this.userService.getRoleLabel(this.lessonTip()?.createdByRole),
  );

  public ngOnInit(): void {
    const lessonTipId = Number(this.route.snapshot.paramMap.get('id'));

    if (!Number.isFinite(lessonTipId)) {
      this.toastService.show('Ongeldige lestip.', 'error');
      return;
    }

    this.loadLessonTip(lessonTipId);
  }

  public navigateToEditPage(): void {
    const lessonTip = this.lessonTip();

    if (!lessonTip) {
      return;
    }

    void this.router.navigate([
      `/${LESSON_TIP_PATHS.root}/${lessonTip.id}/${LESSON_TIP_PATHS.edit}`,
    ]);
  }

  public deleteLessonTip(): void {
    const selectedLessonTip = this.lessonTip();

    if (!selectedLessonTip || this.isDeleting()) {
      return;
    }

    this.toastService.showConfirm(
      'Weet je zeker dat je deze lestip wilt verwijderen?',
      () => {
        this.isDeleting.set(true);

        this.lessonTipService
          .delete(selectedLessonTip.id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.isDeleting.set(false);
              this.toastService.show('Lestip is verwijderd.', 'success');
              void this.router.navigate([`/${LESSON_TIP_PATHS.root}`]);
            },
            error: () => {
              this.isDeleting.set(false);
              this.toastService.show(
                'De lestip kon niet worden verwijderd.',
                'error',
              );
            },
          });
      },
      () => {
        this.toastService.show('Verwijderen geannuleerd.', 'error');
      },
    );
  }

  protected getLessonTipTypeLabel(
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

  private loadLessonTip(lessonTipId: number): void {
    this.isLoading.set(true);

    this.lessonTipService
      .getById(lessonTipId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (lessonTip) => {
          this.lessonTip.set(lessonTip);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
          this.toastService.show('De lestip kon niet worden geladen.', 'error');
        },
      });
  }
}
