import { Routes } from '@angular/router';
import { CreateReadingListComponent } from './component/create-reading-list/create-reading-list.component';
import { ReadingListOverviewComponent } from './component/reading-list-overview/reading-list-overview.component';
import { BooksComponent } from './component/books/books.component';
import { BookDetailComponent } from './component/book-detail/book-detail.component';
import { AddBookPageComponent } from './component/add-book-page/add-book-page.component';
import { ReadingListDetailComponent } from './component/reading-list-detail/reading-list-detail.component';
import { MyReviewsComponent } from './component/my-reviews/my-reviews.component';
import { ProfileComponent } from './component/profile/profile.component';
import { DashboardGenresComponent } from './component/dashboard-genres/dashboard-genres.component';
import { LoginComponent } from './component/login/login.component';
import { SmartschoolCallbackComponent } from './component/smartschool-callback/smartschool-callback.component';
import { authGuard, publicOnlyGuard } from './guards/auth.guard';
import { BookManagementComponent } from './component/book-management/book-management.component';
import { CreateLoanComponent } from './component/create-loan/create-loan.component';
import { BookEditPageComponent } from './component/book-edit-page/book-edit-page.component';
import { ReviewDashboardComponent } from './component/dashboard/review-dashboard/review-dashboard.component';
import { reviewModerationGuard } from './guards/review-moderation.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/catalog', pathMatch: 'full' },
  { path: 'account', redirectTo: '/profile', pathMatch: 'full' },
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard]},
  { path: 'catalog', component: BooksComponent, canActivate: [authGuard] },
  { path: 'review-dashboard', component: ReviewDashboardComponent, canActivate: [reviewModerationGuard, authGuard],},
  {
    path: 'book/:id',
    component: BookDetailComponent,
    canActivate: [authGuard],
  },
  {
    path: 'books/:id/edit',
    component: BookEditPageComponent,
    canActivate: [authGuard],
  },
  {
    path: 'genre',
    component: DashboardGenresComponent,
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
  {
    path: 'oauth2/callback/smartschool',
    component: SmartschoolCallbackComponent,
  },
  { path: 'smartschool-callback', component: SmartschoolCallbackComponent },
  { path: 'books-management', component: BookManagementComponent },
  { path: 'loans/create', component: CreateLoanComponent},

  { path: '**', redirectTo: '/catalog' },

];