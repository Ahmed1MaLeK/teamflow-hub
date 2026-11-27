import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { errorMessage } from '../core/auth.interceptor';
import { Attachment, Project, STATUS_LABEL, TASK_STATUSES, Task, Team } from '../core/models';

@Component({
  selector: 'app-project-detail', standalone: true, imports: [FormsModule, RouterLink, DatePipe],
  template: `
  <div class="page">
    <a routerLink="/projects">&larr; Back to projects</a>
    @if (p(); as p) {
      <div class="row spread" style="margin-top:8px"><div><h1>{{ p.name }}</h1><span class="badge">{{ p.status }}</span>
        <span class="muted"> Manager: {{ p.manager.name }} · {{ p.startDate || '?' }} to {{ p.endDate || '?' }}</span></div>
        @if (isManager()) { <button class="btn danger" (click)="remove()">Delete project</button> }</div>
      <p>{{ p.description }}</p>
      <div class="progress"><div [style.width.%]="p.progress"></div></div>
      <small class="muted">{{ p.progress }}% complete ({{ p.completedTasks }}/{{ p.totalTasks }})</small>
      @if (error()) { <div class="error">{{ error() }}</div> }

      <div class="grid c2" style="margin-top:16px;grid-template-columns:2fr 1fr">
        <div>
          <div class="row spread"><h2>Task board</h2>
            @if (isManager()) { <button class="btn small" (click)="showTask.set(!showTask())">{{ showTask() ? 'Cancel' : 'New task' }}</button> }</div>
          @if (showTask()) {
            <form class="card" (ngSubmit)="addTask()" style="margin-bottom:12px">
              <label>Title</label><input name="t" [(ngModel)]="nt.title" required>
              <label>Description</label><textarea name="d" rows="2" [(ngModel)]="nt.description"></textarea>
              <div class="grid c3"><div><label>Start</label><input type="date" name="s" [(ngModel)]="nt.startDate"></div>
                <div><label>Deadline</label><input type="date" name="dl" [(ngModel)]="nt.deadline"></div>
                <div><label>Assign to</label><select name="a" [(ngModel)]="nt.assignedToId"><option [ngValue]="null">Unassigned</option>
                  <option [ngValue]="p.manager.id">{{ p.manager.name }} (manager)</option>
                  @for (m of p.members; track m.id) { <option [ngValue]="m.id">{{ m.name }}</option> }</select></div></div>
              <br><button class="btn">Create task</button>
            </form>
          }
          <div class="board">
            @for (s of statuses; track s) {
              <div class="col"><div class="row spread"><span class="badge {{ s }}">{{ label(s) }}</span><small class="muted">{{ byStatus(s).length }}</small></div>
                @for (t of byStatus(s); track t.id) {
                  <a class="card task" [routerLink]="['/tasks', t.id]"><b>{{ t.title }}</b>
                    <small class="muted">{{ t.assignedTo?.name || 'Unassigned' }}{{ t.deadline ? ' · due ' + t.deadline : '' }}</small></a>
                }</div>
            }
          </div>
        </div>
        <div>
          <div class="card"><h2>Team</h2>
            <div class="row wrap">@for (m of p.members; track m.id) {
              <span class="chip">&#64;{{ m.username }} @if (isManager()) { <button title="Remove" (click)="removeMember(m.id)">&times;</button> }</span>
            } @empty { <span class="muted">No members yet.</span> }</div>
            @if (isManager()) {
              <label>Add member by username</label>
              <div class="row"><input [(ngModel)]="username" placeholder="username"><button class="btn small" (click)="addMember()">Add</button></div>
              @if (teams().length) {
                <label>Add a whole team</label>
                <div class="row"><select [(ngModel)]="teamId"><option [ngValue]="null">Choose team</option>
                  @for (t of teams(); track t.id) { <option [ngValue]="t.id">{{ t.name }}</option> }</select>
                  <button class="btn small" (click)="addTeam()">Add</button></div>
              }
            }
          </div>
          <div class="card" style="margin-top:16px"><h2>Files</h2>
            @for (a of files(); track a.id) {
              <div class="row spread" style="margin:6px 0"><a href="javascript:void 0" (click)="download(a)">{{ a.fileName }}</a><small class="muted">{{ a.createdAt | date:'shortDate' }}</small></div>
            } @empty { <span class="muted">No files attached.</span> }
            @if (isManager()) { <br><input type="file" (change)="upload($event)"> }
          </div>
        </div>
      </div>
    }
  </div>`,
  styles: [`
    .board { display: grid; grid-template-columns: repeat(5, minmax(130px, 1fr)); gap: 10px; overflow-x: auto; }
    .col { background: #eef0f6; border-radius: 10px; padding: 8px; min-height: 120px; display: flex; flex-direction: column; gap: 8px; }
    .task { padding: 10px; display: flex; flex-direction: column; gap: 4px; color: inherit; }
  `]
})
export class ProjectDetailComponent implements OnInit {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private auth = inject(AuthService);
  p = signal<Project | null>(null);
  tasks = signal<Task[]>([]);
  files = signal<Attachment[]>([]);
  teams = signal<Team[]>([]);
  error = signal(''); showTask = signal(false);
  statuses = TASK_STATUSES;
  username = ''; teamId: number | null = null;
  nt: any = { title: '', description: '', startDate: null, deadline: null, assignedToId: null };
  private id = 0;

