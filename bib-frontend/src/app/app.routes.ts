import { Routes } from '@angular/router';
import { CreateReadingListComponent } from './component/create-reading-list/create-reading-list.component';
import { ReadingListOverviewComponent } from "./component/reading-list-overview/reading-list-overview.component";
import { BooksComponent } from './component/books/books.component';
import { BookDetailComponent } from './component/book-detail/book-detail.component';
import { AddBookPageComponent } from './component/add-book-page/add-book-page.component';
import { ReadingListDetailComponent } from './component/reading-list-detail/reading-list-detail.component';

export const routes: Routes = [
    { path: 'catalog', component: BooksComponent },
    { path: 'book/:id', component: BookDetailComponent },
    { path: '', redirectTo: '/catalog', pathMatch: 'full' },
    { path: 'reading-list', component: ReadingListOverviewComponent },
    { path: 'reading-list/create', component: CreateReadingListComponent },
    { path: 'reading-list/:id', component: ReadingListDetailComponent },
    { path: 'add-book', component: AddBookPageComponent }
];

