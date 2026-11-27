import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { errorMessage } from '../core/auth.interceptor';
import { Project } from '../core/models';

@Component({
  selector: 'app-projects', standalone: true, imports: [FormsModule, RouterLink],
  template: `
  <div class="page">
    <div class="row spread"><div><h1>Projects</h1><p class="muted">All projects you manage or belong to.</p></div>
      @if (auth.isAdmin()) { <button class="btn" (click)="show.set(!show())">{{ show() ? 'Cancel' : 'New project' }}</button> }</div>
    @if (show()) {
      <form class="card" (ngSubmit)="create()" style="margin-bottom:16px">
        <label>Name</label><input name="n" [(ngModel)]="f.name" required>
        <label>Description</label><textarea name="d" rows="3" [(ngModel)]="f.description"></textarea>
        <div class="grid c2"><div><label>Start date</label><input type="date" name="s" [(ngModel)]="f.startDate"></div>
        <div><label>End date</label><input type="date" name="e" [(ngModel)]="f.endDate"></div></div>
        @if (error()) { <div class="error">{{ error() }}</div> }
        <br><button class="btn">Create project</button>
      </form>
    }
    <div class="grid c3">
      @for (p of projects(); track p.id) {
        <a class="card" [routerLink]="['/projects', p.id]" style="color:inherit">
          <div class="row spread"><b>{{ p.name }}</b><span class="badge">{{ p.status }}</span></div>
          <p class="muted">{{ p.description || 'No description' }}</p>
          <div class="progress"><div [style.width.%]="p.progress"></div></div>
          <small class="muted">{{ p.progress }}% · {{ p.completedTasks }}/{{ p.totalTasks }} tasks · {{ p.members.length }} members</small>
        </a>
      } @empty { <div class="empty" style="grid-column:1/-1">No projects yet.</div> }
    </div>
  </div>`
})
export class ProjectsComponent implements OnInit {
  auth = inject(AuthService);
  private api = inject(ApiService);
  projects = signal<Project[]>([]);
  show = signal(false); error = signal('');
  f: any = { name: '', description: '', startDate: null, endDate: null };

  ngOnInit() { this.load(); }
  load() { this.api.projects().subscribe(p => this.projects.set(p)); }
  create() {
    this.error.set('');
    this.api.createProject({ ...this.f, startDate: this.f.startDate || null, endDate: this.f.endDate || null }).subscribe({
      next: () => { this.show.set(false); this.f = { name: '', description: '', startDate: null, endDate: null }; this.load(); },
      error: e => this.error.set(errorMessage(e))
    });
  }
}
