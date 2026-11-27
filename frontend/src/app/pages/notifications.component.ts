import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ApiService } from '../core/api.service';
import { RealtimeService } from '../core/realtime.service';
import { AppNotification } from '../core/models';

@Component({
  selector: 'app-notifications', standalone: true, imports: [DatePipe],
  template: `
  <div class="page" style="max-width:800px">
    <div class="row spread"><h1>Notifications</h1><button class="btn ghost small" (click)="readAll()">Mark all as read</button></div>
    @for (n of items(); track n.id) {
      <div class="card" style="margin:8px 0;cursor:pointer" [style.opacity]="n.read ? .6 : 1" (click)="open(n)">
        <div class="row spread"><span>{{ n.message }}</span>@if (!n.read) { <span class="badge">new</span> }</div>
        <small class="muted">{{ n.createdAt | date:'medium' }}</small></div>
    } @empty { <div class="empty">You are all caught up.</div> }
  </div>`
})
export class NotificationsComponent implements OnInit, OnDestroy {
  private api = inject(ApiService);
  private rt = inject(RealtimeService);
  private router = inject(Router);
  items = signal<AppNotification[]>([]);
  private sub?: Subscription;

  ngOnInit() { this.load(); this.sub = this.rt.notifications$.subscribe(() => this.load()); }
  load() { this.api.notifications().subscribe(n => this.items.set(n)); }
  readAll() { this.api.markAllRead().subscribe(() => this.load()); }
  open(n: AppNotification) {
    this.api.markRead(n.id).subscribe(() => { this.load(); if (n.link) this.router.navigateByUrl(n.link); });
  }
  ngOnDestroy() { this.sub?.unsubscribe(); }
}
