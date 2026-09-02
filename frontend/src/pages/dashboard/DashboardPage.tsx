import React from 'react';
import { useAuthStore } from '../../store/authStore';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  reportApi,
  taskApi,
  attendanceApi,
  actionCenterApi,
  activityApi,
  leaveApi,
} from '../../api/services';
import { StatCard } from '../../components/common/StatCard';
import {
  FolderKanban,
  CheckSquare,
  Clock,
  Calendar,
  Users,
  Inbox,
  Play,
  Square,
  TrendingUp,
  AlertTriangle,
  Sparkles,
  ArrowUpRight,
} from 'lucide-react';
import { Link } from 'react-router-dom';

export const DashboardPage: React.FC = () => {
  const { user, tenant } = useAuthStore();
  const queryClient = useQueryClient();
  const isAdmin = user?.role === 'TENANT_ADMIN';
  const isManager = user?.role === 'MANAGER';

  // Admin/Manager Org Report
  const { data: orgReportData } = useQuery({
    queryKey: ['org-report'],
    queryFn: () => reportApi.getOrgReport(),
    enabled: isAdmin || isManager,
  });

  // Action center summary
  const { data: actionSummaryData } = useQuery({
    queryKey: ['action-center-summary'],
    queryFn: () => actionCenterApi.getSummary(),
    enabled: isAdmin || isManager,
  });

  // Employee personal work report
  const { data: empReportData } = useQuery({
    queryKey: ['my-work-report'],
    queryFn: () => reportApi.getEmployeeReport(),
  });

  // Today's attendance status
  const { data: todayAttendanceData } = useQuery({
    queryKey: ['today-attendance'],
    queryFn: () => attendanceApi.getToday(),
  });

  // Active tasks for user
  const { data: myTasksData } = useQuery({
    queryKey: ['my-active-tasks'],
    queryFn: () => taskApi.getMyActive(),
  });

  // Leave balances
  const { data: balancesData } = useQuery({
    queryKey: ['my-leave-balances'],
    queryFn: () => leaveApi.getMyBalances(),
  });

  // Check-in / check-out mutations
  const checkInMutation = useMutation({
    mutationFn: () => attendanceApi.checkIn(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['today-attendance'] });
      queryClient.invalidateQueries({ queryKey: ['my-work-report'] });
    },
  });

  const checkOutMutation = useMutation({
    mutationFn: () => attendanceApi.checkOut(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['today-attendance'] });
      queryClient.invalidateQueries({ queryKey: ['my-work-report'] });
    },
  });

  const orgReport = orgReportData?.data;
  const actionSummary = actionSummaryData?.data;
  const empReport = empReportData?.data;
  const todayAttendance = todayAttendanceData?.data;
  const myTasks = myTasksData?.data || [];
  const balances = balancesData?.data || [];

  return (
    <div className="space-y-8">
      {/* Welcome Banner */}
      <div className="glass-panel p-6 sm:p-8 rounded-3xl relative overflow-hidden flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="space-y-2">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider border border-indigo-500/20">
            <Sparkles className="w-3.5 h-3.5" />
            {tenant?.name} • Workspace
          </div>
          <h1 className="text-3xl sm:text-4xl font-black font-display text-white">
            Welcome back, {user?.fullName?.split(' ')[0]} 👋
          </h1>
          <p className="text-slate-400 text-sm max-w-xl">
            {isAdmin
              ? 'Here is your organization-wide control center and pending workflow summary.'
              : isManager
              ? 'Here is your team oversight, project health, and review queue.'
              : 'Here is your personal day plan, assigned work, and attendance status.'}
          </p>
        </div>

        {/* Quick Attendance Widget */}
        <div className="glass-card p-4 rounded-2xl border border-slate-800 flex items-center gap-4 self-start md:self-auto">
          <div className="flex flex-col">
            <span className="text-xs text-slate-400 font-semibold uppercase tracking-wider">
              Today's Attendance
            </span>
            <span className="text-sm font-bold text-slate-200 mt-0.5">
              {todayAttendance?.checkedIn
                ? todayAttendance?.status === 'CHECKED_IN'
                  ? '🟢 Checked In'
                  : '⚪ Checked Out'
                : '🟡 Not Checked In'}
            </span>
          </div>

          {!todayAttendance?.checkedIn ? (
            <button
              onClick={() => checkInMutation.mutate()}
              disabled={checkInMutation.isPending}
              className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs shadow-lg shadow-emerald-600/25 flex items-center gap-1.5 transition-all"
            >
              <Play className="w-3.5 h-3.5 fill-current" />
              <span>Check In</span>
            </button>
          ) : todayAttendance?.status === 'CHECKED_IN' ? (
            <button
              onClick={() => checkOutMutation.mutate()}
              disabled={checkOutMutation.isPending}
              className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs shadow-lg shadow-rose-600/25 flex items-center gap-1.5 transition-all"
            >
              <Square className="w-3.5 h-3.5 fill-current" />
              <span>Check Out</span>
            </button>
          ) : null}
        </div>
      </div>

      {/* Admin / Manager Top Stats */}
      {(isAdmin || isManager) && orgReport && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          <StatCard
            title="Total Employees"
            value={orgReport.totalEmployees}
            subtitle={`${orgReport.totalDepartments} departments • ${orgReport.totalTeams} teams`}
            icon={Users}
            color="indigo"
          />
          <StatCard
            title="Active Projects"
            value={orgReport.activeProjects}
            subtitle={`${orgReport.totalTasks} total tasks`}
            icon={FolderKanban}
            color="sky"
          />
          <StatCard
            title="Action Center Pending"
            value={actionSummary?.totalPending || 0}
            subtitle={`${actionSummary?.pendingLeaves || 0} leaves • ${actionSummary?.pendingTaskReviews || 0} reviews`}
            icon={Inbox}
            color="amber"
          />
          <StatCard
            title="Present Today"
            value={orgReport.presentToday}
            subtitle="Employees checked in"
            icon={Clock}
            color="emerald"
          />
        </div>
      )}

      {/* Employee / Personal Work Stats */}
      {empReport && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          <StatCard
            title="My Assigned Tasks"
            value={empReport.assignedTasks}
            subtitle={`${empReport.completedTasks} completed`}
            icon={CheckSquare}
            color="indigo"
          />
          <StatCard
            title="Completion Rate"
            value={`${empReport.completionRate}%`}
            subtitle="Overall performance"
            icon={TrendingUp}
            color="emerald"
          />
          <StatCard
            title="Overdue Tasks"
            value={empReport.overdueTasks}
            subtitle={empReport.overdueTasks > 0 ? 'Requires attention' : 'All on track'}
            icon={AlertTriangle}
            color={empReport.overdueTasks > 0 ? 'rose' : 'emerald'}
          />
          <StatCard
            title="Hours Logged (30d)"
            value={`${Math.round(empReport.totalTimeLoggedMinutes / 60)}h`}
            subtitle={`${empReport.daysPresent} days present`}
            icon={Clock}
            color="violet"
          />
        </div>
      )}

      {/* Two Column Layout: My Tasks & Quick Actions / Balances */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left 2 Cols: My Active Tasks */}
        <div className="lg:col-span-2 space-y-6">
          <div className="glass-panel p-6 rounded-3xl">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold font-display text-white flex items-center gap-2">
                <CheckSquare className="w-5 h-5 text-indigo-400" /> My Active Tasks
              </h3>
              <Link to="/tasks" className="text-xs font-semibold text-indigo-400 hover:underline flex items-center gap-1">
                View All Tasks <ArrowUpRight className="w-3.5 h-3.5" />
              </Link>
            </div>

            {myTasks.length === 0 ? (
              <div className="text-center py-10 text-slate-500 text-sm">
                No active tasks assigned. Great job!
              </div>
            ) : (
              <div className="space-y-3">
                {myTasks.slice(0, 5).map((task) => (
                  <div
                    key={task.id}
                    className="p-4 rounded-xl bg-slate-900/60 border border-slate-800/80 flex items-center justify-between hover:border-slate-700 transition-colors"
                  >
                    <div className="space-y-1">
                      <div className="text-sm font-bold text-slate-200">{task.title}</div>
                      <div className="text-xs text-slate-500 flex items-center gap-3">
                        <span className="font-mono text-slate-400">{task.priority} Priority</span>
                        {task.dueDate && <span>Due {task.dueDate}</span>}
                      </div>
                    </div>
                    <span
                      className={`text-xs px-2.5 py-1 rounded-lg font-semibold border ${
                        task.status === 'IN_PROGRESS'
                          ? 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                          : task.status === 'REVIEW'
                          ? 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20'
                          : 'bg-slate-800 text-slate-400 border-slate-700'
                      }`}
                    >
                      {task.status}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Right 1 Col: Leave Balances & Quick Links */}
        <div className="space-y-6">
          {/* Leave Balances */}
          <div className="glass-panel p-6 rounded-3xl">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold font-display text-white flex items-center gap-2">
                <Calendar className="w-5 h-5 text-indigo-400" /> Leave Balances
              </h3>
              <Link to="/leave" className="text-xs font-semibold text-indigo-400 hover:underline">
                Apply Leave
              </Link>
            </div>

            {balances.length === 0 ? (
              <div className="text-center py-6 text-slate-500 text-sm">No leave types assigned</div>
            ) : (
              <div className="space-y-3">
                {balances.map((b) => (
                  <div
                    key={b.id}
                    className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center justify-between"
                  >
                    <div>
                      <span className="text-xs font-bold text-slate-300">Remaining</span>
                      <div className="text-xs text-slate-500">{b.used} of {b.total} days used</div>
                    </div>
                    <div className="text-2xl font-black font-display text-indigo-400">
                      {b.remaining}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Quick Hub Shortcut for Admins */}
          {(isAdmin || isManager) && (
            <div className="glass-panel p-6 rounded-3xl border border-indigo-500/20 bg-gradient-to-br from-indigo-950/30 to-slate-900">
              <h4 className="text-sm font-bold text-white mb-2 flex items-center gap-2">
                <Inbox className="w-4 h-4 text-indigo-400" /> Action Center Hub
              </h4>
              <p className="text-xs text-slate-400 mb-4">
                You have {actionSummary?.totalPending || 0} pending items requiring review.
              </p>
              <Link
                to="/action-center"
                className="w-full py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs flex items-center justify-center gap-2 transition-all shadow-md"
              >
                <span>Open Action Center</span>
                <ArrowUpRight className="w-4 h-4" />
              </Link>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
