import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { errorMessage } from '../core/auth.interceptor';

@Component({
  selector: 'app-forgot', standalone: true, imports: [FormsModule, RouterLink],
  template: `
  <div class="auth-wrap"><div class="card auth-card">
    <h1>Reset password</h1>
    @if (step() === 1) {
      <p class="muted">Enter your email to get a reset token.</p>
      <form (ngSubmit)="request()">
        <label>Email</label><input type="email" name="email" [(ngModel)]="email" required>
        <br><button class="btn" style="width:100%">Send reset token</button>
      </form>
    } @else {
      @if (info()) { <div class="info">{{ info() }}</div> }
      <form (ngSubmit)="reset()">
        <label>Reset token</label><input name="token" [(ngModel)]="token" required>
        <label>New password</label><input type="password" name="pw" [(ngModel)]="password" required minlength="6">
        <br><button class="btn" style="width:100%">Set new password</button>
      </form>
    }
    @if (error()) { <div class="error">{{ error() }}</div> }
    @if (done()) { <div class="info">Password updated. You can log in now.</div> }
    <p><a routerLink="/login">Back to login</a></p>
  </div></div>`
})
export class ForgotComponent {
  private api = inject(ApiService);
  step = signal(1); error = signal(''); info = signal(''); done = signal(false);
  email = ''; token = ''; password = '';

  request() {
    this.error.set('');
    this.api.forgot(this.email).subscribe({
      next: r => {
        // No mail server in this project: the backend returns the token (and logs it) for local development.
        if (r.resetToken) this.token = r.resetToken;
        this.info.set(r.resetToken ? 'Development mode: your token was filled in below (it is also printed in the backend console).' : r.message);
        this.step.set(2);
      },
      error: e => this.error.set(errorMessage(e))
    });
  }

  reset() {
    this.error.set('');
    this.api.reset(this.token, this.password).subscribe({ next: () => this.done.set(true), error: e => this.error.set(errorMessage(e)) });
  }
}
