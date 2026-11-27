import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: 'login', canActivate: [guestGuard], loadComponent: () => import('./pages/login.component').then(m => m.LoginComponent) },
  { path: 'register', canActivate: [guestGuard], loadComponent: () => import('./pages/register.component').then(m => m.RegisterComponent) },
  { path: 'forgot-password', canActivate: [guestGuard], loadComponent: () => import('./pages/forgot.component').then(m => m.ForgotComponent) },
  {
    path: '', canActivate: [authGuard], loadComponent: () => import('./layout/shell.component').then(m => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', loadComponent: () => import('./pages/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'projects', loadComponent: () => import('./pages/projects.component').then(m => m.ProjectsComponent) },
      { path: 'projects/:id', loadComponent: () => import('./pages/project-detail.component').then(m => m.ProjectDetailComponent) },
      { path: 'tasks', loadComponent: () => import('./pages/tasks.component').then(m => m.TasksComponent) },
      { path: 'tasks/:id', loadComponent: () => import('./pages/task-detail.component').then(m => m.TaskDetailComponent) },
      { path: 'team', loadComponent: () => import('./pages/team.component').then(m => m.TeamComponent) },
      { path: 'messages', loadComponent: () => import('./pages/messages.component').then(m => m.MessagesComponent) },
      { path: 'notifications', loadComponent: () => import('./pages/notifications.component').then(m => m.NotificationsComponent) },
      { path: 'profile', loadComponent: () => import('./pages/profile.component').then(m => m.ProfileComponent) },
    ]
  },
  { path: '**', redirectTo: '' },
];
