import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { errorMessage } from '../core/auth.interceptor';

@Component({
  selector: 'app-register', standalone: true, imports: [FormsModule, RouterLink],
  template: `
  <div class="auth-wrap"><form class="card auth-card" (ngSubmit)="submit()">
    <h1>Create account</h1><p class="muted">Join TeamFlow Hub</p>
    <label>Full name</label><input name="name" [(ngModel)]="f.name" required>
    <label>Username</label><input name="username" [(ngModel)]="f.username" required minlength="3">
    <label>Email</label><input type="email" name="email" [(ngModel)]="f.email" required>
    <label>Password (min 6 characters)</label><input type="password" name="password" [(ngModel)]="f.password" required minlength="6">
    <label>I am a</label>
    <select name="role" [(ngModel)]="f.role"><option value="MEMBER">Team member</option><option value="ADMIN">Manager</option></select>
    @if (error()) { <div class="error">{{ error() }}</div> }
    <br><button class="btn" style="width:100%" [disabled]="busy()">Sign up</button>
    <p class="muted">Already registered? <a routerLink="/login">Log in</a></p>
  </form></div>`
})
export class RegisterComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  f = { name: '', username: '', email: '', password: '', role: 'MEMBER' };
  error = signal(''); busy = signal(false);

  submit() {
    this.busy.set(true); this.error.set('');
    this.auth.register(this.f).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: e => { this.error.set(errorMessage(e, 'Sign up failed')); this.busy.set(false); }
    });
  }
}
