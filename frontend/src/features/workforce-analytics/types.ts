export interface UnitMemberLiveDTO {
  userId: string;
  fullName: string;
  email: string;
  employeeCode: string;
  avatarUrl?: string | null;
  role: string;
  organizationalRole: string;
  departmentId?: string | null;
  departmentName?: string | null;
  teamId?: string | null;
  teamName?: string | null;
  presenceStatus: 'Coding / Active' | 'In Sync' | 'Idle' | 'Offline' | string;
  assignedTasksCount: number;
  pendingTasksCount: number;
  completedTasksCount: number;
  overdueTasksCount: number;
  activeHoursToday: number;
  gitCommitsToday: number;
  gitPrsToday: number;
  topTaskTitles: string[];
  lastActiveTimeFormatted: string;
}

export interface UnitRosterResponseDTO {
  unitId: string;
  unitType: 'DEPARTMENT' | 'TEAM';
  unitName: string;
  description?: string | null;
  leadName?: string | null;
  leadRole?: string | null;
  leadAvatarUrl?: string | null;
  totalMembers: number;
  liveOnlineCount: number;
  activeTasksCount: number;
  members: UnitMemberLiveDTO[];
}

export interface HourlyActivityPoint {
  hour: string;
  activeMinutes: number;
  idleMinutes: number;
}

export interface TaskBreakdownItem {
  name: string;
  value: number;
  color: string;
  status: string;
  priority: string;
}

export interface RolePerformanceRadar {
  subject: string;
  score: number;
  benchmark: number;
  fullMark?: string;
}

export interface DailyVelocityPoint {
  date: string;
  commits: number;
  tasksDone: number;
}

export interface ActivityTimelineItem {
  id: string;
  eventType: 'GIT_PUSH' | 'GIT_PR' | 'TASK_ASSIGNED' | 'TASK_COMPLETED' | 'HEARTBEAT' | string;
  title: string;
  description: string;
  timestamp: string;
  repository?: string | null;
  branch?: string | null;
  commitSha?: string | null;
  externalUrl?: string | null;
  metadataJson?: string | null;
}

export interface TaskSummaryItem {
  id: string;
  title: string;
  status: string;
  priority: string;
  dueDate: string;
  overdue: boolean;
  projectName?: string | null;
}

export interface Employee360DashboardDTO {
  userId: string;
  fullName: string;
  email: string;
  employeeCode: string;
  role: string;
  organizationalRole: string;
  avatarUrl?: string | null;
  departmentName: string;
  teamName: string;
  presenceStatus: string;
  activeFocusTask: string;
  activeGitBranch: string;
  timeRange: 'TODAY' | '7D' | '30D' | 'CURRENT_SPRINT' | string;

  activeWorkHours: number;
  efficiencyPercentage: number;
  tasksAssigned: number;
  tasksPending: number;
  tasksCompleted: number;
  taskCompletionRatePercentage: number;
  commitsPushed: number;
  prsOpenedOrMerged: number;
  linesChanged: number;
  currentStreakDays: number;
  slaAdherencePercentage: number;

  hourlyActivity: HourlyActivityPoint[];
  taskBreakdown: TaskBreakdownItem[];
  rolePerformance: RolePerformanceRadar[];
  dailyVelocity: DailyVelocityPoint[];

  activityTimeline: ActivityTimelineItem[];
  assignedTasks: TaskSummaryItem[];
  pendingTasks: TaskSummaryItem[];
}
