import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../../services/Toast/toast.service';

@Component({
  selector: 'app-file-upload',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './file-upload.component.html',
  styleUrl: './file-upload.component.css'
})
export class FileUploadComponent {
  @Input() selectedFile: File | null = null;
  @Input() accept = '.xlsx';
  @Input() maxSizeMB = 10;
  
  @Output() fileChanged = new EventEmitter<File | null>();

  isDragOver = false;

  constructor(private ts: ToastService) {}

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.validateAndSetFile(file);
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.isDragOver = false;
    const file = event.dataTransfer?.files[0];
    if (file) this.validateAndSetFile(file);
  }

  private validateAndSetFile(file: File) {
    const isCorrectType = file.name.toLowerCase().endsWith(this.accept);
    if (!isCorrectType) {
      this.ts.show(`Alleen ${this.accept} bestanden zijn toegestaan`, 'error');
      return;
    }
    if (file.size > this.maxSizeMB * 1024 * 1024) {
      this.ts.show(`Bestand is te groot (max ${this.maxSizeMB}MB)`, 'error');
      return;
    }

    this.fileChanged.emit(file);
  }
}