import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { reportApi } from '../../api/services';
import api from '../../api/client';
import { useAuthStore } from '../../store/authStore';
import { StatCard } from '../../components/common/StatCard';
import {
  BarChart3,
  Download,
  FileSpreadsheet,
  FileText,
  TrendingUp,
  CheckCircle,
  Clock,
  Calendar,
  AlertTriangle,
  FolderKanban,
  FileDown,
} from 'lucide-react';

export const ReportsPage: React.FC = () => {
  const { user } = useAuthStore();
  const isAdmin = user?.role === 'TENANT_ADMIN' || user?.role === 'MANAGER';
  const [downloading, setDownloading] = useState<string | null>(null);

  const { data: empReportData, isLoading: empLoading } = useQuery({
    queryKey: ['my-work-report'],
    queryFn: () => reportApi.getEmployeeReport(),
  });

  const { data: orgReportData, isLoading: orgLoading } = useQuery({
    queryKey: ['org-report'],
    queryFn: () => reportApi.getOrgReport(),
    enabled: isAdmin,
  });

  const emp = empReportData?.data;
  const org = orgReportData?.data;

  const downloadFile = async (endpoint: string, defaultFilename: string, key: string) => {
    setDownloading(key);
    try {
      const response = await api.get(endpoint, {
        responseType: 'blob',
      });
      const contentType = (response.headers['content-type'] as string) || 'application/octet-stream';
      const blob = new Blob([response.data], { type: contentType });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = defaultFilename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Export download failed', err);
      alert('Failed to generate export file. Please try again.');
    } finally {
      setDownloading(null);
    }
  };

  return (
    <div className="space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider border border-indigo-500/20 mb-2">
            <BarChart3 className="w-3.5 h-3.5" />
            Productivity & Insights
          </div>
          <h1 className="text-3xl font-black font-display text-white">Reports & Analytics</h1>
          <p className="text-slate-400 text-sm">
            Explainable productivity signals derived from verified, authorized work activity.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <button
            onClick={() => downloadFile('/exports/tasks/csv', `tasks-${new Date().toISOString().slice(0, 10)}.csv`, 'csv')}
            disabled={downloading === 'csv'}
            className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold border border-slate-700 flex items-center gap-1.5 transition-all disabled:opacity-50"
          >
            <FileText className="w-4 h-4 text-indigo-400" />
            <span>{downloading === 'csv' ? 'Exporting CSV...' : 'Export Tasks CSV'}</span>
          </button>

          <button
            onClick={() => downloadFile('/exports/attendance/xlsx', `attendance-${new Date().toISOString().slice(0, 10)}.xlsx`, 'xlsx')}
            disabled={downloading === 'xlsx'}
            className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold border border-slate-700 flex items-center gap-1.5 transition-all disabled:opacity-50"
          >
            <FileSpreadsheet className="w-4 h-4 text-emerald-400" />
            <span>{downloading === 'xlsx' ? 'Exporting XLSX...' : 'Export Attendance XLSX'}</span>
          </button>

          <button
            onClick={() => downloadFile('/exports/pdf/employee', `employee-report-${new Date().toISOString().slice(0, 10)}.pdf`, 'pdf')}
            disabled={downloading === 'pdf'}
            className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold border border-slate-700 flex items-center gap-1.5 transition-all disabled:opacity-50"
          >
            <FileDown className="w-4 h-4 text-rose-400" />
            <span>{downloading === 'pdf' ? 'Exporting PDF...' : 'Report PDF'}</span>
          </button>
        </div>
      </div>

      {/* Employee Personal Summary */}
      {empLoading ? (
        <div className="text-center py-10 text-slate-500 text-sm">Loading performance report...</div>
      ) : emp ? (
        <div className="space-y-4">
          <h2 className="text-lg font-bold font-display text-white">Employee Work Performance (30 Days)</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
            <StatCard
              title="Completion Rate"
              value={`${emp.completionRate}%`}
              subtitle={`${emp.completedTasks} of ${emp.assignedTasks} tasks done`}
              icon={TrendingUp}
              color="emerald"
            />
            <StatCard
              title="Time Logged"
              value={`${Math.round(emp.totalTimeLoggedMinutes / 60)} hrs`}
              subtitle={`${emp.totalTimeLoggedMinutes} recorded minutes`}
              icon={Clock}
              color="indigo"
            />
            <StatCard
              title="Days Present"
              value={emp.daysPresent}
              subtitle="Recorded in last 30 days"
              icon={CheckCircle}
              color="sky"
            />
            <StatCard
              title="Leave Days Taken"
              value={emp.leaveDaysTaken}
              subtitle="Approved leave days"
              icon={Calendar}
              color="amber"
            />
          </div>
        </div>
      ) : null}

      {/* Organization Level Analytics */}
      {isAdmin && org && (
        <div className="space-y-6 pt-4 border-t border-slate-800">
          <div>
            <h2 className="text-xl font-bold font-display text-white">Organization Operations Overview</h2>
            <p className="text-xs text-slate-400">Aggregated organizational health and productivity metrics</p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
            <div className="glass-panel p-5 rounded-2xl border border-slate-800 space-y-1">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Total Workspaces</span>
              <div className="text-3xl font-black font-display text-white">{org.totalProjects}</div>
              <div className="text-xs text-slate-500">{org.activeProjects} active project initiatives</div>
            </div>

            <div className="glass-panel p-5 rounded-2xl border border-slate-800 space-y-1">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Total Workforce</span>
              <div className="text-3xl font-black font-display text-white">{org.totalUsers}</div>
              <div className="text-xs text-slate-500">Across {org.totalDepartments} departments</div>
            </div>

            <div className="glass-panel p-5 rounded-2xl border border-slate-800 space-y-1">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Task Velocity</span>
              <div className="text-3xl font-black font-display text-white">{org.completedTasks}</div>
              <div className="text-xs text-slate-500">Of {org.totalTasks} total tasks completed</div>
            </div>

            <div className="glass-panel p-5 rounded-2xl border border-slate-800 space-y-1">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Leave Utilization</span>
              <div className="text-3xl font-black font-display text-white">{org.approvedLeaves}</div>
              <div className="text-xs text-slate-500">Approved leave requests logged</div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
