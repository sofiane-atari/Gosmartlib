import { Routes } from '@angular/router';
import { CreateReadingListComponent } from './component/create-reading-list/create-reading-list.component';
import {ReadingListOverviewComponent} from "./component/reading-list-overview/reading-list-overview.component";
import { BooksComponent } from './component/books/books.component';

export const routes: Routes = [
    { path: 'catalogus', component: BooksComponent },
    // { path: 'favorieten', component:  },
    { path: '', redirectTo: '/catalogus', pathMatch: 'full' },
    { path: 'reading-list', component: ReadingListOverviewComponent},
    { path: 'reading-list/create', component: CreateReadingListComponent },

];
