import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { API_URL } from './config';
import * as M from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private u = (p: string) => `${API_URL}/api${p}`;

  // auth / profile
  forgot(email: string) { return this.http.post<{ message: string; resetToken: string | null }>(this.u('/auth/forgot-password'), { email }); }
  reset(token: string, newPassword: string) { return this.http.post(this.u('/auth/reset-password'), { token, newPassword }); }
  updateProfile(name: string, newPassword: string) { return this.http.put<M.User>(this.u('/me'), { name, newPassword: newPassword || null }); }

  // projects
  projects() { return this.http.get<M.Project[]>(this.u('/projects')); }
  project(id: number) { return this.http.get<M.Project>(this.u(`/projects/${id}`)); }
  createProject(b: any) { return this.http.post<M.Project>(this.u('/projects'), b); }
  updateProject(id: number, b: any) { return this.http.put<M.Project>(this.u(`/projects/${id}`), b); }
  deleteProject(id: number) { return this.http.delete(this.u(`/projects/${id}`)); }
  addMember(id: number, username: string) { return this.http.post<M.Project>(this.u(`/projects/${id}/members`), { username }); }
  removeMember(id: number, userId: number) { return this.http.delete<M.Project>(this.u(`/projects/${id}/members/${userId}`)); }
  addTeamToProject(id: number, teamId: number) { return this.http.post<M.Project>(this.u(`/projects/${id}/teams/${teamId}`), {}); }
  projectFiles(id: number) { return this.http.get<M.Attachment[]>(this.u(`/projects/${id}/attachments`)); }
  uploadProjectFile(id: number, file: File) { const f = new FormData(); f.append('file', file); return this.http.post<M.Attachment>(this.u(`/projects/${id}/attachments`), f); }
  downloadFile(id: number) { return this.http.get(this.u(`/attachments/${id}/download`), { responseType: 'blob' }); }

  // tasks
  tasks(projectId?: number) {
    let p = new HttpParams();
    if (projectId) p = p.set('projectId', projectId);
    return this.http.get<M.Task[]>(this.u('/tasks'), { params: p });
  }
  task(id: number) { return this.http.get<M.Task>(this.u(`/tasks/${id}`)); }
  createTask(b: any) { return this.http.post<M.Task>(this.u('/tasks'), b); }
  updateTask(id: number, b: any) { return this.http.put<M.Task>(this.u(`/tasks/${id}`), b); }
  deleteTask(id: number) { return this.http.delete(this.u(`/tasks/${id}`)); }
  setStatus(id: number, status: M.TaskStatus) { return this.http.patch<M.Task>(this.u(`/tasks/${id}/status`), { status }); }
  review(id: number, accept: boolean, rating: number | null, feedback: string) {
    return this.http.post<M.Task>(this.u(`/tasks/${id}/review`), { accept, rating, feedback });
  }
  taskFiles(id: number) { return this.http.get<M.Attachment[]>(this.u(`/tasks/${id}/attachments`)); }
  uploadTaskFile(id: number, kind: 'GUIDELINE' | 'DELIVERABLE', file: File) {
    const f = new FormData(); f.append('file', file);
    return this.http.post<M.Attachment>(this.u(`/tasks/${id}/attachments`), f, { params: { kind } });
  }

  // teams / users
  teams() { return this.http.get<M.Team[]>(this.u('/teams')); }
  createTeam(name: string, memberIds: number[]) { return this.http.post<M.Team>(this.u('/teams'), { name, memberIds }); }
  updateTeam(id: number, name: string, memberIds: number[]) { return this.http.put<M.Team>(this.u(`/teams/${id}`), { name, memberIds }); }
  deleteTeam(id: number) { return this.http.delete(this.u(`/teams/${id}`)); }
  users(q = '') { return this.http.get<M.User[]>(this.u('/users'), { params: { q } }); }

  // chat
  channels() { return this.http.get<M.Channels>(this.u('/chat/channels')); }
  messages(channel: M.ChannelType, projectId?: number, userId?: number) {
    let p = new HttpParams().set('channel', channel);
    if (projectId) p = p.set('projectId', projectId);
    if (userId) p = p.set('userId', userId);
    return this.http.get<M.ChatMessage[]>(this.u('/chat/messages'), { params: p });
  }
  send(b: { channel: M.ChannelType; projectId?: number; receiverId?: number; text: string }) {
    return this.http.post<M.ChatMessage>(this.u('/chat/messages'), b);
  }

  // notifications / dashboard
  notifications() { return this.http.get<M.AppNotification[]>(this.u('/notifications')); }
  unread() { return this.http.get<{ count: number }>(this.u('/notifications/unread-count')); }
  markRead(id: number) { return this.http.post(this.u(`/notifications/${id}/read`), {}); }
  markAllRead() { return this.http.post(this.u('/notifications/read-all'), {}); }
  dashboard() { return this.http.get<M.Dashboard>(this.u('/dashboard')); }
  insights() { return this.http.get<M.Insights>(this.u('/insights')); }
}
