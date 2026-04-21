import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../../../services/toast/toast.service';

@Component({
  selector: 'app-file-upload',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './file-upload.component.html',
  styleUrl: './file-upload.component.css',
})
export class FileUploadComponent {
  @Input() selectedFile: File | null = null;
  @Input() accept = '.xlsx';
  @Input() maxSizeMB = 10;

  /**
   * Optionele extra configuratie.
   * Bestaande componenten moeten deze niet meegeven.
   */
  @Input() label = 'Sleep een bestand hierheen of klik om te uploaden';
  @Input() subLabel = 'Excel bestanden';
  @Input() disabled = false;
  @Input() uploadProgress: number | null = null;
  @Input() showProgress = false;

  @Output() fileChanged = new EventEmitter<File | null>();

  isDragOver = false;

  constructor(private ts: ToastService) {}

  get displayLabel(): string {
    return this.selectedFile ? this.selectedFile.name : this.label;
  }

  onFileSelected(event: Event): void {
    if (this.disabled) {
      return;
    }

    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    if (file) {
      this.validateAndSetFile(file);
    }

    input.value = '';
  }

  onDragOver(event: DragEvent): void {
    if (this.disabled) {
      return;
    }

    event.preventDefault();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent): void {
    if (this.disabled) {
      return;
    }

    event.preventDefault();
    this.isDragOver = false;

    const file = event.dataTransfer?.files?.[0] ?? null;

    if (file) {
      this.validateAndSetFile(file);
    }
  }

  private validateAndSetFile(file: File): void {
    if (!this.isAllowedFileType(file.name)) {
      this.ts.show(
        `Ongeldig bestandstype. Toegelaten: ${this.accept}`,
        'error',
      );
      return;
    }

    if (file.size > this.maxSizeMB * 1024 * 1024) {
      this.ts.show(`Bestand is te groot (max ${this.maxSizeMB} MB)`, 'error');
      return;
    }

    this.fileChanged.emit(file);
  }

  private isAllowedFileType(fileName: string): boolean {
    const normalizedFileName = fileName.toLowerCase();

    const allowedExtensions = this.accept
      .split(',')
      .map((value) => value.trim().toLowerCase())
      .filter((value) => value.length > 0);

    return allowedExtensions.some((extension) =>
      normalizedFileName.endsWith(extension),
    );
  }
}
