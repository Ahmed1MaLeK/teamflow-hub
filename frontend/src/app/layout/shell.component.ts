import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { ApiService } from '../core/api.service';
import { RealtimeService } from '../core/realtime.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
  <div class="shell">
    <aside>
      <div class="brand">TeamFlow Hub</div>
      <nav>
        <a routerLink="/dashboard" routerLinkActive="on">Dashboard</a>
        <a routerLink="/projects" routerLinkActive="on">Projects</a>
        <a routerLink="/tasks" routerLinkActive="on">Tasks</a>
        <a routerLink="/team" routerLinkActive="on">Team</a>
        <a routerLink="/messages" routerLinkActive="on">Messages</a>
        <a routerLink="/notifications" routerLinkActive="on">Notifications @if (unread() > 0) { <span class="dot">{{ unread() }}</span> }</a>
      </nav>
      <div class="me">
        <a routerLink="/profile"><b>{{ auth.user()?.name }}</b><br><span class="muted">{{ auth.user()?.role === 'ADMIN' ? 'Manager' : 'Member' }}</span></a>
        <button class="btn ghost small" (click)="logout()">Log out</button>
      </div>
    </aside>
    <main><router-outlet /></main>
  </div>`,
  styles: [`
    .shell { display: flex; min-height: 100vh; }
    aside { width: 220px; background: #fff; border-right: 1px solid var(--border); padding: 18px 12px; display: flex; flex-direction: column; position: sticky; top: 0; height: 100vh; }
    .brand { font-weight: 800; font-size: 18px; color: var(--primary); padding: 0 10px 18px; }
    nav { display: flex; flex-direction: column; gap: 2px; flex: 1; }
    nav a { padding: 9px 12px; border-radius: 8px; color: var(--text); display: flex; justify-content: space-between; }
    nav a.on { background: var(--primary-soft); color: var(--primary); font-weight: 600; }
    .dot { background: var(--bad); color: #fff; border-radius: 99px; font-size: 11px; padding: 0 7px; }
    .me { display: flex; flex-direction: column; gap: 8px; padding: 10px; border-top: 1px solid var(--border); }
    .me a { color: var(--text); }
    main { flex: 1; min-width: 0; }
    @media (max-width: 800px) { aside { width: 150px; } }
  `]
})
export class ShellComponent implements OnInit, OnDestroy {
  auth = inject(AuthService);
  private api = inject(ApiService);
  private rt = inject(RealtimeService);
  private router = inject(Router);
  unread = signal(0);
  private sub?: Subscription;
  private nav?: Subscription;

  ngOnInit() {
    this.refresh();
    this.rt.connect();
    this.sub = this.rt.notifications$.subscribe(() => this.refresh());
    this.nav = this.router.events.subscribe(() => this.refresh());
  }

  refresh() { this.api.unread().subscribe(r => this.unread.set(r.count)); }

  logout() { this.rt.disconnect(); this.auth.logout(); }

  ngOnDestroy() { this.sub?.unsubscribe(); this.nav?.unsubscribe(); }
}
