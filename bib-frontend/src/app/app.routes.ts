import { Routes } from '@angular/router';
import { BooksComponent } from './component/books/books.component';
import { TestComponentRenderer } from '@angular/core/testing';

export const routes: Routes = [
    { path: 'catalogus', component: BooksComponent },
    // { path: 'favorieten', component:  },
    { path: '', redirectTo: '/catalogus', pathMatch: 'full' },
];
