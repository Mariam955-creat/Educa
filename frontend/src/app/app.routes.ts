import { Routes } from '@angular/router';

import { authGuard, roleGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'account' },
  {
    path: 'login',
    loadComponent: () => import('./feature/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () => import('./feature/auth/register.component').then((m) => m.RegisterComponent),
  },
  // Anciennes adresses : leurs pages vivent désormais dans l'espace « Mon compte »
  { path: 'dashboard', pathMatch: 'full', redirectTo: 'account/courses' },
  { path: 'certificates', pathMatch: 'full', redirectTo: 'account/certificates' },
  { path: 'invoices', pathMatch: 'full', redirectTo: 'account/invoices' },
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
    path: 'account',
    canActivate: [authGuard],
    loadComponent: () => import('./feature/account/account-layout.component').then((m) => m.AccountLayoutComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'overview' },
      {
        path: 'overview',
        loadComponent: () =>
          import('./feature/account/account-overview.component').then((m) => m.AccountOverviewComponent),
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./feature/account/account-profile.component').then((m) => m.AccountProfileComponent),
      },
      {
        path: 'courses',
        loadComponent: () =>
          import('./feature/dashboard/learner-dashboard.component').then((m) => m.LearnerDashboardComponent),
      },
      {
        path: 'certificates',
        loadComponent: () =>
          import('./feature/certificate/my-certificates.component').then((m) => m.MyCertificatesComponent),
      },
      {
        path: 'invoices',
        loadComponent: () => import('./feature/payment/my-invoices.component').then((m) => m.MyInvoicesComponent),
      },
      {
        path: 'reviews',
        loadComponent: () => import('./feature/account/my-reviews.component').then((m) => m.MyReviewsComponent),
      },
    ],
  },
  {
    path: 'verify/:code',
    loadComponent: () => import('./feature/certificate/verify.component').then((m) => m.VerifyComponent),
  },
  {
    path: 'instructor',
    canActivate: [roleGuard('INSTRUCTOR', 'ADMIN')],
    loadComponent: () =>
      import('./feature/instructor/instructor-layout.component').then((m) => m.InstructorLayoutComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'overview' },
      {
        path: 'overview',
        loadComponent: () =>
          import('./feature/instructor/instructor-overview.component').then((m) => m.InstructorOverviewComponent),
      },
      {
        path: 'courses',
        loadComponent: () =>
          import('./feature/instructor/instructor-courses.component').then((m) => m.InstructorCoursesComponent),
      },
      {
        path: 'courses/new',
        loadComponent: () =>
          import('./feature/instructor/course-editor.component').then((m) => m.CourseEditorComponent),
      },
      {
        path: 'courses/:slug/edit',
        loadComponent: () =>
          import('./feature/instructor/course-editor.component').then((m) => m.CourseEditorComponent),
      },
      {
        path: 'courses/:slug/results',
        loadComponent: () =>
          import('./feature/instructor/course-results.component').then((m) => m.CourseResultsComponent),
      },
      {
        path: 'quizzes/:id/edit',
        loadComponent: () =>
          import('./feature/instructor/quiz-editor.component').then((m) => m.QuizEditorComponent),
      },
      {
        path: 'sales',
        loadComponent: () =>
          import('./feature/instructor/instructor-sales.component').then((m) => m.InstructorSalesComponent),
      },
      {
        path: 'reviews',
        loadComponent: () =>
          import('./feature/instructor/instructor-reviews.component').then((m) => m.InstructorReviewsComponent),
      },
    ],
  },
  {
    path: 'admin',
    canActivate: [roleGuard('ADMIN')],
    loadComponent: () =>
      import('./feature/admin/admin-dashboard.component').then((m) => m.AdminDashboardComponent),
  },
  { path: '**', redirectTo: 'account' },
];
