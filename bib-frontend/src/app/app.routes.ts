import { Routes } from '@angular/router';
import { HomeComponent } from './component/home/home.component';
import { CreateReadingListComponent } from './component/create-reading-list/create-reading-list.component';
import {ReadingListOverviewComponent} from "./component/reading-list-overview/reading-list-overview.component";

export const routes: Routes = [
    { path: '', component: HomeComponent },
  {path: 'reading-list', component: ReadingListOverviewComponent},
    { path: 'reading-list/create', component: CreateReadingListComponent },

];
