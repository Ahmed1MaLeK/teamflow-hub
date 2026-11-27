import { Injectable, inject } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { Subject } from 'rxjs';
import { API_URL } from './config';
import { AuthService } from './auth.service';
import { AppNotification, ChatMessage } from './models';

/** Live updates over STOMP/WebSocket: notifications, direct messages and project channels. */
@Injectable({ providedIn: 'root' })
export class RealtimeService {
  private auth = inject(AuthService);
  private client?: Client;
  private pending: (() => void)[] = [];
  private subs = new Map<string, StompSubscription>();

  readonly notifications$ = new Subject<AppNotification>();
  readonly direct$ = new Subject<ChatMessage>();
  readonly project$ = new Subject<ChatMessage>();

  connect() {
    if (this.client?.active || !this.auth.token()) return;
    this.client = new Client({
      brokerURL: API_URL.replace(/^http/, 'ws') + '/ws',
      connectHeaders: { Authorization: `Bearer ${this.auth.token()}` },
      reconnectDelay: 5000,
    });
    this.client.onConnect = () => {
      this.subs.clear();
      this.client!.subscribe('/user/queue/notifications', m => this.notifications$.next(JSON.parse(m.body)));
      this.client!.subscribe('/user/queue/direct', m => this.direct$.next(JSON.parse(m.body)));
      this.pending.forEach(fn => fn());
    };
    this.client.activate();
  }

  disconnect() {
    this.client?.deactivate();
    this.client = undefined;
    this.pending = [];
    this.subs.clear();
  }

  /** Subscribe to a project channel ("project" or "announcement"); replaces older subscriptions to the same channel. */
  watchProject(projectId: number, channel: 'project' | 'announcement') {
    const dest = `/topic/project.${projectId}.${channel}`;
    const run = () => {
      if (this.subs.has(dest)) return;
      this.subs.set(dest, this.client!.subscribe(dest, (m: IMessage) => this.project$.next(JSON.parse(m.body))));
    };
    if (this.client?.connected) run(); else this.pending.push(run);
  }

  unwatchProject(projectId: number, channel: 'project' | 'announcement') {
    const dest = `/topic/project.${projectId}.${channel}`;
    this.subs.get(dest)?.unsubscribe();
    this.subs.delete(dest);
  }
}
