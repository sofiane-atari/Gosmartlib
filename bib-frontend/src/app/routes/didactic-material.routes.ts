import { Routes } from '@angular/router';
import { authGuard } from '../guards/authentication/auth.guard';
import { staffOnlyGuard } from '../guards/authentication/staff-only.guard';
import { DidacticMaterialOverviewComponent } from '../component/didactic/didactic-material-overview/didactic-material-overview.component';
import { DidacticMaterialFormPageComponent } from '../component/didactic/didactic-material-form-page/didactic-material-form-page.component';
import { DidacticMaterialDetailComponent } from '../component/didactic/didactic-material-detail/didactic-material-detail.component';

export const DIDACTIC_MATERIAL_PATHS = {
  root: 'didactisch',
  create: 'create',
  edit: 'edit',
} as const;

export const didacticMaterialRoutes: Routes = [
  {
    path: DIDACTIC_MATERIAL_PATHS.root,
    canActivate: [authGuard, staffOnlyGuard],
    children: [
      {
        path: '',
        component: DidacticMaterialOverviewComponent,
      },
      {
        path: DIDACTIC_MATERIAL_PATHS.create,
        component: DidacticMaterialFormPageComponent,
      },
      {
        path: ':id',
        component: DidacticMaterialDetailComponent,
      },
      {
        path: `:id/${DIDACTIC_MATERIAL_PATHS.edit}`,
        component: DidacticMaterialFormPageComponent,
      },
    ],
  },
];
