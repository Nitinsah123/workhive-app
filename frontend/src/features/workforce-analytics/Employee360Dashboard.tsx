import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  ArrowLeft,
  Calendar,
  Clock,
  CheckCircle2,
  GitCommit,
  GitPullRequest,
  GitBranch,
  Flame,
  ShieldCheck,
  Activity,
  Layers,
  Radio,
  ExternalLink,
  Code2,
  AlertCircle,
  TrendingUp,
  Cpu,
  BarChart3,
  PieChart as PieIcon,
  RefreshCw,
  FolderGit2,
  Search,
  History,
} from 'lucide-react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  PieChart,
  Pie,
  Cell,
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
  Radar,
  AreaChart,
  Area,
} from 'recharts';
import api from '../../api/client';
import { Employee360DashboardDTO } from './types';
import { useActivityStream } from './useActivityStream';

export const Employee360Dashboard: React.FC = () => {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();

  const [timeRange, setTimeRange] = useState<'TODAY' | '7D' | '30D' | 'CURRENT_SPRINT'>('TODAY');
  const [activeTab, setActiveTab] = useState<'OVERVIEW' | 'TASKS' | 'TELEMETRY'>('OVERVIEW');
  const [taskSearchQuery, setTaskSearchQuery] = useState('');
  const [taskStatusFilter, setTaskStatusFilter] = useState<'ALL' | 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE'>('ALL');
  const [hourlySeriesFilter, setHourlySeriesFilter] = useState<'ALL' | 'ACTIVE' | 'IDLE'>('ALL');

  // SSE real-time stream listener for this employee
  const { isConnected, lastEvent, events } = useActivityStream(userId);

  // Fetch 360 Analytics data
  const {
    data: dashboard,
    isLoading,
    isError,
    refetch,
  } = useQuery<Employee360DashboardDTO>({
    queryKey: ['employee-360', userId, timeRange],
    queryFn: async () => {
      const res = await api.get<Employee360DashboardDTO>(
        `/v1/analytics/employees/${userId}/360`,
        { params: { range: timeRange } }
      );
      return res.data;
    },
    enabled: Boolean(userId),
    refetchInterval: 12000,
  });

  const getPresenceBadge = (status: string) => {
    switch (status) {
      case 'Coding / Active':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 shadow-sm shadow-emerald-500/10">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            Coding / Active
          </span>
        );
      case 'In Sync':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">
            <span className="w-2 h-2 rounded-full bg-cyan-400 animate-ping" />
            In Sync
          </span>
        );
      case 'Idle':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-amber-500/10 text-amber-400 border border-amber-500/20">
            <span className="w-2 h-2 rounded-full bg-amber-400" />
            Idle
          </span>
        );
      case 'Offline':
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-slate-800 text-slate-400 border border-slate-700">
            <span className="w-2 h-2 rounded-full bg-slate-500" />
            Offline
          </span>
        );
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-[75vh] flex flex-col items-center justify-center space-y-4">
        <div className="w-12 h-12 border-4 border-indigo-500/20 border-t-indigo-500 rounded-full animate-spin" />
        <p className="text-sm text-slate-400 font-medium tracking-wide">
          Assembling Power BI 360° telemetry dashboard...
        </p>
      </div>
    );
  }

  if (isError || !dashboard) {
    return (
      <div className="p-8 max-w-4xl mx-auto space-y-6">
        <button
          onClick={() => navigate(-1)}
          className="inline-flex items-center gap-2 text-xs font-bold text-slate-400 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back</span>
        </button>

        <div className="glass-panel p-8 rounded-3xl border border-rose-500/20 bg-rose-500/5 text-center space-y-4">
          <AlertCircle className="w-10 h-10 text-rose-400 mx-auto" />
          <h2 className="text-xl font-bold text-white">Employee Profile Not Found</h2>
          <p className="text-sm text-slate-400">
            Could not retrieve 360° telemetry for employee ID: {userId}.
          </p>
          <button
            onClick={() => refetch()}
            className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold transition-all shadow-lg shadow-indigo-600/30"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  // Combined timeline items: API timeline + live incoming SSE events
  const combinedTimeline = [
    ...events,
    ...dashboard.activityTimeline.filter((t) => !events.some((e) => e.id === t.id)),
  ];

  // Filtered assigned tasks list
  const filteredTasks = (dashboard.assignedTasks || []).filter((t) => {
    const q = taskSearchQuery.toLowerCase().trim();
    const matchesSearch =
      !q ||
      t.title.toLowerCase().includes(q) ||
      (t.id && t.id.toLowerCase().includes(q)) ||
      (t.projectName && t.projectName.toLowerCase().includes(q));

    if (!matchesSearch) return false;

    if (taskStatusFilter === 'ALL') return true;
    const statusUpper = (t.status || '').toUpperCase();
    if (taskStatusFilter === 'TODO') return statusUpper === 'TODO';
    if (taskStatusFilter === 'IN_PROGRESS') return statusUpper === 'IN_PROGRESS';
    if (taskStatusFilter === 'IN_REVIEW') return statusUpper === 'IN_REVIEW' || statusUpper === 'REVIEW';
    if (taskStatusFilter === 'DONE') return statusUpper === 'DONE' || statusUpper === 'COMPLETED';
    return true;
  });

  return (
    <div className="space-y-6 pb-16 animate-fade-in max-w-7xl mx-auto">
      {/* Top Bar Navigation & Stream Status */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <button
          onClick={() => navigate(-1)}
          className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl bg-slate-900/80 hover:bg-slate-800 text-slate-300 hover:text-white text-xs font-bold transition-all border border-slate-800 shadow-sm"
        >
          <ArrowLeft className="w-4 h-4 text-indigo-400" />
          <span>Back to Roster</span>
        </button>

        <div className="flex items-center gap-3">
          <div
            className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-bold border transition-all ${
              isConnected
                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20 shadow-sm shadow-emerald-500/10'
                : 'bg-amber-500/10 text-amber-400 border-amber-500/20'
            }`}
          >
            <Radio className={`w-3.5 h-3.5 ${isConnected ? 'animate-pulse text-emerald-400' : 'text-amber-400'}`} />
            <span>{isConnected ? 'Live Telemetry Connected' : 'Reconnecting...'}</span>
          </div>

          <button
            onClick={() => refetch()}
            className="p-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-400 hover:text-white border border-slate-800 transition-colors"
            title="Refresh telemetry"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* 1. Employee 360° Header Hero Card */}
      <div className="relative overflow-hidden rounded-3xl border border-slate-800 bg-[#0B0F19] p-6 sm:p-8 shadow-2xl">
        <div className="absolute -top-24 -right-24 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute -bottom-24 -left-24 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl pointer-events-none" />

        <div className="relative z-10 flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          {/* Profile Identity & Focus */}
          <div className="flex items-start sm:items-center gap-4 sm:gap-5">
            <div className="relative shrink-0">
              {dashboard.avatarUrl ? (
                <img
                  src={dashboard.avatarUrl}
                  alt={dashboard.fullName}
                  className="w-16 h-16 sm:w-20 sm:h-20 rounded-3xl object-cover border-2 border-indigo-500/30 shadow-xl"
                />
              ) : (
                <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-3xl bg-gradient-to-br from-indigo-600/40 via-purple-600/30 to-cyan-600/30 border-2 border-indigo-500/30 flex items-center justify-center text-xl sm:text-2xl font-black text-indigo-200 font-display shadow-xl">
                  {dashboard.fullName
                    .split(' ')
                    .map((n) => n[0])
                    .slice(0, 2)
                    .join('')}
                </div>
              )}
            </div>

            <div className="space-y-1.5">
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-[10px] font-mono uppercase tracking-widest font-extrabold px-2 py-0.5 rounded-md bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                  {dashboard.employeeCode}
                </span>
                {getPresenceBadge(dashboard.presenceStatus)}
                <span className="text-xs text-slate-400 font-medium">{dashboard.email}</span>
              </div>

              <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight font-display">
                {dashboard.fullName}
              </h1>

              <div className="flex flex-wrap items-center gap-3 text-xs text-slate-300">
                <span className="font-bold text-indigo-300">{dashboard.organizationalRole}</span>
                <span className="text-slate-600">•</span>
                <span>Dept: <strong className="text-white">{dashboard.departmentName}</strong></span>
                <span className="text-slate-600">•</span>
                <span>Team: <strong className="text-white">{dashboard.teamName}</strong></span>
              </div>

              {/* Current Live Focus Badge */}
              <div className="pt-2 flex flex-wrap items-center gap-2 text-xs">
                <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-xl bg-slate-900/90 border border-indigo-500/30 text-slate-200">
                  <Cpu className="w-3.5 h-3.5 text-indigo-400" />
                  <span className="text-slate-400">Live Focus:</span>
                  <strong className="text-white truncate max-w-xs">{dashboard.activeFocusTask}</strong>
                </div>

                <div className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-900/90 border border-slate-800 text-slate-300 font-mono text-[11px]">
                  <GitBranch className="w-3.5 h-3.5 text-cyan-400" />
                  <span>{dashboard.activeGitBranch}</span>
                </div>
              </div>
            </div>
          </div>

          {/* Time-Range Slicer Controls */}
          <div className="shrink-0 space-y-2">
            <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider block text-left lg:text-right">
              Time Range Slicer
            </span>
            <div className="flex items-center gap-1 p-1 rounded-2xl bg-slate-900/90 border border-slate-800 shadow-inner">
              {(['TODAY', '7D', '30D', 'CURRENT_SPRINT'] as const).map((range) => (
                <button
                  key={range}
                  onClick={() => setTimeRange(range)}
                  className={`px-3 py-2 rounded-xl text-xs font-bold transition-all ${
                    timeRange === range
                      ? 'bg-gradient-to-r from-indigo-600 to-indigo-700 text-white shadow-md shadow-indigo-600/30'
                      : 'text-slate-400 hover:text-white'
                  }`}
                >
                  {range === 'TODAY'
                    ? 'Today'
                    : range === '7D'
                    ? '7 Days'
                    : range === '30D'
                    ? '30 Days'
                    : 'Sprint'}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* 2. KPI Summary Strip (4 Power BI Metric Cards) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* KPI 1: Active Time & Efficiency */}
        <div className="relative overflow-hidden rounded-3xl border border-slate-800 bg-[#0E1322] p-5 shadow-xl group hover:border-indigo-500/40 transition-all">
          <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-indigo-500 to-cyan-500" />
          <div className="flex items-center justify-between">
            <span className="text-[11px] uppercase tracking-wider font-bold text-slate-400">
              Active Time & Efficiency
            </span>
            <div className="p-2 rounded-xl bg-indigo-500/10 text-indigo-400">
              <Clock className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-3xl font-black text-white font-display">
              {dashboard.activeWorkHours}h
            </span>
            <span className="text-xs font-bold text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full border border-emerald-500/20">
              {dashboard.efficiencyPercentage}% Ratio
            </span>
          </div>
          <p className="text-[11px] text-slate-400 mt-2">
            Tracked via attendance & active ide/git activity sessions
          </p>
        </div>

        {/* KPI 2: Task Completion Rate */}
        <div className="relative overflow-hidden rounded-3xl border border-slate-800 bg-[#0E1322] p-5 shadow-xl group hover:border-emerald-500/40 transition-all">
          <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-emerald-500 to-teal-500" />
          <div className="flex items-center justify-between">
            <span className="text-[11px] uppercase tracking-wider font-bold text-slate-400">
              Task Completion Rate
            </span>
            <div className="p-2 rounded-xl bg-emerald-500/10 text-emerald-400">
              <CheckCircle2 className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-3xl font-black text-white font-display">
              {dashboard.taskCompletionRatePercentage}%
            </span>
            <span className="text-xs text-slate-400 font-medium">
              {dashboard.tasksCompleted}/{dashboard.tasksAssigned} done
            </span>
          </div>
          <div className="mt-2.5 h-1.5 w-full bg-slate-800 rounded-full overflow-hidden">
            <div
              className="h-full bg-gradient-to-r from-emerald-500 to-cyan-500 rounded-full"
              style={{ width: `${Math.min(100, dashboard.taskCompletionRatePercentage)}%` }}
            />
          </div>
        </div>

        {/* KPI 3: Real-Time Git Velocity */}
        <div className="relative overflow-hidden rounded-3xl border border-slate-800 bg-[#0E1322] p-5 shadow-xl group hover:border-cyan-500/40 transition-all">
          <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-cyan-500 to-blue-500" />
          <div className="flex items-center justify-between">
            <span className="text-[11px] uppercase tracking-wider font-bold text-slate-400">
              Git Velocity
            </span>
            <div className="p-2 rounded-xl bg-cyan-500/10 text-cyan-400">
              <GitCommit className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-3xl font-black text-white font-display">
              {dashboard.commitsPushed}
            </span>
            <span className="text-xs font-bold text-cyan-400">
              commits • {dashboard.prsOpenedOrMerged} PRs
            </span>
          </div>
          <p className="text-[11px] text-slate-400 mt-2">
            ~{dashboard.linesChanged.toLocaleString()} lines impacted across tracked repositories
          </p>
        </div>

        {/* KPI 4: Current Streak & SLA */}
        <div className="relative overflow-hidden rounded-3xl border border-slate-800 bg-[#0E1322] p-5 shadow-xl group hover:border-amber-500/40 transition-all">
          <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-amber-500 to-rose-500" />
          <div className="flex items-center justify-between">
            <span className="text-[11px] uppercase tracking-wider font-bold text-slate-400">
              Streak & SLA Adherence
            </span>
            <div className="p-2 rounded-xl bg-amber-500/10 text-amber-400">
              <Flame className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-3xl font-black text-white font-display">
              {dashboard.currentStreakDays}d
            </span>
            <span className="text-xs font-bold text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded-full border border-amber-500/20">
              {dashboard.slaAdherencePercentage}% SLA
            </span>
          </div>
          <p className="text-[11px] text-slate-400 mt-2">
            Consecutive active delivery streak with zero missed deliverables
          </p>
        </div>
      </div>

      {/* Navigation Sub-Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-800 pb-2">
        <button
          onClick={() => setActiveTab('OVERVIEW')}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            activeTab === 'OVERVIEW'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/30'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          <BarChart3 className="w-4 h-4" />
          <span>Interactive Power BI Visuals</span>
        </button>

        <button
          onClick={() => setActiveTab('TELEMETRY')}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            activeTab === 'TELEMETRY'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/30'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          <Activity className="w-4 h-4" />
          <span>Live Telemetry & Git Push Feed ({combinedTimeline.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('TASKS')}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            activeTab === 'TASKS'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/30'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          <Layers className="w-4 h-4" />
          <span>Assigned Work ({dashboard.assignedTasks.length})</span>
        </button>
      </div>

      {/* TAB 1: INTERACTIVE CHARTS & VISUALS */}
      {activeTab === 'OVERVIEW' && (
        <div className="space-y-6">
          {/* Charts Row 1: Hourly Heatmap Bar + Task Breakdown Donut */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Chart 1: Hourly Activity Bar */}
            <div className="lg:col-span-2 rounded-3xl border border-slate-800 bg-[#0E1322] p-6 shadow-xl space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-base font-bold text-white font-display">
                    Hourly Focus & Activity Distribution
                  </h3>
                  <p className="text-xs text-slate-400 mt-0.5">
                    Active productive coding / task work vs. idle time per hour
                  </p>
                </div>
                <div className="flex items-center gap-2 text-xs">
                  <button
                    type="button"
                    onClick={() => setHourlySeriesFilter((prev) => (prev === 'ACTIVE' ? 'ALL' : 'ACTIVE'))}
                    className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg font-semibold cursor-pointer transition-all ${
                      hourlySeriesFilter === 'ACTIVE'
                        ? 'bg-indigo-500/20 text-indigo-300 ring-1 ring-indigo-500/40 shadow-sm'
                        : hourlySeriesFilter === 'IDLE'
                        ? 'opacity-40 hover:opacity-80 text-indigo-400'
                        : 'text-indigo-400 hover:bg-slate-800/60'
                    }`}
                    title="Click to toggle Active series view"
                  >
                    <span className="w-2.5 h-2.5 rounded-sm bg-indigo-500 shrink-0" />
                    <span>Active (mins)</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setHourlySeriesFilter((prev) => (prev === 'IDLE' ? 'ALL' : 'IDLE'))}
                    className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg font-semibold cursor-pointer transition-all ${
                      hourlySeriesFilter === 'IDLE'
                        ? 'bg-slate-700/60 text-slate-200 ring-1 ring-slate-600 shadow-sm'
                        : hourlySeriesFilter === 'ACTIVE'
                        ? 'opacity-40 hover:opacity-80 text-slate-400'
                        : 'text-slate-400 hover:bg-slate-800/60'
                    }`}
                    title="Click to toggle Idle / Sync series view"
                  >
                    <span className="w-2.5 h-2.5 rounded-sm bg-slate-700 shrink-0" />
                    <span>Idle / Sync</span>
                  </button>
                </div>
              </div>

              <div className="h-72 w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={dashboard.hourlyActivity}>
                    <XAxis dataKey="hour" stroke="#64748b" fontSize={11} tickLine={false} />
                    <YAxis stroke="#64748b" fontSize={11} tickLine={false} unit="m" domain={[0, 60]} />
                    <Tooltip
                      content={({ active, payload, label }) => {
                        if (active && payload && payload.length) {
                          const act = payload.find((p) => p.dataKey === 'activeMinutes')?.value ?? 0;
                          const idle = payload.find((p) => p.dataKey === 'idleMinutes')?.value ?? 0;
                          const total = Number(act) + Number(idle);
                          return (
                            <div className="p-3 rounded-2xl bg-[#0B0F19] border border-slate-700 text-xs space-y-1.5 shadow-xl">
                              <p className="font-bold text-white font-mono">{label} Slot</p>
                              <div className="flex items-center justify-between gap-4 text-indigo-400">
                                <span>Active Focus:</span>
                                <strong>{act} mins</strong>
                              </div>
                              <div className="flex items-center justify-between gap-4 text-slate-400">
                                <span>Idle / In Sync:</span>
                                <strong>{idle} mins</strong>
                              </div>
                              <div className="flex items-center justify-between gap-4 text-slate-300 border-t border-slate-800 pt-1 font-bold">
                                <span>Recorded Total:</span>
                                <span>{total} mins</span>
                              </div>
                            </div>
                          );
                        }
                        return null;
                      }}
                    />
                    {(hourlySeriesFilter === 'ALL' || hourlySeriesFilter === 'ACTIVE') && (
                      <Bar dataKey="activeMinutes" name="Active" fill="#6366F1" radius={[4, 4, 0, 0]} stackId="a" />
                    )}
                    {(hourlySeriesFilter === 'ALL' || hourlySeriesFilter === 'IDLE') && (
                      <Bar dataKey="idleMinutes" name="Idle" fill="#334155" radius={[4, 4, 0, 0]} stackId="a" />
                    )}
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Chart 2: Task Breakdown Donut */}
            <div className="rounded-3xl border border-slate-800 bg-[#0E1322] p-6 shadow-xl space-y-4 flex flex-col justify-between">
              <div>
                <h3 className="text-base font-bold text-white font-display">Task Status Breakdown</h3>
                <p className="text-xs text-slate-400 mt-0.5">Distribution across Kanban states</p>
              </div>

              <div className="h-56 w-full flex items-center justify-center">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={dashboard.taskBreakdown}
                      cx="50%"
                      cy="50%"
                      innerRadius={55}
                      outerRadius={80}
                      paddingAngle={4}
                      dataKey="value"
                    >
                      {dashboard.taskBreakdown.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip
                      contentStyle={{
                        backgroundColor: '#0B0F19',
                        borderColor: '#334155',
                        borderRadius: '0.75rem',
                        fontSize: '12px',
                      }}
                    />
                  </PieChart>
                </ResponsiveContainer>
              </div>

              <div className="grid grid-cols-2 gap-2 pt-2 border-t border-slate-800/80 text-xs">
                {dashboard.taskBreakdown.map((item) => (
                  <div key={item.name} className="flex items-center gap-2">
                    <span
                      className="w-2.5 h-2.5 rounded-full shrink-0"
                      style={{ backgroundColor: item.color }}
                    />
                    <span className="text-slate-400">{item.name}:</span>
                    <strong className="text-white">{item.value}</strong>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Charts Row 2: Role Performance Radar + Velocity Trajectory */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Chart 3: Role-Specific Performance Radar */}
            <div className="rounded-3xl border border-slate-800 bg-[#0E1322] p-6 shadow-xl space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-base font-bold text-white font-display">
                    Role-Adaptive Competency Matrix
                  </h3>
                  <p className="text-xs text-slate-400 mt-0.5">
                    Dynamic metrics adjusted for {dashboard.organizationalRole}
                  </p>
                </div>
                <span className="text-[11px] font-bold text-indigo-400 px-2.5 py-1 rounded-lg bg-indigo-500/10 border border-indigo-500/20">
                  Role Adapted
                </span>
              </div>

              <div className="h-64 w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <RadarChart data={dashboard.rolePerformance}>
                    <PolarGrid stroke="#1e293b" />
                    <PolarAngleAxis dataKey="subject" stroke="#94a3b8" fontSize={11} />
                    <PolarRadiusAxis stroke="#475569" fontSize={10} domain={[0, 100]} />
                    <Radar
                      name="Employee Score"
                      dataKey="score"
                      stroke="#06B6D4"
                      fill="#06B6D4"
                      fillOpacity={0.4}
                    />
                    <Radar
                      name="Benchmark"
                      dataKey="benchmark"
                      stroke="#6366F1"
                      fill="#6366F1"
                      fillOpacity={0.15}
                    />
                    <Legend wrapperStyle={{ fontSize: '11px', color: '#94a3b8' }} />
                  </RadarChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Chart 4: Velocity Trend Area Chart */}
            <div className="rounded-3xl border border-slate-800 bg-[#0E1322] p-6 shadow-xl space-y-4">
              <div>
                <h3 className="text-base font-bold text-white font-display">
                  Delivery Velocity Trajectory
                </h3>
                <p className="text-xs text-slate-400 mt-0.5">
                  Daily code commits vs. completed work items
                </p>
              </div>

              <div className="h-64 w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={dashboard.dailyVelocity}>
                    <defs>
                      <linearGradient id="colorCommits" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#06B6D4" stopOpacity={0.4} />
                        <stop offset="95%" stopColor="#06B6D4" stopOpacity={0} />
                      </linearGradient>
                      <linearGradient id="colorTasks" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#10B981" stopOpacity={0.4} />
                        <stop offset="95%" stopColor="#10B981" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <XAxis dataKey="date" stroke="#64748b" fontSize={11} />
                    <YAxis stroke="#64748b" fontSize={11} />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: '#0B0F19',
                        borderColor: '#334155',
                        borderRadius: '0.75rem',
                        fontSize: '12px',
                      }}
                    />
                    <Area
                      type="monotone"
                      dataKey="commits"
                      name="Commits"
                      stroke="#06B6D4"
                      fillOpacity={1}
                      fill="url(#colorCommits)"
                    />
                    <Area
                      type="monotone"
                      dataKey="tasksDone"
                      name="Tasks Completed"
                      stroke="#10B981"
                      fillOpacity={1}
                      fill="url(#colorTasks)"
                    />
                    <Legend wrapperStyle={{ fontSize: '11px', color: '#94a3b8' }} />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: LIVE TELEMETRY & GIT PUSH FEED */}
      {activeTab === 'TELEMETRY' && (
        <div className="rounded-3xl border border-slate-800 bg-[#0E1322] p-6 sm:p-8 shadow-xl space-y-6">
          <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-800 pb-4">
            <div>
              <h3 className="text-lg font-bold text-white font-display flex items-center gap-2">
                <FolderGit2 className="w-5 h-5 text-indigo-400" />
                <span>Live Real-Time Telemetry & Git Push Feed</span>
              </h3>
              <p className="text-xs text-slate-400 mt-1">
                Real-time activity stream capturing commits, PR merges, Kanban transitions, and heartbeats
              </p>
            </div>

            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse" />
              <span className="text-xs font-bold text-emerald-400">Stream listening via SSE</span>
            </div>
          </div>

          <div className="space-y-4">
            {combinedTimeline.map((item, idx) => (
              <div
                key={item.id || idx}
                className="p-4 rounded-2xl bg-slate-900/70 border border-slate-800/80 hover:border-indigo-500/30 transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3"
              >
                <div className="flex items-start gap-3">
                  <div
                    className={`p-2.5 rounded-xl shrink-0 mt-0.5 ${
                      item.eventType === 'GIT_PUSH'
                        ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/20'
                        : item.eventType === 'GIT_PR'
                        ? 'bg-purple-500/10 text-purple-400 border border-purple-500/20'
                        : item.eventType === 'TASK_COMPLETED'
                        ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                        : 'bg-indigo-500/10 text-indigo-400 border border-indigo-500/20'
                    }`}
                  >
                    {item.eventType === 'GIT_PUSH' ? (
                      <GitCommit className="w-4 h-4" />
                    ) : item.eventType === 'GIT_PR' ? (
                      <GitPullRequest className="w-4 h-4" />
                    ) : (
                      <CheckCircle2 className="w-4 h-4" />
                    )}
                  </div>

                  <div className="space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-xs font-bold text-white">{item.title}</span>
                      {item.commitSha && (
                        <span className="text-[10px] font-mono font-bold bg-slate-800 text-cyan-300 px-1.5 py-0.5 rounded border border-slate-700">
                          {item.commitSha.slice(0, 7)}
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-slate-400">{item.description}</p>
                    <div className="flex items-center gap-3 text-[11px] text-slate-500">
                      {item.repository && <span>Repo: {item.repository}</span>}
                      {item.branch && <span>Branch: {item.branch}</span>}
                    </div>
                  </div>
                </div>

                <div className="text-[11px] text-slate-500 font-mono shrink-0">
                  {new Date(item.timestamp).toLocaleTimeString([], {
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit',
                  })}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* TAB 3: ASSIGNED & PENDING WORK */}
      {activeTab === 'TASKS' && (
        <div className="rounded-3xl border border-slate-800 bg-[#0E1322] p-6 sm:p-8 shadow-xl space-y-6">
          <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-800 pb-4">
            <div>
              <h3 className="text-lg font-bold text-white font-display">Assigned Work & Deliverables</h3>
              <p className="text-xs text-slate-400 mt-1">
                Active tasks, sprints, and priorities assigned to {dashboard.fullName}
              </p>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold text-indigo-300 bg-indigo-500/10 px-3 py-1.5 rounded-xl border border-indigo-500/20">
                {filteredTasks.length} of {dashboard.assignedTasks.length} Tasks
              </span>
            </div>
          </div>

          {/* Search bar & Status filter pills */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="relative flex-1 max-w-md">
              <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Search tasks by title, key, ID..."
                value={taskSearchQuery}
                onChange={(e) => setTaskSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-2 bg-slate-900/90 border border-slate-800 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 transition-colors"
              />
            </div>

            <div className="flex flex-wrap items-center gap-1.5">
              {(['ALL', 'TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'] as const).map((status) => (
                <button
                  key={status}
                  onClick={() => setTaskStatusFilter(status)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all border ${
                    taskStatusFilter === status
                      ? 'bg-indigo-600 text-white border-indigo-500 shadow-sm shadow-indigo-600/30'
                      : 'bg-slate-900/80 text-slate-400 hover:text-white border-slate-800'
                  }`}
                >
                  {status === 'ALL'
                    ? 'All'
                    : status === 'TODO'
                    ? 'To Do'
                    : status === 'IN_PROGRESS'
                    ? 'In Progress'
                    : status === 'IN_REVIEW'
                    ? 'In Review'
                    : 'Done'}
                </button>
              ))}
            </div>
          </div>

          <div className="space-y-3">
            {filteredTasks.length === 0 ? (
              <div className="text-center py-12 border border-dashed border-slate-800 rounded-2xl">
                <p className="text-sm font-semibold text-slate-400">No tasks match your filters</p>
                <p className="text-xs text-slate-500 mt-1">Try adjusting the search query or status filter</p>
              </div>
            ) : (
              filteredTasks.map((t) => (
                <div
                  key={t.id}
                  onClick={() => navigate(`/tasks?taskId=${t.id}`)}
                  className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 hover:border-indigo-500/50 hover:bg-slate-800/40 cursor-pointer transition-all space-y-3 group"
                  title="Click to open task in Kanban"
                >
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div className="space-y-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-sm font-bold text-white">{t.title}</span>
                        {t.overdue && (
                          <span className="text-[10px] font-bold text-rose-400 bg-rose-500/10 px-2 py-0.5 rounded-full border border-rose-500/20">
                            Overdue
                          </span>
                        )}
                        {t.reviewCount > 0 && (
                          <span className="text-[10px] font-bold text-purple-400 bg-purple-500/10 px-2 py-0.5 rounded-full border border-purple-500/20 flex items-center gap-1">
                            <History className="w-3 h-3" />
                            Reviewed {t.reviewCount}x
                          </span>
                        )}
                      </div>
                      <span className="text-xs text-slate-500">Project: {t.projectName || 'Core Workspace'}</span>
                    </div>

                    <div className="flex flex-wrap items-center gap-2">
                      <span
                        className={`text-[11px] font-bold px-2.5 py-1 rounded-lg border ${
                          t.priority === 'HIGH' || t.priority === 'URGENT'
                            ? 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                            : t.priority === 'MEDIUM'
                            ? 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20'
                            : 'bg-slate-800 text-slate-400 border-slate-700'
                        }`}
                      >
                        {t.priority}
                      </span>
                      <span
                        className={`text-[11px] font-bold px-2.5 py-1 rounded-lg border ${
                          t.status === 'DONE' || t.status === 'COMPLETED'
                            ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                            : t.status === 'IN_PROGRESS'
                            ? 'bg-blue-500/10 text-blue-400 border-blue-500/20'
                            : t.status === 'IN_REVIEW' || t.status === 'REVIEW'
                            ? 'bg-cyan-500/10 text-cyan-400 border-cyan-500/20'
                            : 'bg-slate-800 text-slate-300 border-slate-700'
                        }`}
                      >
                        {t.status}
                      </span>
                    </div>
                  </div>

                  {/* Task Lifecycle & Review Audit History Timestamps */}
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-2 border-t border-slate-800/80 text-[11px]">
                    <div>
                      <span className="text-slate-500 block">Assigned Date:</span>
                      <span className="font-mono text-slate-300">
                        {t.assignedAt ? new Date(t.assignedAt).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' }) : '—'}
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-500 block">Started Date:</span>
                      <span className="font-mono text-slate-300">
                        {t.startedAt ? new Date(t.startedAt).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' }) : 'Not started'}
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-500 block">Completed Date:</span>
                      <span className="font-mono text-slate-300">
                        {t.completedAt
                          ? new Date(t.completedAt).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' })
                          : (t.status === 'DONE' || t.status === 'COMPLETED' ? 'Completed' : 'In Progress')}
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-500 block">Due Date:</span>
                      <span className={`font-mono ${t.overdue ? 'text-rose-400 font-bold' : 'text-slate-300'}`}>
                        {t.dueDate || 'No deadline'}
                      </span>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
};
