import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { reportApi, userApi } from '../../api/services';
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
  const [selectedUserId, setSelectedUserId] = useState<string>('');

  const { data: usersData } = useQuery({
    queryKey: ['active-users-for-reports'],
    queryFn: () => userApi.getActive(),
    enabled: isAdmin,
  });
  const users = usersData?.data || [];

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

      {/* Individual Employee Export Card */}
      <div className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-4 bg-gradient-to-br from-slate-900/90 to-slate-900/50">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <FileDown className="w-5 h-5 text-indigo-400" />
              <h2 className="text-lg font-bold font-display text-white">Individual Employee Export</h2>
            </div>
            <p className="text-xs text-slate-400 mt-0.5">
              Generate comprehensive 360° dossiers and audit reports in PDF or multi-sheet Excel (.xlsx) format.
            </p>
          </div>

          <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
            {isAdmin && (
              <select
                value={selectedUserId || user?.id || ''}
                onChange={(e) => setSelectedUserId(e.target.value)}
                className="bg-slate-900 border border-slate-700 rounded-xl px-3.5 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
              >
                <option value={user?.id}>Myself ({user?.fullName})</option>
                {users
                  .filter((u: any) => u.id !== user?.id)
                  .map((u: any) => (
                    <option key={u.id} value={u.id}>
                      {u.fullName} ({u.employeeCode || u.email})
                    </option>
                  ))}
              </select>
            )}

            <div className="flex items-center gap-2">
              <button
                onClick={() => {
                  const targetId = selectedUserId || user?.id || '';
                  downloadFile(
                    `/exports/employee/pdf${targetId ? `?userId=${targetId}` : ''}`,
                    `employee-dossier-${new Date().toISOString().slice(0, 10)}.pdf`,
                    'emp-pdf'
                  );
                }}
                disabled={downloading === 'emp-pdf'}
                className="flex-1 sm:flex-none px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold flex items-center justify-center gap-1.5 transition-all disabled:opacity-50 shadow-lg shadow-indigo-600/20"
              >
                <FileText className="w-4 h-4" />
                <span>{downloading === 'emp-pdf' ? 'Generating PDF...' : 'Download PDF'}</span>
              </button>

              <button
                onClick={() => {
                  const targetId = selectedUserId || user?.id || '';
                  downloadFile(
                    `/exports/employee/xlsx${targetId ? `?userId=${targetId}` : ''}`,
                    `employee-report-${new Date().toISOString().slice(0, 10)}.xlsx`,
                    'emp-xlsx'
                  );
                }}
                disabled={downloading === 'emp-xlsx'}
                className="flex-1 sm:flex-none px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold flex items-center justify-center gap-1.5 transition-all disabled:opacity-50 shadow-lg shadow-emerald-600/20"
              >
                <FileSpreadsheet className="w-4 h-4" />
                <span>{downloading === 'emp-xlsx' ? 'Generating XLSX...' : 'Download Excel'}</span>
              </button>
            </div>
          </div>
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
