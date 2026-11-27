import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { errorMessage } from '../core/auth.interceptor';

@Component({
  selector: 'app-login', standalone: true, imports: [FormsModule, RouterLink],
  template: `
  <div class="auth-wrap"><form class="card auth-card" (ngSubmit)="submit()">
    <h1>Welcome back</h1><p class="muted">Log in to TeamFlow Hub</p>
    <label>Email</label><input type="email" name="email" [(ngModel)]="email" required autocomplete="username">
    <label>Password</label><input type="password" name="password" [(ngModel)]="password" required autocomplete="current-password">
    @if (error()) { <div class="error">{{ error() }}</div> }
    <p><a routerLink="/forgot-password">Forgot password?</a></p>
    <button class="btn" style="width:100%" [disabled]="busy()">Log in</button>
    <p class="muted">No account? <a routerLink="/register">Sign up</a></p>
  </form></div>`
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  email = ''; password = '';
  error = signal(''); busy = signal(false);

  submit() {
    this.busy.set(true); this.error.set('');
    this.auth.login(this.email, this.password).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: e => { this.error.set(errorMessage(e, 'Login failed')); this.busy.set(false); }
    });
  }
}
