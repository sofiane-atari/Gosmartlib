import { Component, OnInit } from '@angular/core';
import { Book } from '../../model/book.model';
import { BookService } from '../../services/book.service';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-books',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './books.component.html',
  styleUrl: './books.component.css'
})
export class BooksComponent implements OnInit {
  books: Book[] = [];
  selectedBook?: Book;
  showModal = false;
  public availabilityOptions = ['AVAILABLE', 'DAMAGED', 'LOST', 'BORROWED'];

  constructor(private bs: BookService, private router: Router){}

  ngOnInit(): void {
    this.bs.getBooks().subscribe(data => {this.books = data;});
  }
  openModal(book: Book){
    this.selectedBook = book;
    this.showModal = true;
  }
  closeModal(){
    this.selectedBook = undefined;
    this.showModal = false;
  }

}
