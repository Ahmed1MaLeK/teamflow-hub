import { AfterViewChecked, Component, ElementRef, OnDestroy, OnInit, ViewChild, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { errorMessage } from '../core/auth.interceptor';
import { RealtimeService } from '../core/realtime.service';
import { ChannelType, Channels, ChatMessage } from '../core/models';

interface Target { channel: ChannelType; id: number; label: string; }

@Component({
  selector: 'app-messages', standalone: true, imports: [FormsModule, DatePipe],
  template: `
  <div class="chat">
    <div class="list card">
      <h3>Direct messages</h3>
      @for (u of ch()?.directs; track u.id) { <a [class.on]="is('DIRECT', u.id)" (click)="open({channel:'DIRECT', id:u.id, label:u.name})">{{ u.name }}</a> } @empty { <small class="muted">Nobody yet. Join a project first.</small> }
      <h3>Group chats</h3>
      @for (p of ch()?.projects; track p.id) { <a [class.on]="is('PROJECT', p.id)" (click)="open({channel:'PROJECT', id:p.id, label:p.name})"># {{ p.name }}</a> }
      <h3>Announcements</h3>
      @for (p of ch()?.projects; track p.id) { <a [class.on]="is('ANNOUNCEMENT', p.id)" (click)="open({channel:'ANNOUNCEMENT', id:p.id, label:p.name + ' announcements'})">{{ p.name }}</a> }
    </div>
    <div class="conv card">
      @if (target(); as t) {
        <h2>{{ t.label }}</h2>
        <div class="msgs" #scroll>
          @for (m of msgs(); track m.id) {
            <div class="msg" [class.mine]="m.sender.id === auth.user()?.id"><small class="muted">{{ m.sender.name }} · {{ m.sentAt | date:'short' }}</small><div class="bubble">{{ m.text }}</div></div>
          } @empty { <div class="empty">No messages yet.</div> }
        </div>
        @if (error()) { <div class="error">{{ error() }}</div> }
        <form class="row" (ngSubmit)="send()"><input name="t" [(ngModel)]="text" placeholder="Write a message..." autocomplete="off"><button class="btn">Send</button></form>
      } @else { <div class="empty">Select a channel or conversation to start messaging.</div> }
    </div>
  </div>`,
  styles: [`
    .chat { display: flex; gap: 16px; padding: 24px; height: 100vh; }
    .list { width: 240px; overflow-y: auto; } .list a { display: block; padding: 7px 10px; border-radius: 8px; cursor: pointer; color: var(--text); }
    .list a.on { background: var(--primary-soft); color: var(--primary); font-weight: 600; } .list h3 { margin-top: 14px; color: var(--muted); font-size: 12px; text-transform: uppercase; }
    .conv { flex: 1; display: flex; flex-direction: column; min-width: 0; } .msgs { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; padding: 8px 0; }
    .msg { max-width: 70%; } .msg.mine { align-self: flex-end; text-align: right; }
    .bubble { background: #eef0f6; padding: 8px 12px; border-radius: 12px; display: inline-block; text-align: left; white-space: pre-wrap; word-break: break-word; }
    .mine .bubble { background: var(--primary); color: #fff; }
  `]
})
export class MessagesComponent implements OnInit, OnDestroy, AfterViewChecked {
  auth = inject(AuthService);
  private api = inject(ApiService);
  private rt = inject(RealtimeService);
  ch = signal<Channels | null>(null);
  target = signal<Target | null>(null);
  msgs = signal<ChatMessage[]>([]);
  error = signal('');
  text = '';
  private subs: Subscription[] = [];
  private stick = false;
  @ViewChild('scroll') scroll?: ElementRef<HTMLElement>;

  ngOnInit() {
    this.rt.connect();
    this.api.channels().subscribe(c => {
      this.ch.set(c);
      for (const p of c.projects) { this.rt.watchProject(p.id, 'project'); this.rt.watchProject(p.id, 'announcement'); }
    });
    this.subs.push(this.rt.direct$.subscribe(m => this.incoming(m)), this.rt.project$.subscribe(m => this.incoming(m)));
  }

  is(channel: ChannelType, id: number) { const t = this.target(); return !!t && t.channel === channel && t.id === id; }

  open(t: Target) {
    this.target.set(t); this.error.set('');
    const req = t.channel === 'DIRECT' ? this.api.messages('DIRECT', undefined, t.id) : this.api.messages(t.channel, t.id);
    req.subscribe(m => { this.msgs.set(m); this.stick = true; });
  }

  private incoming(m: ChatMessage) {
    const t = this.target();
    if (!t || this.msgs().some(x => x.id === m.id)) return;
    const mine = this.auth.user()?.id;
    const match = t.channel === 'DIRECT'
      ? m.channel === 'DIRECT' && (m.sender.id === t.id || (m.sender.id === mine && m.receiverId === t.id))
      : m.channel === t.channel && m.projectId === t.id;
    if (match) { this.msgs.update(a => [...a, m]); this.stick = true; }
  }

  send() {
    const t = this.target();
    if (!t || !this.text.trim()) return;
    this.error.set('');
    const body = t.channel === 'DIRECT' ? { channel: t.channel, receiverId: t.id, text: this.text } : { channel: t.channel, projectId: t.id, text: this.text };
    this.api.send(body).subscribe({
      next: m => { this.text = ''; this.incoming(m); },   // live push may also deliver it; duplicates are ignored by id
      error: e => this.error.set(errorMessage(e))
    });
  }

  ngAfterViewChecked() {
    if (this.stick && this.scroll) { this.scroll.nativeElement.scrollTop = this.scroll.nativeElement.scrollHeight; this.stick = false; }
  }

  ngOnDestroy() { this.subs.forEach(s => s.unsubscribe()); }
}
