import { Routes } from '@angular/router';
import { authGuard } from '../guards/authentication/auth.guard';
import { staffOnlyGuard } from '../guards/authentication/staff-only.guard';

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
