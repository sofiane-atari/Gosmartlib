import { Routes } from '@angular/router';
import { BooksComponent } from './component/books/books.component';
import { CreateLabelComponent } from './component/create-label/create-label.component';
import { LabelsListComponent } from './component/labels-list/labels-list.component';

export const routes: Routes = [
  { path: 'catalogus', component: BooksComponent },
  { path: '', redirectTo: '/catalogus', pathMatch: 'full' },

  { path: 'labels', component: LabelsListComponent },
];
