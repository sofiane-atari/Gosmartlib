import { Routes } from '@angular/router';
import { CreateReadingListComponent } from './component/create-reading-list/create-reading-list.component';
import { ReadingListOverviewComponent } from "./component/reading-list-overview/reading-list-overview.component";
import { BooksComponent } from './component/books/books.component';
import { CreateLabelComponent } from './component/create-label/create-label.component';
import { LabelsListComponent } from './component/labels-list/labels-list.component';
import { TestComponentRenderer } from '@angular/core/testing';
import { BookDetailComponent } from './component/book-detail/book-detail.component';
import { AddBookPageComponent } from './component/add-book-page/add-book-page.component';
import { ReadingListDetailComponent } from './component/reading-list-detail/reading-list-detail.component';
import { MyReviewsComponent } from './component/my-reviews/my-reviews.component';

export const routes: Routes = [
  { path: 'catalogus', component: BooksComponent },
  { path: '', redirectTo: '/catalogus', pathMatch: 'full' },

  { path: 'labels', component: LabelsListComponent },
  { path: 'catalog', component: BooksComponent },
  { path: 'book/:id', component: BookDetailComponent },

  { path: '', redirectTo: '/catalog', pathMatch: 'full' },
  { path: 'reading-list', component: ReadingListOverviewComponent },
  { path: 'reading-list/create', component: CreateReadingListComponent },
  { path: 'reading-list/:id', component: ReadingListDetailComponent },
  { path: 'add-book', component: AddBookPageComponent },
  { path: 'reviews', component: MyReviewsComponent }
];

