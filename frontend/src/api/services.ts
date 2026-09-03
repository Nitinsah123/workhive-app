import api from './client';
import { AuthResponse, Department, Team, Project, Milestone, Task, Subtask, TaskComment, Attendance, LeaveType, LeaveBalance, LeaveRequest, DocumentItem, Announcement, NotificationItem, ActionItem, WorkActivity } from '../types';

export const authApi = {
  createWorkspace: (data: any) => api.post<AuthResponse>('/auth/workspace', data),
  login: (data: any) => api.post<AuthResponse>('/auth/login', data),
  logout: () => api.post('/auth/logout'),
};

export const departmentApi = {
  getAll: () => api.get<Department[]>('/departments/active'),
  create: (data: any) => api.post<Department>('/departments', data),
  update: (id: string, data: any) => api.put<Department>(`/departments/${id}`, data),
  delete: (id: string) => api.delete(`/departments/${id}`),
  archive: (id: string) => api.post(`/departments/${id}/archive`),
  restore: (id: string) => api.post(`/departments/${id}/restore`),
  permanentDelete: (id: string) => api.delete(`/departments/${id}/permanent`),
  getArchived: () => api.get<Department[]>('/departments/archived'),
};

export const teamApi = {
  getAll: () => api.get<Team[]>('/teams/active'),
  getAllWithInactive: () => api.get<Team[]>('/teams/all'),
  getByDepartment: (deptId: string) => api.get<Team[]>(`/teams/department/${deptId}`),
  create: (data: any) => api.post<Team>('/teams', data),
  update: (id: string, data: any) => api.put<Team>(`/teams/${id}`, data),
  delete: (id: string) => api.delete(`/teams/${id}`),
  archive: (id: string) => api.post(`/teams/${id}/archive`),
  restore: (id: string) => api.post(`/teams/${id}/restore`),
  permanentDelete: (id: string) => api.delete(`/teams/${id}/permanent`),
  getArchived: () => api.get<Team[]>('/teams/archived'),
};

export const userApi = {
  getAll: (page = 0, size = 50) => api.get<{ content: any[] }>(`/users?page=${page}&size=${size}`),
  getActive: () => api.get<any[]>('/users/active'),
  getMe: () => api.get<any>('/users/me'),
  create: (data: any) => api.post('/users', data),
  update: (id: string, data: any) => api.put(`/users/${id}`, data),
  updateProfile: (data: any) => api.put('/users/profile', data),
  changePassword: (data: any) => api.post('/users/change-password', data),
  deactivate: (id: string) => api.delete(`/users/${id}`),
  reactivate: (id: string) => api.post(`/users/${id}/reactivate`),
  archive: (id: string) => api.post(`/users/${id}/archive`),
  restore: (id: string) => api.post(`/users/${id}/restore`),
  permanentDelete: (id: string) => api.delete(`/users/${id}/permanent`),
  getArchived: () => api.get<any[]>('/users/archived'),
  invite: (data: any) => api.post<any>('/invitations', data),
  getInvitations: () => api.get<{ content: any[] }>('/invitations'),
  getInvitationDetails: (token: string) => api.get<any>(`/invitations/token/${token}`),
  resendInvitation: (id: string) => api.post<any>(`/invitations/${id}/resend`),
  revokeInvitation: (id: string) => api.delete(`/invitations/${id}`),
  acceptInvitation: (data: any) => api.post<AuthResponse>('/invitations/accept', data),
};

export const archiveApi = {
  getAll: (type?: string) => api.get<any>('/archive', { params: type && type !== 'ALL' ? { type } : {} }),
  restore: (type: string, id: string) => api.post(`/archive/restore/${type}/${id}`),
  permanentDelete: (type: string, id: string) => api.delete(`/archive/permanent/${type}/${id}`),
};

export const projectApi = {
  getAll: (page = 0, size = 50) => api.get<{ content: Project[] }>(`/projects?page=${page}&size=${size}`),
  getById: (id: string) => api.get<Project>(`/projects/${id}`),
  create: (data: any) => api.post<Project>('/projects', data),
  update: (id: string, data: any) => api.put<Project>(`/projects/${id}`, data),
  delete: (id: string) => api.delete(`/projects/${id}`),
  getMembers: (id: string) => api.get<any[]>(`/projects/${id}/members`),
  addMember: (id: string, userId: string, role = 'MEMBER') => api.post(`/projects/${id}/members?userId=${userId}&role=${role}`),
  removeMember: (id: string, userId: string) => api.delete(`/projects/${id}/members/${userId}`),
  getMilestones: (id: string) => api.get<Milestone[]>(`/projects/${id}/milestones`),
  createMilestone: (id: string, data: any) => api.post<Milestone>(`/projects/${id}/milestones`, data),
  completeMilestone: (id: string) => api.patch<Milestone>(`/projects/milestones/${id}/complete`),
};

