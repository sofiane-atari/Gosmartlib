import { HttpClient } from '@angular/common/http';
import { Component } from '@angular/core';
import { Info, LucideAngularModule } from 'lucide-angular';
import { BookService } from '../../../services/BookService/book.service';
import { ToastService } from '../../../services/Toast/toast.service';
import { FileValidationError } from '../../../model/FileValidationError';
import { FileValidationResult } from '../../../model/FileValidationResult';

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
  url: string = "http://localhost:8080/api/books/template";


  constructor(private http: HttpClient, private bs: BookService, private ts: ToastService){}

  downloadTemplate() {
    window.open(this.url);
  }
  
  onFileSelected(event: any) {
    this.selectedFile = event.target.files[0];
    this.progress = 0;
    this.errors = [];
  }
  validate(){
    if (!this.selectedFile) {
      this.ts.show('Geen bestand geselecteerd', 'error' );
      return;
    }

    this.validating = true;
    
    this.bs.validateFile(this.selectedFile).subscribe({
      next: (res: FileValidationResult) => {
        this.progress = 100;
        this.errors = res.errors;
        this.ts.show(`Validatie compleet : ${res.validRows} valide, ${res.invalidRows} errors`, 'success');
      },
      error: (err) => {
        console.error(err);
        this.ts.show(err.error?.message || 'Validatie gefailed', 'error');
        this.progress = 0;
      },
      complete: () => (this.validating = false),
    });
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
