import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { errorMessage } from '../core/auth.interceptor';
import { Team, User } from '../core/models';

@Component({
  selector: 'app-team', standalone: true, imports: [FormsModule],
  template: `
  <div class="page">
    <h1>Teams</h1><p class="muted">Group people into teams, then add a whole team to a project.</p>
    @if (error()) { <div class="error">{{ error() }}</div> }
    @if (auth.isAdmin()) {
      <form class="card" (ngSubmit)="create()" style="margin-bottom:16px">
        <h2>New team</h2>
        <label>Team name</label><input name="n" [(ngModel)]="name" required>
        <label>Members (hold Ctrl or Cmd to select several)</label>
        <select multiple size="6" name="m" [(ngModel)]="selected">@for (u of users(); track u.id) { <option [ngValue]="u.id">{{ u.name }} (&#64;{{ u.username }})</option> }</select>
        <br><button class="btn">Create team</button>
      </form>
    }
    <div class="grid c3">
      @for (t of teams(); track t.id) {
        <div class="card"><div class="row spread"><b>{{ t.name }}</b>@if (auth.isAdmin()) { <button class="btn danger small" (click)="remove(t)">Delete</button> }</div>
          <div class="row wrap" style="margin-top:8px">@for (m of t.members; track m.id) { <span class="chip">{{ m.name }}</span> } @empty { <span class="muted">No members</span> }</div></div>
      } @empty { <div class="empty" style="grid-column:1/-1">No teams yet.</div> }
    </div>
    <h2 style="margin-top:24px">Everyone</h2>
    <div class="card"><table><tr><th>Name</th><th>Username</th><th>Email</th><th>Role</th></tr>
      @for (u of users(); track u.id) { <tr><td>{{ u.name }}</td><td>&#64;{{ u.username }}</td><td>{{ u.email }}</td><td>{{ u.role === 'ADMIN' ? 'Manager' : 'Member' }}</td></tr> }
    </table></div>
  </div>`
})
export class TeamComponent implements OnInit {
  auth = inject(AuthService);
  private api = inject(ApiService);
  teams = signal<Team[]>([]); users = signal<User[]>([]); error = signal('');
  name = ''; selected: number[] = [];

  ngOnInit() { this.load(); this.api.users().subscribe(u => this.users.set(u)); }
  load() { this.api.teams().subscribe(t => this.teams.set(t)); }
  create() {
    this.error.set('');
    this.api.createTeam(this.name, this.selected).subscribe({ next: () => { this.name = ''; this.selected = []; this.load(); }, error: e => this.error.set(errorMessage(e)) });
  }
  remove(t: Team) { if (confirm(`Delete team ${t.name}?`)) this.api.deleteTeam(t.id).subscribe(() => this.load()); }
}
