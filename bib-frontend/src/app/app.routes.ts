import { Routes } from '@angular/router';
import { CreateReadingListComponent } from './component/create-reading-list/create-reading-list.component';
import {ReadingListOverviewComponent} from "./component/reading-list-overview/reading-list-overview.component";
import { BooksComponent } from './component/books/books.component';
import { TestComponentRenderer } from '@angular/core/testing';
import { BookDetailComponent } from './component/book-detail/book-detail.component';
import { AddBookPageComponent } from './component/add-book-page/add-book-page.component';

export const routes: Routes = [
    { path: 'catalog', component: BooksComponent },
    { path: 'book/:id', component: BookDetailComponent },

    // { path: 'favorieten', component:  },
    { path: '', redirectTo: '/catalog', pathMatch: 'full' },
    { path: 'reading-list', component: ReadingListOverviewComponent},
    { path: 'reading-list/create', component: CreateReadingListComponent },
    { path: 'add-book', component: AddBookPageComponent }

];
