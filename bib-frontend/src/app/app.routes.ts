import { BooksComponent } from './component/catalogus/books/books.component';
import { BookDetailComponent } from './component/catalogus/book-detail/book-detail.component';
import { AddBookPageComponent } from './component/book/add-book-page/add-book-page.component';
import { BookEditPageComponent } from './component/book/book-edit-page/book-edit-page.component';
import { BookManagementComponent } from './component/book/book-management/book-management.component';
import { LessonTipRoutes } from './routes/lesson-tip.routes';

// Genre
import { DashboardGenresComponent } from './component/genre/dashboard-genres/dashboard-genres.component';

// Loan
import { CreateLoanComponent } from './component/loan/create-loan/create-loan.component';

// Login & Profile
import { ProfileComponent } from './component/login-profile/profile/profile.component';
import { LoginComponent } from './component/login-profile/login/login.component';
import { SmartschoolCallbackComponent } from './component/login-profile/smartschool-callback/smartschool-callback.component';

// Guards
import { authGuard, publicOnlyGuard } from './guards/authentication/auth.guard';
import { teacherOnlyGuard } from './guards/authentication/teacher-only.guard';
import { adminOnlyGuard } from './guards/authentication/admin-only.guard';
import { reviewModerationGuard } from './guards/review/review-moderation.guard';
import { Routes } from '@angular/router';

// reviews
import { ReviewDashboardComponent } from './component/reviews/review-dashboard/review-dashboard.component';
import { MyReviewsComponent } from './component/reviews/my-reviews/my-reviews.component';

// Reading List
import { ReadingListOverviewComponent } from './component/reading-list/reading-list-overview/reading-list-overview.component';
import { CreateReadingListComponent } from './component/reading-list/create-reading-list/create-reading-list.component';
import { CreateTeacherReadingListComponent } from './component/reading-list/create-teacher-reading-list/create-teacher-reading-list.component';
import { ReadingListDetailComponent } from './component/reading-list/reading-list-detail/reading-list-detail.component';
import { SchoolDashboardComponent } from './component/school/school-dashboard/school-dashboard.component';
import { staffOnlyGuard } from './guards/authentication/staff-only.guard';
import { SettingsComponent } from './component/settings/settings/settings.component';
import { TagDashboardComponent } from './component/tags/tag-dashboard/tag-dashboard.component';
import {ReturnLoanComponent} from "./component/loan/return-loan/return-loan.component";

export const routes: Routes = [
  { path: '', redirectTo: '/catalog', pathMatch: 'full' },
  { path: 'account', redirectTo: '/profile', pathMatch: 'full' },
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
  { path: 'catalog', component: BooksComponent, canActivate: [authGuard] },
  {
    path: 'review-dashboard',
    component: ReviewDashboardComponent,
    canActivate: [reviewModerationGuard, authGuard, staffOnlyGuard],
  },

  ...LessonTipRoutes,
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
    canActivate: [authGuard, adminOnlyGuard],
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
    path: 'reading-list/create-teacher',
    component: CreateTeacherReadingListComponent,
    canActivate: [authGuard, teacherOnlyGuard],
  },
  {
    path: 'reading-list/shared/:publicShareId',
    component: ReadingListDetailComponent,
    canActivate: [authGuard],
    data: { shared: true }
  },
  {
    path: 'reading-list/:id',
    component: ReadingListDetailComponent,
    canActivate: [authGuard],
  },
  {
    path: 'add-book',
    component: AddBookPageComponent,
    canActivate: [authGuard, staffOnlyGuard],
  },
  { path: 'reviews', component: MyReviewsComponent, canActivate: [authGuard] },
  { path: 'login', component: LoginComponent, canActivate: [publicOnlyGuard] },
  {
    path: 'oauth2/callback/smartschool',
    component: SmartschoolCallbackComponent,
  },
  { path: 'smartschool-callback', component: SmartschoolCallbackComponent },
  {
    path: 'books-management',
    component: BookManagementComponent,
    canActivate: [authGuard, adminOnlyGuard],
  },
  {
    path: 'loans/create',
    component: CreateLoanComponent,
    canActivate: [authGuard, staffOnlyGuard],
  },
  {
    path: 'schools',
    component: SchoolDashboardComponent,
    canActivate: [authGuard, adminOnlyGuard],
  },
  {
    path: 'tag',
    component: TagDashboardComponent,
    canActivate: [authGuard, adminOnlyGuard],
  },
  {
    path: 'loans/return',
    component: ReturnLoanComponent,
    canActivate: [authGuard, staffOnlyGuard],
  },
  {
    path: 'settings',
    component: SettingsComponent,
    canActivate: [authGuard],
  },
  { path: '**', redirectTo: '/catalog' },
];
