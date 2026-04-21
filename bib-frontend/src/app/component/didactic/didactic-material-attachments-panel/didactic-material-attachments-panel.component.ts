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

import { DidacticMaterialAttachmentService } from '../../../services/didactic/didactic-material-attachment.service';
import { ToastService } from '../../../services/toast/toast.service';
import { FileUploadComponent } from '../../shared/file-upload/file-upload.component';
import { DidacticMaterialAttachment } from '../../../model/didactic/didactic-material-attachment.model';

@Component({
  selector: 'app-didactic-material-attachments-panel',
  standalone: true,
  imports: [CommonModule, DatePipe, FileUploadComponent],
  templateUrl: './didactic-material-attachments-panel.component.html',
  styleUrl: './didactic-material-attachments-panel.component.css',
})
export class DidacticMaterialAttachmentsPanelComponent {
  private readonly attachmentService = inject(
    DidacticMaterialAttachmentService,
  );
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  public readonly didacticMaterialId = input.required<number>();
  public readonly canManageAttachments = input<boolean>(false);

  protected readonly attachments = signal<DidacticMaterialAttachment[]>([]);
  protected readonly selectedFile = signal<File | null>(null);
  protected readonly isLoading = signal(false);
  protected readonly isUploading = signal(false);
  protected readonly uploadProgress = signal<number | null>(null);
  protected readonly deletingAttachmentId = signal<number | null>(null);

  public constructor() {
    effect(
      () => {
        const materialId = this.didacticMaterialId();
        this.loadAttachments(materialId);
      },
      { allowSignalWrites: true },
    );
  }

  public handleFileSelected(file: File | null): void {
    this.selectedFile.set(file);
    this.uploadProgress.set(null);
  }

  public uploadSelectedFile(): void {
    const materialId = this.didacticMaterialId();
    const file = this.selectedFile();

    if (!file) {
      this.toastService.show('Selecteer eerst een bestand.', 'error');
      return;
    }

    this.isUploading.set(true);
    this.uploadProgress.set(0);

    this.attachmentService
      .upload(materialId, file)
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
            'De bijlage kon niet worden opgeladen.',
            'error',
          );
        },
      });
  }

  public downloadAttachment(attachment: DidacticMaterialAttachment): void {
    this.attachmentService
      .download(this.didacticMaterialId(), attachment.id)
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

  public deleteAttachment(attachment: DidacticMaterialAttachment): void {
    this.toastService.showConfirm(
      `Weet je zeker dat je "${attachment.originalFileName}" wilt verwijderen?`,
      () => {
        this.deletingAttachmentId.set(attachment.id);

        this.attachmentService
          .delete(this.didacticMaterialId(), attachment.id)
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

  private loadAttachments(didacticMaterialId: number): void {
    this.isLoading.set(true);

    this.attachmentService
      .getAllByMaterialId(didacticMaterialId)
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
}