export const taskApi = {
  getAll: (projectId?: string, assigneeId?: string) => {
    let url = '/tasks?size=100';
    if (projectId) url += `&projectId=${projectId}`;
    if (assigneeId) url += `&assigneeId=${assigneeId}`;
    return api.get<{ content: Task[] }>(url);
  },
  getMyActive: () => api.get<Task[]>('/tasks/my-active'),
  getById: (id: string) => api.get<Task>(`/tasks/${id}`),
  create: (data: any) => api.post<Task>('/tasks', data),
  update: (id: string, data: any) => api.put<Task>(`/tasks/${id}`, data),
  delete: (id: string) => api.delete(`/tasks/${id}`),
  updateStatus: (id: string, data: { status: string; comment?: string; version?: number }) =>
    api.patch<Task>(`/tasks/${id}/status`, data),
  submitReview: (id: string, data: { workSummary: string; repositoryUrl: string; provider?: string; branch?: string; pullRequestUrl?: string; commitSha?: string }) =>
    api.post<Task>(`/tasks/${id}/submit-review`, data),
  getSubmissions: (id: string) => api.get<any[]>(`/tasks/${id}/submissions`),
  review: (id: string, data: { decision: string; comment?: string }) =>
    api.patch<Task>(`/tasks/${id}/review`, data),
  getSubtasks: (id: string) => api.get<Subtask[]>(`/tasks/${id}/subtasks`),
  addSubtask: (id: string, data: { title: string }) => api.post<Subtask>(`/tasks/${id}/subtasks`, data),
  toggleSubtask: (subtaskId: string) => api.patch<Subtask>(`/tasks/subtasks/${subtaskId}/toggle`),
  getComments: (id: string) => api.get<{ content: TaskComment[] }>(`/tasks/${id}/comments`),
  addComment: (id: string, data: { content: string }) => api.post<TaskComment>(`/tasks/${id}/comments`, data),
  getHistory: (id: string) => api.get<any[]>(`/tasks/${id}/history`),
};

export const attendanceApi = {
  checkIn: (data?: any) => api.post<Attendance>('/attendance/check-in', data || {}),
  checkOut: (data?: any) => api.post<Attendance>('/attendance/check-out', data || {}),
  getToday: () => api.get<{ checkedIn: boolean; status: string; record?: Attendance }>('/attendance/today'),
  getMy: (page = 0, size = 30) => api.get<{ content: Attendance[] }>(`/attendance/my?page=${page}&size=${size}`),
  getDaily: (date?: string) => api.get<{ content: Attendance[] }>(`/attendance/daily${date ? `?date=${date}` : ''}`),
};

export const timeTrackingApi = {
  logTime: (data: any) => api.post('/time-entries', data),
  getMy: () => api.get<{ content: any[] }>('/time-entries/my'),
  getByTask: (taskId: string) => api.get<any[]>(`/time-entries/task/${taskId}`),
};

export const leaveApi = {
  apply: (data: any) => api.post<LeaveRequest>('/leaves/apply', data),
  getMy: () => api.get<{ content: LeaveRequest[] }>('/leaves/my'),
  getMyBalances: (year?: number) => api.get<LeaveBalance[]>(`/leaves/my-balances${year ? `?year=${year}` : ''}`),
  getAll: (status?: string) => api.get<{ content: LeaveRequest[] }>(`/leaves${status ? `?status=${status}` : ''}`),
  review: (id: string, data: { status: string; reviewComment?: string }) =>
    api.patch<LeaveRequest>(`/leaves/${id}/review`, data),
  getTypes: () => api.get<LeaveType[]>('/leaves/types'),
  createType: (data: any) => api.post<LeaveType>('/leaves/types', data),
};

export const actionCenterApi = {
  getPending: (type?: string) => api.get<ActionItem[]>(`/action-center${type ? `?type=${type}` : ''}`),
  getSummary: () => api.get<any>('/action-center/summary'),
};

