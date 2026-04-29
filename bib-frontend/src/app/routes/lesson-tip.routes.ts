import { Routes } from '@angular/router';
import { authGuard } from '../guards/authentication/auth.guard';
import { staffOnlyGuard } from '../guards/authentication/staff-only.guard';
import { LessonTipOverviewComponent } from '../component/lesson-tip/lesson-tip-overview/lesson-tip-overview.component';
import { LessonTipFormPageComponent } from '../component/lesson-tip/lesson-tip-form-page/lesson-tip-form-page.component';
import { LessonTipDetailComponent } from '../component/lesson-tip/lesson-tip-detail/lesson-tip-detail.component';

export const LESSON_TIP_PATHS = {
  root: 'lesson',
  create: 'create',
  edit: 'edit',
} as const;

export const LessonTipRoutes: Routes = [
  {
    path: LESSON_TIP_PATHS.root,
    canActivate: [authGuard, staffOnlyGuard],
    children: [
      {
        path: '',
        component: LessonTipOverviewComponent,
      },
      {
        path: LESSON_TIP_PATHS.create,
        component: LessonTipFormPageComponent,
      },
      {
        path: ':id',
        component: LessonTipDetailComponent,
      },
      {
        path: `:id/${LESSON_TIP_PATHS.edit}`,
        component: LessonTipFormPageComponent,
      },
    ],
  },
];
