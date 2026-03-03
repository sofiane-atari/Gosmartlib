import { Component, OnInit } from '@angular/core';
import { Book } from '../../model/book.model';
import { BookService } from '../../service/book.service';

@Component({
  selector: 'app-books',
  standalone: true,
  imports: [],
  templateUrl: './books.component.html',
  styleUrl: './books.component.css'
})
export class BooksComponent implements OnInit {
  books: Book[] = [];

  constructor(private bs: BookService){}

  ngOnInit(): void {
    this.bs.getBooks().subscribe(data => {this.books = data;});
  }

}
