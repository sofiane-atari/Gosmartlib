import { Routes } from '@angular/router';
import { BooksComponent } from './component/books/books.component';
import { TestComponentRenderer } from '@angular/core/testing';
import { CreateLabelComponent } from './component/create-label/create-label.component';

export const routes: Routes = [
  { path: 'catalogus', component: BooksComponent },
  { path: '', redirectTo: '/catalogus', pathMatch: 'full' },

  { path: '/labels/create', component: CreateLabelComponent },
];
