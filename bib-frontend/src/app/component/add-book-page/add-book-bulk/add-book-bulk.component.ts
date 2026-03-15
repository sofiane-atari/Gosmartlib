import { HttpClient } from '@angular/common/http';
import { Component } from '@angular/core';
import { Info, LucideAngularModule } from 'lucide-angular';
import { BookService } from '../../../services/BookService/book.service';
import { ToastService } from '../../../services/Toast/toast.service';
import { FileValidationError } from '../../../model/FileValidationError';
import { FileValidationResult } from '../../../model/FileValidationResult';
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
  selectedFile: File | null = null;
  progress = 0;
  errors: FileValidationError[] = [];
  validating = false;
  url: string = "${environment.apiUrl}/bookstemplate";
  apiUrl: string = `${environment.apiUrl}/books`;


  constructor(private http: HttpClient, private bs: BookService, private ts: ToastService){}

  downloadTemplate() {
    window.open(this.url);
  }
  
  onFileSelected(event: any) {
    this.selectedFile = event.target.files[0];
    this.progress = 0;
    this.errors = [];
  }
  async startValidation(file: File) {

    // Step 1: upload file and get temp ID
    const formData = new FormData();
    formData.append('file', file);

    const tempFileId = await this.http.post(
      `${this.apiUrl}/upload/temp`,
      formData,
      { responseType: 'text' } // <-- important!
    ).toPromise();    
    
    // Step 2: SSE validation

    if (!tempFileId) {
      this.ts.show('Kan tijdelijk bestand niet ophalen', 'error');
      this.validating = false;
      return;
    }
    const sse = this.bs.validateBooksStream(tempFileId);

    sse.addEventListener("progress", (event: any) => {
      const data = JSON.parse(event.data);
      this.progress = data.progress;
      if (data.errors?.length) this.errors.push(...data.errors);
    });

    sse.addEventListener("complete", () => {
      this.validating = false;
      this.progress = 100;
      sse.close();
    });

    sse.onerror = (err) => {
      console.error(err);
      this.ts.show("Validation failed", "error");
      this.validating = false;
      sse.close();
    };
  }

  uploadBooks() {
    if (!this.selectedFile) {
     this.ts.show('Geen bestand geselecteerd', 'error' );
      return;
    }

    this.bs.uploadBooks(this.selectedFile).subscribe({
      next: () => {
      this.ts.show('Boeken succesvol geüpload','success' );
      this.selectedFile = null;
      },
      error: (err) => {
        console.error(err);
        let msg = 'Upload gefailed';
        if (err.status === 400) msg = 'invalide formaat';
        else if (err.status === 409) msg = 'Bepaalde boeken bestaan al';
        else if (err.error?.message) msg = err.error.message;

        this.ts.show(msg, 'error' );

      }
    });
  }
}
