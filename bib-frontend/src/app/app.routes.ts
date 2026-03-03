import { Routes } from '@angular/router';
import { DashboardRolesComponent } from './component/dashboard-roles/dashboard-roles.component';

export const routes: Routes = [

  // Als de gebruiker gewoon naar 'localhost:4200' gaat, stuur ze direct door naar het dashboard
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  
  // Koppel de URL '/dashboard' aan jouw DashboardComponent
  { path: 'dashboard', component: DashboardRolesComponent },
  
  // Hier kun je later andere pagina's toevoegen (zoals catalogus, beheer, etc.)
  // { path: 'catalog', component: CatalogComponent },
];
