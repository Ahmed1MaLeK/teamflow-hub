import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { API_URL } from './config';
import { AuthResponse, User } from './models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  readonly token = signal<string | null>(localStorage.getItem('tf_token'));
  readonly user = signal<User | null>(JSON.parse(localStorage.getItem('tf_user') ?? 'null'));
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');
  readonly loggedIn = computed(() => !!this.token());

  login(email: string, password: string) {
    return this.http.post<AuthResponse>(`${API_URL}/api/auth/login`, { email, password }).pipe(tap(r => this.store(r)));
  }

  register(body: { name: string; username: string; email: string; password: string; role: string }) {
    return this.http.post<AuthResponse>(`${API_URL}/api/auth/register`, body).pipe(tap(r => this.store(r)));
  }

  setUser(u: User) {
    this.user.set(u);
    localStorage.setItem('tf_user', JSON.stringify(u));
  }

  logout() {
    this.token.set(null);
    this.user.set(null);
    localStorage.removeItem('tf_token');
    localStorage.removeItem('tf_user');
    this.router.navigate(['/login']);
  }

  private store(r: AuthResponse) {
    this.token.set(r.token);
    localStorage.setItem('tf_token', r.token);
    this.setUser(r.user);
  }
}
