import { Routes } from '@angular/router';
import { CreateReadingListComponent } from './component/create-reading-list/create-reading-list.component';
import { ReadingListOverviewComponent } from './component/reading-list-overview/reading-list-overview.component';
import { BooksComponent } from './component/books/books.component';
import { BookDetailComponent } from './component/book-detail/book-detail.component';
import { AddBookPageComponent } from './component/add-book-page/add-book-page.component';
import { ReadingListDetailComponent } from './component/reading-list-detail/reading-list-detail.component';
import { MyReviewsComponent } from './component/my-reviews/my-reviews.component';
import { DashboardLabelsComponent } from './component/dashboard-labels/dashboard-labels.component';
import { LoginComponent } from './component/login/login.component';
import { SmartschoolCallbackComponent } from './component/smartschool-callback/smartschool-callback.component';
import { authGuard, publicOnlyGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/catalog', pathMatch: 'full' },
  { path: 'catalog', component: BooksComponent, canActivate: [authGuard] },
  {
    path: 'book/:id',
    component: BookDetailComponent,
    canActivate: [authGuard],
  },
  {
    path: 'labels',
    component: DashboardLabelsComponent,
    canActivate: [authGuard],
  },
  {
    path: 'reading-list',
    component: ReadingListOverviewComponent,
    canActivate: [authGuard],
  },
  {
    path: 'reading-list/create',
    component: CreateReadingListComponent,
    canActivate: [authGuard],
  },
  {
    path: 'reading-list/:id',
    component: ReadingListDetailComponent,
    canActivate: [authGuard],
  },
  {
    path: 'add-book',
    component: AddBookPageComponent,
    canActivate: [authGuard],
  },
  { path: 'reviews', component: MyReviewsComponent, canActivate: [authGuard] },
  { path: 'login', component: LoginComponent, canActivate: [publicOnlyGuard] },
  { path: 'smartschool-callback', component: SmartschoolCallbackComponent },
  { path: '**', redirectTo: '/catalog' },
];
