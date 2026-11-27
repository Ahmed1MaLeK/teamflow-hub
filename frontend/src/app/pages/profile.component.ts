import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { errorMessage } from '../core/auth.interceptor';

@Component({
  selector: 'app-profile', standalone: true, imports: [FormsModule],
  template: `
  <div class="page" style="max-width:520px">
    <h1>Profile</h1>
    <form class="card" (ngSubmit)="save()">
      <label>Name</label><input name="n" [(ngModel)]="name" required>
      <label>Username</label><input [value]="auth.user()?.username" disabled>
      <label>Email</label><input [value]="auth.user()?.email" disabled>
      <label>Role</label><input [value]="auth.user()?.role === 'ADMIN' ? 'Manager' : 'Member'" disabled>
      <label>New password (leave empty to keep the current one)</label><input type="password" name="p" [(ngModel)]="password" minlength="6">
      @if (error()) { <div class="error">{{ error() }}</div> }
      @if (ok()) { <div class="info">Saved.</div> }
      <br><button class="btn">Save changes</button>
    </form>
  </div>`
})
export class ProfileComponent {
  auth = inject(AuthService);
  private api = inject(ApiService);
  name = this.auth.user()?.name ?? ''; password = '';
  error = signal(''); ok = signal(false);

  save() {
    this.error.set(''); this.ok.set(false);
    this.api.updateProfile(this.name, this.password).subscribe({
      next: u => { this.auth.setUser(u); this.password = ''; this.ok.set(true); },
      error: e => this.error.set(errorMessage(e))
    });
  }
}
