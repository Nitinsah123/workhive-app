export type Role = 'TENANT_ADMIN' | 'MANAGER' | 'EMPLOYEE';

export interface User {
  id: string;
  tenantId: string;
  email: string;
  fullName: string;
  employeeCode: string;
  role: Role;
  departmentId?: string;
  teamId?: string;
  managerId?: string;
  phone?: string;
  avatarUrl?: string;
  timezone?: string;
  status: string;
}

export interface Tenant {
  id: string;
  name: string;
  slug: string;
  code: string;
  logoUrl?: string;
  industry?: string;
  timezone: string;
  workingDays: string;
  status: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  user: {
    id: string;
    email: string;
    fullName: string;
    role: Role;
    employeeCode: string;
    avatarUrl?: string;
  };
  tenant: {
    id: string;
    name: string;
    code: string;
    logoUrl?: string;
    timezone: string;
  };
}

export interface Department {
  id: string;
  tenantId: string;
  name: string;
  description?: string;
  managerId?: string;
  status: string;
}

export interface Team {
  id: string;
  tenantId: string;
  name: string;
  departmentId?: string;
  leadId?: string;
  description?: string;
  status: string;
}

export interface Project {
  id: string;
  tenantId: string;
  name: string;
  description?: string;
  ownerId?: string;
  managerId?: string;
  departmentId?: string;
  teamId?: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  status: 'PLANNING' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'ARCHIVED';
  startDate?: string;
  targetDate?: string;
  progress: number;
  health: 'ON_TRACK' | 'AT_RISK' | 'OFF_TRACK' | 'COMPLETED';
}

export interface Milestone {
  id: string;
  projectId: string;
  name: string;
  description?: string;
  targetDate?: string;
  status: 'PENDING' | 'COMPLETED';
}

export interface Task {
  id: string;
  tenantId: string;
  projectId?: string;
  title: string;
  description?: string;
  assigneeId?: string;
  creatorId: string;
  reviewerId?: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  status: 'TODO' | 'IN_PROGRESS' | 'REVIEW' | 'COMPLETED' | 'CANCELLED';
  dueDate?: string;
  labels?: string;
  estimatedHours?: number;
  actualHours?: number;
  milestoneId?: string;
  version?: number;
}

export interface Subtask {
  id: string;
  taskId: string;
  title: string;
  completed: boolean;
  sortOrder: number;
}

export interface TaskComment {
  id: string;
  taskId: string;
  authorId: string;
  content: string;
  createdAt: string;
}

export interface Attendance {
  id: string;
  tenantId: string;
  userId: string;
  date: string;
  checkIn?: string;
  checkOut?: string;
  durationMinutes?: number;
  status: 'CHECKED_IN' | 'CHECKED_OUT';
  notes?: string;
}

export interface LeaveType {
  id: string;
  name: string;
  description?: string;
  defaultBalance: number;
  carryForward: boolean;
}

export interface LeaveBalance {
  id: string;
  leaveTypeId: string;
  year: number;
  total: number;
  used: number;
  remaining: number;
}

export interface LeaveRequest {
  id: string;
  userId: string;
  leaveTypeId: string;
  startDate: string;
  endDate: string;
  days: number;
  reason?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  reviewerId?: string;
  reviewComment?: string;
  createdAt: string;
}

export interface DocumentItem {
  id: string;
  name: string;
  originalName: string;
  contentType?: string;
  fileSize?: number;
  entityType?: string;
  entityId?: string;
  version: number;
  description?: string;
  createdAt: string;
}

export interface Announcement {
  id: string;
  title: string;
  content: string;
  authorId: string;
  targetType: string;
  priority: string;
  publishedAt: string;
}

export interface NotificationItem {
  id: string;
  type: string;
  title: string;
  message?: string;
  read: boolean;
  actionUrl?: string;
  createdAt: string;
}

export interface ActionItem {
  id: string;
  type: 'LEAVE_REQUEST' | 'TASK_REVIEW' | 'DOCUMENT_APPROVAL';
  title: string;
  description?: string;
  status: string;
  createdAt: string;
  requesterName: string;
  requesterEmail: string;
  requesterEmployeeCode: string;
  requesterAvatarUrl?: string;
  requesterDepartment?: string;
  requesterTeam?: string;
  entityType: string;
  entityId: string;
  metadata?: any;
}

export interface WorkActivity {
  id: string;
  source: string;
  activityType: string;
  title?: string;
  description?: string;
  externalEventId?: string;
  externalUrl?: string;
  createdAt: string;
}
