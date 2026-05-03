import { CommonModule } from '@angular/common';
import {
  Component,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';

import { ToastService } from '../../../services/toast/toast.service';
import { FileUploadComponent } from '../../shared/file-upload/file-upload.component';

@Component({
  selector: 'app-lesson-tip-attachments-queue',
  standalone: true,
  imports: [CommonModule, FileUploadComponent],
  templateUrl: './lesson-tip-attachments-queue.component.html',
  styleUrl: './lesson-tip-attachments-queue.component.css',
})
export class LessonTipAttachmentsQueueComponent {
  private readonly toastService = inject(ToastService);

  public readonly isUploading = input<boolean>(false);
  public readonly uploadProgress = input<number | null>(null);
  public readonly currentUploadingFileName = input<string | null>(null);

  public readonly queueChanged = output<File[]>();

  protected readonly selectedFile = signal<File | null>(null);
  protected readonly queuedFiles = signal<File[]>([]);

  protected readonly hasQueuedFiles = computed(
    () => this.queuedFiles().length > 0,
  );

  public handleFileChange(file: File | null): void {
    this.selectedFile.set(file);
  }

  public addToQueue(): void {
    const selectedFile = this.selectedFile();

    if (!selectedFile) {
      this.toastService.show('Selecteer eerst een bestand.', 'error');
      return;
    }

    const alreadyQueued = this.queuedFiles().some(
      (queuedFile) =>
        queuedFile.name === selectedFile.name &&
        queuedFile.size === selectedFile.size &&
        queuedFile.lastModified === selectedFile.lastModified,
    );

    if (alreadyQueued) {
      this.toastService.show('Dit bestand staat al in de wachtrij.', 'error');
      return;
    }

    this.queuedFiles.update((currentFiles) => [...currentFiles, selectedFile]);
    this.selectedFile.set(null);
    this.emitQueue();
    this.toastService.show('Bestand toegevoegd aan de wachtrij.', 'success');
  }

  public removeQueuedFile(index: number): void {
    this.queuedFiles.update((currentFiles) =>
      currentFiles.filter((_, currentIndex) => currentIndex !== index),
    );

    this.emitQueue();
  }

  public clearQueue(): void {
    this.queuedFiles.set([]);
    this.selectedFile.set(null);
    this.emitQueue();
  }

  private emitQueue(): void {
    this.queueChanged.emit([...this.queuedFiles()]);
  }
}
