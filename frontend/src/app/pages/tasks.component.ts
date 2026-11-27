import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { STATUS_LABEL, TASK_STATUSES, Task } from '../core/models';

@Component({
  selector: 'app-tasks', standalone: true, imports: [FormsModule, RouterLink],
  template: `
  <div class="page">
    <h1>All tasks</h1><p class="muted">Tasks from every project you are part of.</p>
    <div class="row" style="margin-bottom:12px"><input style="max-width:260px" placeholder="Search..." [(ngModel)]="q">
      <select style="max-width:180px" [(ngModel)]="status"><option value="">All statuses</option>
        @for (s of statuses; track s) { <option [value]="s">{{ label(s) }}</option> }</select></div>
    <div class="card"><table>
      <tr><th>Task</th><th>Project</th><th>Assignee</th><th>Deadline</th><th>Status</th></tr>
      @for (t of filtered(); track t.id) {
        <tr><td><a [routerLink]="['/tasks', t.id]">{{ t.title }}</a></td><td>{{ t.projectName }}</td><td>{{ t.assignedTo?.name || '-' }}</td>
          <td>{{ t.deadline || '-' }}</td><td><span class="badge {{ t.status }}">{{ label(t.status) }}</span></td></tr>
      } @empty { <tr><td colspan="5" class="muted">No tasks found.</td></tr> }
    </table></div>
  </div>`
})
export class TasksComponent implements OnInit {
  private api = inject(ApiService);
  tasks = signal<Task[]>([]);
  statuses = TASK_STATUSES; q = ''; status = '';
  label(s: string) { return (STATUS_LABEL as Record<string, string>)[s]; }
  ngOnInit() { this.api.tasks().subscribe(t => this.tasks.set(t)); }
  filtered() {
    const q = this.q.toLowerCase();
    return this.tasks().filter(t => (!this.status || t.status === this.status) && (!q || (t.title + t.projectName + (t.assignedTo?.name ?? '')).toLowerCase().includes(q)));
  }
}
