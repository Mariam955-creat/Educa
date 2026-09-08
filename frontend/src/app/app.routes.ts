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
    path: 'quizzes/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./feature/quiz/quiz-take.component').then((m) => m.QuizTakeComponent),
  },
  {
    path: 'certificates',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./feature/certificate/my-certificates.component').then((m) => m.MyCertificatesComponent),
  },
  {
    path: 'verify/:code',
    loadComponent: () => import('./feature/certificate/verify.component').then((m) => m.VerifyComponent),
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
    path: 'instructor/courses/:slug/results',
    canActivate: [roleGuard('INSTRUCTOR', 'ADMIN')],
    loadComponent: () =>
      import('./feature/instructor/course-results.component').then((m) => m.CourseResultsComponent),
  },
  {
    path: 'instructor/quizzes/:id/edit',
    canActivate: [roleGuard('INSTRUCTOR', 'ADMIN')],
    loadComponent: () =>
      import('./feature/instructor/quiz-editor.component').then((m) => m.QuizEditorComponent),
  },
  {
    path: 'admin',
    canActivate: [roleGuard('ADMIN')],
    loadComponent: () =>
      import('./feature/admin/admin-dashboard.component').then((m) => m.AdminDashboardComponent),
  },
  { path: '**', redirectTo: 'dashboard' },
];
