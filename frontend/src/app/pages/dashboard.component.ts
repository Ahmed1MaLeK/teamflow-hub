import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, KeyValuePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Dashboard, Insights, STATUS_LABEL } from '../core/models';

@Component({
  selector: 'app-dashboard', standalone: true, imports: [RouterLink, DatePipe, KeyValuePipe],
  template: `
  <div class="page">
    <h1>Hello, {{ auth.user()?.name }}</h1>
    <p class="muted">Here is what is happening with your projects.</p>
    <div class="row" style="margin-bottom:16px">
      <button class="btn" [class.ghost]="tab() !== 'overview'" (click)="tab.set('overview')">Overview</button>
      <button class="btn" [class.ghost]="tab() !== 'insights'" (click)="openInsights()">Insights</button>
    </div>
    @if (tab() === 'overview' && d(); as d) {
      <div class="grid c4">
        <div class="card"><div class="muted">Projects</div><div class="stat">{{ d.projects }}</div></div>
        <div class="card"><div class="muted">Open tasks</div><div class="stat">{{ d.pendingTasks }}</div></div>
        <div class="card"><div class="muted">Completed tasks</div><div class="stat">{{ d.completedTasks }}</div></div>
        <div class="card"><div class="muted">Efficiency</div><div class="stat">{{ d.efficiencyPercent }}%</div></div>
      </div>
      <div class="grid c2" style="margin-top:16px">
        <div class="card"><h2>Completed tasks, last 7 days</h2>
          <div class="bars">@for (e of d.completedPerDay | keyvalue; track e.key) {
            <div class="bar"><div class="fill" [style.height.px]="e.value * 24 + 2"></div><small>{{ e.key.slice(5) }}</small><b>{{ e.value }}</b></div> }</div></div>
        <div class="card"><h2>Tasks by status</h2>
          @for (e of d.tasksByStatus | keyvalue; track e.key) {
            <div class="row spread" style="margin:6px 0"><span class="badge {{ e.key }}">{{ label(e.key) }}</span><b>{{ e.value }}</b></div> }</div>
      </div>
      <h2 style="margin-top:20px">Recent projects</h2>
      <div class="grid c3">
        @for (p of d.recentProjects; track p.id) {
          <a class="card" [routerLink]="['/projects', p.id]"><b>{{ p.name }}</b>
            <div class="progress" style="margin:10px 0"><div [style.width.%]="p.progress"></div></div>
            <span class="muted">{{ p.progress }}% · {{ p.completedTasks }}/{{ p.totalTasks }} tasks</span></a>
        } @empty { <div class="empty">No projects yet.</div> }
      </div>
      <div class="grid c2" style="margin-top:20px">
        <div class="card"><h2>Team performance</h2>
          <table><tr><th>Member</th><th>Rating</th><th>Completion</th><th>Tasks</th></tr>
          @for (m of d.performance; track m.user.id) {
            <tr><td>{{ m.user.name }}</td><td>{{ m.avgRating ?? '-' }}</td><td>{{ m.completionPercent }}%</td><td>{{ m.completed }}/{{ m.assigned }}</td></tr>
          } @empty { <tr><td colspan="4" class="muted">No data yet.</td></tr> }</table></div>
        <div class="card"><h2>Recent activity</h2>
          @for (a of d.recentActivity; track a.taskId) {
            <div class="row spread" style="margin:8px 0"><div><a [routerLink]="['/tasks', a.taskId]">{{ a.title }}</a><br><small class="muted">{{ a.projectName }} · {{ a.at | date:'short' }}</small></div>
            <span class="badge {{ a.status }}">{{ label(a.status) }}</span></div>
          } @empty { <div class="empty">Nothing yet.</div> }</div>
      </div>
    }
    @if (tab() === 'insights') {
      <div class="card">
        <h2>Performance insights</h2>
        @if (ins(); as i) {
          <p>{{ i.summary }}</p>
          @if (i.highlights.length) { <h3>Highlights</h3><ul>@for (h of i.highlights; track h) { <li>{{ h }}</li> }</ul> }
          <h3>Recommendations</h3><ul>@for (r of i.recommendations; track r) { <li>{{ r }}</li> }</ul>
        } @else { <p class="muted">Loading...</p> }
      </div>
    }
  </div>`,
  styles: [`
    .bars { display: flex; gap: 10px; align-items: flex-end; height: 160px; }
    .bar { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; gap: 2px; }
    .fill { width: 100%; background: var(--primary); border-radius: 6px 6px 0 0; min-height: 2px; }
    a.card { color: inherit; display: block; }
  `]
})
export class DashboardComponent implements OnInit {
  auth = inject(AuthService);
  private api = inject(ApiService);
  d = signal<Dashboard | null>(null);
  ins = signal<Insights | null>(null);
  tab = signal<'overview' | 'insights'>('overview');

  label(s: string) { return (STATUS_LABEL as Record<string, string>)[s] ?? s; }
  ngOnInit() { this.api.dashboard().subscribe(d => this.d.set(d)); }
  openInsights() { this.tab.set('insights'); this.api.insights().subscribe(i => this.ins.set(i)); }
}
