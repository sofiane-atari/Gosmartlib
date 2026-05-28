import { HttpClient } from '@angular/common/http';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Download, CheckSquare, Plus, Info, LucideAngularModule } from 'lucide-angular';
import { BookService } from '../../../../services/bookService/book.service';
import { ToastService } from '../../../../services/toast/toast.service';
import { FileValidationError } from '../../../../model/validation/fileValidationError.model';
import { environment } from '../../../../../environments/environment';
import { FileUploadComponent } from '../../../shared/file-upload/file-upload.component';
import { CommonModule } from '@angular/common';
import { ButtonHoverHelpComponent } from "../../../shared/button-hover-help/button-hover-help.component";
import { UserSettingsService } from '../../../../services/user-settings/user-settings.service';
import { FormsModule } from '@angular/forms';
import { Campus } from '../../../../model/campus.model';

@Component({
  selector: 'app-add-book-bulk',
  standalone: true,
  imports: [LucideAngularModule, FileUploadComponent, CommonModule, FormsModule, ButtonHoverHelpComponent],
  templateUrl: './add-book-bulk.component.html',
  styleUrl: './add-book-bulk.component.css'
})
export class AddBookBulkComponent {
  readonly InfoIcon = Info;
  readonly DownloadIcon = Download;
  readonly CheckIcon = CheckSquare;
  readonly PlusIcon = Plus;
  validationCompleted = false;
  validationSucceeded = false;
  @Input() campuses: Campus[] = [];
  @Input() campusId: number | null = null;
  @Output() campusIdChange = new EventEmitter<number | null>();

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
    private ts: ToastService,
    public userSettingsService: UserSettingsService
  ) {}

  downloadTemplate() {
    window.open(this.templateUrl);
  }

  handleFileChange(file: File | null) {
    this.selectedFile = file;
    this.errors = [];
    this.validationCompleted = false;
    this.validationSucceeded = false;
    this.progress = 0;
  }

  selectCampus(campusId: number | null): void {
    this.campusId = campusId;
    this.campusIdChange.emit(campusId);
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.setFile(file);
  }

  private setFile(file: File) {
    this.selectedFile = file;
    this.progress = 0;
    this.errors = [];
    this.validationCompleted = false;
    this.validationSucceeded = false;
  }

  async startValidation(file: File) {
    this.validating = true;
    this.validationCompleted = false;
    this.validationSucceeded = false;
    this.errors = [];

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

    const abortCtrl = this.bs.validateBooksStream(
      tempFileId,
      (event) => {
        if (event.event === 'progress') {
          const data = JSON.parse(event.data);
          this.progress = data.progress;
          if (data.errors?.length) this.errors.push(...data.errors);
        } else if (event.event === 'complete') {
          this.validating = false;
          this.progress = 100;
          this.validationCompleted = true;
          this.validationSucceeded = this.blockingErrors.length === 0;
          abortCtrl.abort();
        }
      },
      (err) => {
        console.error(err);
        this.ts.show('Validatie mislukt', 'error');
        this.validating = false;
        this.validationCompleted = true;
        this.validationSucceeded = false;
        abortCtrl.abort();
      },
    );
  }
  get blockingErrors(): FileValidationError[] {
    return this.errors.filter(e => (e.severity ?? 'error') === 'error');
  }

  get warnings(): FileValidationError[] {
    return this.errors.filter(e => e.severity === 'warning' || e.severity === 'info');
  }
  uploadBooks() {
    if (!this.selectedFile) {
      this.ts.show('Geen bestand geselecteerd', 'error');
      return;
    }

    if (!this.campusId) {
      this.ts.show('Selecteer eerst een campus.', 'error');
      return;
    }

    this.bs.uploadBooks(this.selectedFile, this.campusId).subscribe({
      next: () => {
        this.ts.show('Boeken succesvol geüpload', 'success');
        this.selectedFile = null;
        this.progress = 0;
        this.errors = [];
        this.validationCompleted = false;
        this.validationSucceeded = false;
      },
      error: (err) => {
        console.error(err);
        let msg = 'Upload gefailed';
        if (err.status === 400) msg = 'Invalide formaat';
        else if (err.status === 409) msg = 'Conflict bij opslaan van boek-genre relaties';
        else if (err.error?.message) msg = err.error.message;
        this.ts.show(msg, 'error');
      }
    });
  }
}
