import { HttpClient } from '@angular/common/http';
import { Component } from '@angular/core';
import { Info, LucideAngularModule } from 'lucide-angular';
import { BookService } from '../../../services/BookService/book.service';

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
  constructor(private http: HttpClient, private bs: BookService){}

  downloadTemplate() {
    window.open(this.url);
  }
  
  onFileSelected(event: any) {
  this.selectedFile = event.target.files[0];
  }

  uploadBooks() {
    if (!this.selectedFile) {
      alert("Please select a file first!");
      return;
    }

    this.bs.uploadBooks(this.selectedFile).subscribe({
      next: () => {
        alert("Books uploaded successfully");
        // Optional: Clear the selection after success
        this.selectedFile = null;
      },
      error: (err) => {
      }
    });
  }
}