export const documentApi = {
  getAll: (page = 0, size = 50) => api.get<{ content: DocumentItem[] }>(`/documents?page=${page}&size=${size}`),
  getMy: () => api.get<{ content: DocumentItem[] }>('/documents/my'),
  upload: (formData: FormData) => api.post<DocumentItem>('/documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
  delete: (id: string) => api.delete(`/documents/${id}`),
};

export const announcementApi = {
  getMy: () => api.get<Announcement[]>('/announcements/my'),
  getAll: () => api.get<{ content: Announcement[] }>('/announcements'),
  create: (data: any) => api.post<Announcement>('/announcements', data),
};

export const notificationApi = {
  getMy: () => api.get<{ content: NotificationItem[] }>('/notifications'),
  getUnreadCount: () => api.get<{ count: number }>('/notifications/unread-count'),
  markRead: (id: string) => api.patch(`/notifications/${id}/read`),
  markAllRead: () => api.post('/notifications/read-all'),
};

export const reportApi = {
  getEmployeeReport: (userId?: string) => api.get<any>(`/reports/employee${userId ? `?userId=${userId}` : ''}`),
  getProjectReport: (projectId: string) => api.get<any>(`/reports/project/${projectId}`),
  getOrgReport: () => api.get<any>('/reports/organization'),
};

export const integrationApi = {
  getAll: () => api.get<any[]>('/integrations'),
  connect: (data: any) => api.post('/integrations/connect', data),
  disconnect: (id: string) => api.delete(`/integrations/${id}`),
  sync: (id: string) => api.post<any>(`/integrations/${id}/sync`),
  mapEntity: (id: string, data: any) => api.post(`/integrations/${id}/mappings`, data),
  getMappings: (id: string) => api.get<any[]>(`/integrations/${id}/mappings`),

  // Jira
  getJiraProjects: (id: string) => api.get<any[]>(`/integrations/jira/${id}/projects`),
  getJiraIssues: (id: string, projectKey?: string) =>
    api.get<any[]>(`/integrations/jira/${id}/issues${projectKey ? `?projectKey=${projectKey}` : ''}`),

  // GitLab
  getGitLabProjects: (id: string) => api.get<any[]>(`/integrations/gitlab/${id}/projects`),
  getGitLabCommits: (id: string, projectId: number | string) =>
    api.get<any[]>(`/integrations/gitlab/${id}/projects/${projectId}/commits`),
  getGitLabMergeRequests: (id: string, projectId: number | string) =>
    api.get<any[]>(`/integrations/gitlab/${id}/projects/${projectId}/merge-requests`),

  // Slack
  getSlackChannels: (id: string) => api.get<any[]>(`/integrations/slack/${id}/channels`),
  postSlackMessage: (id: string, data: { channel: string; text: string }) =>
    api.post<any>(`/integrations/slack/${id}/test-message`, data),

  // GitHub Data Hub
  getGitHubOAuthUrl: () => api.get<any>('/integrations/github/oauth/url'),
  getGitHubOverview: (id: string) => api.get<any>(`/integrations/github/${id}/overview`),
  getGitHubRepositories: (id: string) => api.get<any[]>(`/integrations/github/${id}/repositories`),
  getGitHubCommits: (id: string, repo?: string) =>
    api.get<any[]>(`/integrations/github/${id}/commits${repo ? `?repo=${encodeURIComponent(repo)}` : ''}`),
  getGitHubPullRequests: (id: string, repo?: string) =>
    api.get<any[]>(`/integrations/github/${id}/pull-requests${repo ? `?repo=${encodeURIComponent(repo)}` : ''}`),
  getGitHubIssues: (id: string, repo?: string) =>
    api.get<any[]>(`/integrations/github/${id}/issues${repo ? `?repo=${encodeURIComponent(repo)}` : ''}`),
};

export const activityApi = {
  getTenant: (page = 0, size = 30, userId?: string) => api.get<{ content: WorkActivity[] }>(`/activities?page=${page}&size=${size}${userId ? `&userId=${userId}` : ''}`),
  getMy: () => api.get<{ content: WorkActivity[] }>('/activities/my'),
  getProject: (projectId: string) => api.get<{ content: WorkActivity[] }>(`/activities/project/${projectId}`),
  getHeatmap: (userId?: string) => api.get<any>(`/activities/heatmap${userId ? `?userId=${userId}` : ''}`),
};

export const exportApi = {
  tasksCsv: () => api.get('/exports/tasks/csv', { responseType: 'blob' }),
  attendanceXlsx: () => api.get('/exports/attendance/xlsx', { responseType: 'blob' }),
  leaveCsv: () => api.get('/exports/leave/csv', { responseType: 'blob' }),
  activityCsv: () => api.get('/exports/activity/csv', { responseType: 'blob' }),
  employeePdf: (userId?: string) => api.get(`/exports/employee/pdf${userId ? `?userId=${userId}` : ''}`, { responseType: 'blob' }),
  projectPdf: (projectId: string) => api.get(`/exports/project/${projectId}/pdf`, { responseType: 'blob' }),
  orgPdf: () => api.get('/exports/organization/pdf', { responseType: 'blob' }),
};

export const searchApi = {
  search: (q: string) => api.get<any>(`/search?q=${encodeURIComponent(q)}`),
};

export const settingsApi = {
  get: () => api.get<any>('/settings'),
  update: (data: any) => api.put('/settings', data),
};

export const emailConnectionApi = {
  getStatus: () => api.get<any>('/email-connections/status'),
  connectGmail: () => api.post<any>('/email-connections/gmail/connect'),
  disconnect: () => api.delete<any>('/email-connections/disconnect'),
  reconnect: () => api.post<any>('/email-connections/gmail/reconnect'),
  callbackExchange: (data: { code: string; state: string }) =>
    api.post<any>('/email-connections/gmail/callback-exchange', data),
};
