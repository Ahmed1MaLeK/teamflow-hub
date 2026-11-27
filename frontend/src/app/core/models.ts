export type Role = 'ADMIN' | 'MEMBER';
export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'REVIEWING' | 'DONE' | 'ACCEPTED';
export type ChannelType = 'DIRECT' | 'PROJECT' | 'ANNOUNCEMENT';

export const TASK_STATUSES: TaskStatus[] = ['PENDING', 'IN_PROGRESS', 'REVIEWING', 'ACCEPTED', 'DONE'];
export const STATUS_LABEL: Record<TaskStatus, string> = {
  PENDING: 'Pending', IN_PROGRESS: 'In progress', REVIEWING: 'Reviewing', ACCEPTED: 'Accepted', DONE: 'Done'
};

export interface User { id: number; name: string; username: string; email: string; role: Role; }
export interface AuthResponse { token: string; user: User; }
export interface Team { id: number; name: string; members: User[]; }
export interface Project {
  id: number; name: string; description: string; status: string; startDate: string | null; endDate: string | null;
  manager: User; members: User[]; teams: Team[]; totalTasks: number; completedTasks: number; progress: number;
}
export interface Task {
  id: number; title: string; description: string; status: TaskStatus; startDate: string | null; deadline: string | null;
  projectId: number; projectName: string; assignedTo: User | null; createdBy: User; rating: number | null;
  reviewFeedback: string | null; createdAt: string; updatedAt: string;
}
export interface Attachment { id: number; fileName: string; kind: string; uploadedBy: User; createdAt: string; }
export interface ChatMessage { id: number; channel: ChannelType; sender: User; receiverId: number | null; projectId: number | null; text: string; sentAt: string; }
export interface Channels { directs: User[]; projects: { id: number; name: string }[]; }
export interface AppNotification { id: number; message: string; link: string | null; read: boolean; createdAt: string; }
export interface MemberPerformance { user: User; assigned: number; completed: number; completionPercent: number; avgRating: number | null; }
export interface Dashboard {
  projects: number; pendingTasks: number; completedTasks: number; efficiencyPercent: number;
  tasksByStatus: Record<string, number>; completedPerDay: Record<string, number>;
  recentProjects: Project[]; performance: MemberPerformance[];
  recentActivity: { taskId: number; title: string; projectName: string; status: TaskStatus; at: string }[];
}
export interface Insights { summary: string; highlights: string[]; recommendations: string[]; }
