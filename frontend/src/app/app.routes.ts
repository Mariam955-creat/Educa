import { Routes } from '@angular/router';

import { authGuard, roleGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'login',
    loadComponent: () => import('./feature/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () => import('./feature/auth/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./feature/dashboard/learner-dashboard.component').then((m) => m.LearnerDashboardComponent),
  },
  {
    path: 'catalog',
    canActivate: [authGuard],
    loadComponent: () => import('./feature/catalog/catalog.component').then((m) => m.CatalogComponent),
  },
  {
    path: 'courses/:slug',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./feature/course/course-detail.component').then((m) => m.CourseDetailComponent),
  },
  {
    path: 'instructor',
    canActivate: [roleGuard('INSTRUCTOR', 'ADMIN')],
    loadComponent: () =>
      import('./feature/instructor/instructor-dashboard.component').then(
        (m) => m.InstructorDashboardComponent,
      ),
  },
  {
    path: 'instructor/courses/new',
    canActivate: [roleGuard('INSTRUCTOR', 'ADMIN')],
    loadComponent: () =>
      import('./feature/instructor/course-editor.component').then((m) => m.CourseEditorComponent),
  },
  {
    path: 'instructor/courses/:slug/edit',
    canActivate: [roleGuard('INSTRUCTOR', 'ADMIN')],
    loadComponent: () =>
      import('./feature/instructor/course-editor.component').then((m) => m.CourseEditorComponent),
  },
  {
    path: 'admin',
    canActivate: [roleGuard('ADMIN')],
    loadComponent: () =>
      import('./feature/admin/admin-dashboard.component').then((m) => m.AdminDashboardComponent),
  },
  { path: '**', redirectTo: 'dashboard' },
];
