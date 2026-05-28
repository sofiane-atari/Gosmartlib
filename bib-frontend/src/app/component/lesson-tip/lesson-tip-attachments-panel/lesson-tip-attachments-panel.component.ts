import { CommonModule, DatePipe } from '@angular/common';
import {
  Component,
  DestroyRef,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ToastService } from '../../../services/toast/toast.service';
import { FileUploadComponent } from '../../shared/file-upload/file-upload.component';
import { LessonTipAttachmentService } from '../../../services/lesson-tip/lesson-tip-attachment.service';
import { LessonTipAttachment } from '../../../model/lesson-tip/lesson-tip-attachment.model';

@Component({
  selector: 'app-lesson-tip-attachments-panel',
  standalone: true,
  imports: [CommonModule, DatePipe, FileUploadComponent],
  templateUrl: './lesson-tip-attachments-panel.component.html',
  styleUrl: './lesson-tip-attachments-panel.component.css',
})
export class LessonTipAttachmentsPanelComponent {
  private readonly attachmentService = inject(LessonTipAttachmentService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  public readonly lessonTipId = input.required<number>();
  public readonly canManageAttachments = input<boolean>(false);
  public readonly compactMode = input<boolean>(false);

  protected readonly attachments = signal<LessonTipAttachment[]>([]);
  protected readonly selectedFile = signal<File | null>(null);
  protected readonly isLoading = signal(false);
  protected readonly isUploading = signal(false);
  protected readonly uploadProgress = signal<number | null>(null);
  protected readonly deletingAttachmentId = signal<number | null>(null);

  private static readonly MAX_ATTACHMENT_SIZE_MB = 50;
  private static readonly BYTES_PER_MB = 1024 * 1024;

  public constructor() {
    effect(
      () => {
        const lessonTipId = this.lessonTipId();
        this.loadAttachments(lessonTipId);
      },
      { allowSignalWrites: true },
    );
  }

  public handleFileSelected(file: File | null): void {
    this.selectedFile.set(file);
    this.uploadProgress.set(null);
  }

  public uploadSelectedFile(): void {
    const lessonTipId = this.lessonTipId();
    const file = this.selectedFile();

    if (!file) {
      this.toastService.show('Selecteer eerst een bestand.', 'error');
      return;
    }

    if (this.isFileTooLarge(file)) {
      this.toastService.show(
        `Dit bestand is te groot. Upload maximaal ${LessonTipAttachmentsPanelComponent.MAX_ATTACHMENT_SIZE_MB} MB per bijlage.`,
        'error',
      );
      return;
    }

    if (this.hasAttachmentWithSameName(file)) {
      this.toastService.show(
        'Er bestaat al een bijlage met deze bestandsnaam.',
        'error',
      );
      return;
    }

    this.isUploading.set(true);
    this.uploadProgress.set(0);

    this.attachmentService
      .upload(lessonTipId, file)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (uploadState) => {
          this.uploadProgress.set(uploadState.progress);

          if (uploadState.attachment) {
            this.attachments.update((currentAttachments) => [
              uploadState.attachment!,
              ...currentAttachments,
            ]);

            this.selectedFile.set(null);
            this.isUploading.set(false);
            this.uploadProgress.set(null);

            this.toastService.show('Bijlage succesvol toegevoegd.', 'success');
          }
        },
        error: () => {
          this.isUploading.set(false);
          this.uploadProgress.set(null);

          this.toastService.show(
            `De bijlage kon niet worden opgeladen. Controleer of het bestand kleiner is dan ${LessonTipAttachmentsPanelComponent.MAX_ATTACHMENT_SIZE_MB} MB.`,
            'error',
          );
        },
      });
  }

  public downloadAttachment(attachment: LessonTipAttachment): void {
    this.attachmentService
      .download(this.lessonTipId(), attachment.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (blob) => {
          const objectUrl = window.URL.createObjectURL(blob);
          const anchor = document.createElement('a');

          anchor.href = objectUrl;
          anchor.download = attachment.originalFileName;
          anchor.click();

          window.URL.revokeObjectURL(objectUrl);
        },
        error: () => {
          this.toastService.show(
            'De bijlage kon niet worden gedownload.',
            'error',
          );
        },
      });
  }

  public deleteAttachment(attachment: LessonTipAttachment): void {
    this.toastService.showConfirm(
      `Weet je zeker dat je "${attachment.originalFileName}" wilt verwijderen?`,
      () => {
        this.deletingAttachmentId.set(attachment.id);

        this.attachmentService
          .delete(this.lessonTipId(), attachment.id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.attachments.update((currentAttachments) =>
                currentAttachments.filter(
                  (currentAttachment) => currentAttachment.id !== attachment.id,
                ),
              );

              this.deletingAttachmentId.set(null);
              this.toastService.show('Bijlage verwijderd.', 'success');
            },
            error: () => {
              this.deletingAttachmentId.set(null);
              this.toastService.show(
                'De bijlage kon niet worden verwijderd.',
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

  protected formatFileSize(fileSize: number): string {
    if (fileSize < 1024) {
      return `${fileSize} B`;
    }

    if (fileSize < 1024 * 1024) {
      return `${(fileSize / 1024).toFixed(1)} KB`;
    }

    return `${(fileSize / (1024 * 1024)).toFixed(1)} MB`;
  }

  protected isDeletingAttachment(attachmentId: number): boolean {
    return this.deletingAttachmentId() === attachmentId;
  }

  private loadAttachments(lessonTipId: number): void {
    this.isLoading.set(true);

    this.attachmentService
      .getAllByLessonTipId(lessonTipId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (attachments) => {
          this.attachments.set(attachments);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
          this.toastService.show(
            'De bijlagen konden niet worden geladen.',
            'error',
          );
        },
      });
  }

  private isFileTooLarge(file: File): boolean {
    return (
      file.size >
      LessonTipAttachmentsPanelComponent.MAX_ATTACHMENT_SIZE_MB *
        LessonTipAttachmentsPanelComponent.BYTES_PER_MB
    );
  }

  private hasAttachmentWithSameName(file: File): boolean {
    const normalizedFileName = file.name.trim().toLowerCase();

    return this.attachments().some(
      (attachment) =>
        attachment.originalFileName.trim().toLowerCase() === normalizedFileName,
    );
  }
}
