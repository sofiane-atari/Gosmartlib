import { Routes } from '@angular/router';
import { BooksComponent } from './component/books/books.component';
import { TestComponentRenderer } from '@angular/core/testing';
import { BookDetailComponent } from './component/book-detail/book-detail.component';

export const routes: Routes = [
    { path: 'catalogus', component: BooksComponent },
    { path: 'book/:id', component: BookDetailComponent },

    // { path: 'favorieten', component:  },
    { path: '', redirectTo: '/catalogus', pathMatch: 'full' },
];