  label(s: string) { return (STATUS_LABEL as Record<string, string>)[s]; }
  byStatus(s: string) { return this.tasks().filter(t => t.status === s); }
  isManager() { return this.p()?.manager.id === this.auth.user()?.id; }

  ngOnInit() {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  load() {
    this.api.project(this.id).subscribe({ next: p => { this.p.set(p); if (this.isManager()) this.api.teams().subscribe(t => this.teams.set(t)); }, error: e => this.error.set(errorMessage(e)) });
    this.api.tasks(this.id).subscribe(t => this.tasks.set(t));
    this.api.projectFiles(this.id).subscribe(f => this.files.set(f));
  }

  addTask() {
    this.error.set('');
    const b = { ...this.nt, projectId: this.id, startDate: this.nt.startDate || null, deadline: this.nt.deadline || null };
    this.api.createTask(b).subscribe({
      next: () => { this.showTask.set(false); this.nt = { title: '', description: '', startDate: null, deadline: null, assignedToId: null }; this.load(); },
      error: e => this.error.set(errorMessage(e))
    });
  }
  addMember() {
    this.error.set('');
    this.api.addMember(this.id, this.username).subscribe({ next: p => { this.p.set(p); this.username = ''; }, error: e => this.error.set(errorMessage(e)) });
  }
  removeMember(uid: number) { this.api.removeMember(this.id, uid).subscribe({ next: p => { this.p.set(p); this.load(); }, error: e => this.error.set(errorMessage(e)) }); }
  addTeam() { if (this.teamId) this.api.addTeamToProject(this.id, this.teamId).subscribe({ next: p => this.p.set(p), error: e => this.error.set(errorMessage(e)) }); }
  remove() { if (confirm('Delete this project and all its tasks?')) this.api.deleteProject(this.id).subscribe(() => this.router.navigate(['/projects'])); }
  upload(ev: Event) {
    const file = (ev.target as HTMLInputElement).files?.[0];
    if (file) this.api.uploadProjectFile(this.id, file).subscribe({ next: () => this.api.projectFiles(this.id).subscribe(f => this.files.set(f)), error: e => this.error.set(errorMessage(e)) });
  }
  download(a: Attachment) {
    this.api.downloadFile(a.id).subscribe(blob => {
      const url = URL.createObjectURL(blob); const el = document.createElement('a');
      el.href = url; el.download = a.fileName; el.click(); URL.revokeObjectURL(url);
    });
  }
}
