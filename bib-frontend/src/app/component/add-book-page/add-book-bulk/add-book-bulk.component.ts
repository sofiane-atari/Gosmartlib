import { HttpClient } from '@angular/common/http';
import { Component } from '@angular/core';
import { Download, CheckSquare, Plus, Info, LucideAngularModule } from 'lucide-angular';
import { BookService } from '../../../services/BookService/book.service';
import { ToastService } from '../../../services/Toast/toast.service';
import { FileValidationError } from '../../../model/FileValidationError';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-add-book-bulk',
  standalone: true,
  imports: [LucideAngularModule],
  templateUrl: './add-book-bulk.component.html',
  styleUrl: './add-book-bulk.component.css'
})
export class AddBookBulkComponent {
  readonly InfoIcon = Info;
  readonly DownloadIcon = Download;
  readonly CheckIcon = CheckSquare;
  readonly PlusIcon = Plus;

  selectedFile: File | null = null;
  progress = 0;
  errors: FileValidationError[] = [];
  validating = false;
  isDragOver = false;

  private templateUrl = `${environment.apiUrl}/books/template`;
  private apiUrl = `${environment.apiUrl}/books`;

  constructor(
    private http: HttpClient,
    private bs: BookService,
    private ts: ToastService
  ) {}

  downloadTemplate() {
    window.open(this.templateUrl);
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.setFile(file);
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
    const file = event.dataTransfer?.files[0];
    if (file && file.name.endsWith('.xlsx')) {
      this.setFile(file);
    } else {
      this.ts.show('Alleen .xlsx bestanden zijn toegestaan', 'error');
    }
  }

  private setFile(file: File) {
    this.selectedFile = file;
    this.progress = 0;
    this.errors = [];
  }

  async startValidation(file: File) {
    this.validating = true;
    this.errors = [];
    this.progress = 0;

    const formData = new FormData();
    formData.append('file', file);

    const tempFileId = await this.http.post(
      `${this.apiUrl}/upload/temp`,
      formData,
      { responseType: 'text' }
    ).toPromise();

    if (!tempFileId) {
      this.ts.show('Kan tijdelijk bestand niet ophalen', 'error');
      this.validating = false;
      return;
    }

    const sse = this.bs.validateBooksStream(tempFileId);

    sse.addEventListener('progress', (event: any) => {
      const data = JSON.parse(event.data);
      this.progress = data.progress;
      if (data.errors?.length) this.errors.push(...data.errors);
    });

    sse.addEventListener('complete', () => {
      this.validating = false;
      this.progress = 100;
      sse.close();
    });

    sse.onerror = (err: any) => {
      console.error(err);
      this.ts.show('Validatie mislukt', 'error');
      this.validating = false;
      sse.close();
    };
  }

  uploadBooks() {
    if (!this.selectedFile) {
      this.ts.show('Geen bestand geselecteerd', 'error');
      return;
    }

    this.bs.uploadBooks(this.selectedFile).subscribe({
      next: () => {
        this.ts.show('Boeken succesvol geüpload', 'success');
        this.selectedFile = null;
        this.progress = 0;
        this.errors = [];
      },
      error: (err) => {
        console.error(err);
        let msg = 'Upload gefailed';
        if (err.status === 400) msg = 'Invalide formaat';
        else if (err.status === 409) msg = 'Bepaalde boeken bestaan al';
        else if (err.error?.message) msg = err.error.message;
        this.ts.show(msg, 'error');
      }
    });
  }
}