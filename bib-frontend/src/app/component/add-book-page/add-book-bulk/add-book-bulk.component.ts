import { HttpClient } from '@angular/common/http';
import { Component } from '@angular/core';
import { Info, LucideAngularModule } from 'lucide-angular';
import { BookService } from '../../../services/BookService/book.service';
import { ToastService } from '../../../services/Toast/toast.service';

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
  url: string = "http://localhost:8080/api/books/template";
  constructor(private http: HttpClient, private bs: BookService, private ts: ToastService){}

  downloadTemplate() {
    window.open(this.url);
  }
  
  onFileSelected(event: any) {
  this.selectedFile = event.target.files[0];
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
