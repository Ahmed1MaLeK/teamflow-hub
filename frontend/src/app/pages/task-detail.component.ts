import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { errorMessage } from '../core/auth.interceptor';
import { Attachment, STATUS_LABEL, Task } from '../core/models';

@Component({
  selector: 'app-task-detail', standalone: true, imports: [FormsModule, RouterLink, DatePipe],
  template: `
  <div class="page" style="max-width:900px">
    @if (t(); as t) {
      <a [routerLink]="['/projects', t.projectId]">&larr; Back to {{ t.projectName }}</a>
      <div class="row spread" style="margin-top:8px"><h1>{{ t.title }}</h1><span class="badge {{ t.status }}">{{ label(t.status) }}</span></div>
      <p class="muted">Assigned to {{ t.assignedTo?.name || 'nobody' }} · {{ t.startDate || '?' }} to {{ t.deadline || '?' }}</p>
      <div class="card"><h3>Description</h3><p>{{ t.description || 'No description.' }}</p></div>
      @if (error()) { <div class="error">{{ error() }}</div> }

      @if (t.reviewFeedback) { <div class="card" style="margin-top:12px"><h3>Review feedback</h3><p>{{ t.reviewFeedback }}</p>
        @if (t.rating) { <b>Rating: {{ t.rating }}/5</b> }</div> }

      <div class="card" style="margin-top:12px"><h3>Actions</h3>
        @if (isAssignee() && t.status === 'PENDING') { <button class="btn" (click)="move('IN_PROGRESS')">Start work</button> }
        @if (isAssignee() && t.status === 'IN_PROGRESS') { <button class="btn" (click)="move('REVIEWING')">Submit for review</button> }
        @if (isManager() && t.status === 'ACCEPTED') { <button class="btn" (click)="move('DONE')">Mark as done</button> }
        @if (isManager() && t.status === 'REVIEWING') {
          <label>Feedback (required for rework)</label><textarea rows="3" [(ngModel)]="feedback"></textarea>
          <label>Quality rating (1-5, required to accept)</label><select style="max-width:120px" [(ngModel)]="rating"><option [ngValue]="null">-</option>
            @for (r of [1,2,3,4,5]; track r) { <option [ngValue]="r">{{ r }}</option> }</select>
          <br><div class="row"><button class="btn ok" (click)="review(true)">Accept</button><button class="btn danger" (click)="review(false)">Reject and request rework</button></div>
        }
        @if (isManager()) { <br><button class="btn danger small" (click)="remove()">Delete task</button> }
        @if (!isManager() && !isAssignee()) { <span class="muted">No actions available for you.</span> }
      </div>

      <div class="grid c2" style="margin-top:12px">
        <div class="card"><h3>Guidelines</h3>@for (a of of('GUIDELINE'); track a.id) { <div><a href="javascript:void 0" (click)="download(a)">{{ a.fileName }}</a></div> } @empty { <span class="muted">None.</span> }
          @if (isManager()) { <br><input type="file" (change)="upload($event, 'GUIDELINE')"> }</div>
        <div class="card"><h3>Deliverables</h3>@for (a of of('DELIVERABLE'); track a.id) { <div><a href="javascript:void 0" (click)="download(a)">{{ a.fileName }}</a> <small class="muted">{{ a.createdAt | date:'short' }}</small></div> } @empty { <span class="muted">None.</span> }
          @if (isAssignee()) { <br><input type="file" (change)="upload($event, 'DELIVERABLE')"> }</div>
      </div>
    }
  </div>`
})
export class TaskDetailComponent implements OnInit {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private auth = inject(AuthService);
  t = signal<Task | null>(null);
  files = signal<Attachment[]>([]);
  managerId = signal<number | null>(null);
  error = signal('');
  feedback = ''; rating: number | null = null;
  private id = 0;

  label(s: string) { return (STATUS_LABEL as Record<string, string>)[s]; }
  of(kind: string) { return this.files().filter(f => f.kind === kind); }
  isAssignee() { return this.t()?.assignedTo?.id === this.auth.user()?.id; }
  isManager() { return this.managerId() === this.auth.user()?.id; }

  ngOnInit() { this.id = Number(this.route.snapshot.paramMap.get('id')); this.load(); }

  load() {
    this.api.task(this.id).subscribe({
      next: t => { this.t.set(t); this.api.project(t.projectId).subscribe(p => this.managerId.set(p.manager.id)); },
      error: e => this.error.set(errorMessage(e))
    });
    this.api.taskFiles(this.id).subscribe(f => this.files.set(f));
  }
  move(s: any) { this.error.set(''); this.api.setStatus(this.id, s).subscribe({ next: t => this.t.set(t), error: e => this.error.set(errorMessage(e)) }); }
  review(accept: boolean) {
    this.error.set('');
    this.api.review(this.id, accept, this.rating, this.feedback).subscribe({ next: t => { this.t.set(t); this.feedback = ''; this.rating = null; }, error: e => this.error.set(errorMessage(e)) });
  }
  remove() { if (confirm('Delete this task?')) this.api.deleteTask(this.id).subscribe(() => this.router.navigate(['/projects', this.t()!.projectId])); }
  upload(ev: Event, kind: 'GUIDELINE' | 'DELIVERABLE') {
    const file = (ev.target as HTMLInputElement).files?.[0];
    if (file) this.api.uploadTaskFile(this.id, kind, file).subscribe({ next: () => this.api.taskFiles(this.id).subscribe(f => this.files.set(f)), error: e => this.error.set(errorMessage(e)) });
  }
  download(a: Attachment) {
    this.api.downloadFile(a.id).subscribe(blob => {
      const url = URL.createObjectURL(blob); const el = document.createElement('a');
      el.href = url; el.download = a.fileName; el.click(); URL.revokeObjectURL(url);
    });
  }
}
