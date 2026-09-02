import React, { useState, useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { activityApi, userApi, exportApi } from '../../api/services';
import { useAuthStore } from '../../store/authStore';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import {
  Activity,
  GitCommit,
  GitPullRequest,
  CheckCircle,
  FolderKanban,
  FileText,
  Clock,
  Calendar,
  Sparkles,
  Download,
  Flame,
  Zap,
  TrendingUp,
  Filter,
} from 'lucide-react';

export const ActivityFeedPage: React.FC = () => {
  const { user: currentUser, tenant } = useAuthStore();
  const isAdmin = currentUser?.role === 'TENANT_ADMIN';
  const isManager = currentUser?.role === 'MANAGER';
  const isEmployee = currentUser?.role === 'EMPLOYEE';

  const [selectedUserId, setSelectedUserId] = useState<string>('');
  const [hoveredDay, setHoveredDay] = useState<any | null>(null);

  const effectiveUserId = isEmployee ? (currentUser?.id || '') : selectedUserId;
  const queryClient = useQueryClient();

  const { data: activitiesData, isLoading: activitiesLoading } = useQuery({
    queryKey: ['activities-stream', effectiveUserId],
    queryFn: () => activityApi.getTenant(0, 50, effectiveUserId || undefined),
  });

  const { data: usersData } = useQuery({
    queryKey: ['users-active'],
    queryFn: () => userApi.getActive(),
    enabled: !isEmployee,
  });

  const { data: heatmapData, isLoading: heatmapLoading } = useQuery({
    queryKey: ['activities-heatmap', effectiveUserId],
    queryFn: () => activityApi.getHeatmap(effectiveUserId || undefined),
  });

  const activities = activitiesData?.data?.content || [];
  const users = usersData?.data || [];
  const heatmap = heatmapData?.data || { totalActivities: 0, activeDays: 0, maxDayCount: 0, days: [] };

  useEffect(() => {
    const tenantId = tenant?.id;
    if (!tenantId) return;

      const apiBase = import.meta.env.VITE_API_BASE_URL || '/api';
      const wsUrl = apiBase.replace(/\/api\/?$/, '') + '/ws';
      const client = new Client({
        webSocketFactory: () => new SockJS(wsUrl),
        reconnectDelay: 5000,
        debug: () => {},
      });

      client.onConnect = () => {
        if (isEmployee && currentUser?.id) {
          client.subscribe(`/topic/tenant.${tenantId}.user.${currentUser.id}.activities`, () => {
            queryClient.invalidateQueries({ queryKey: ['activities-stream'] });
            queryClient.invalidateQueries({ queryKey: ['activities-heatmap'] });
          });
        } else {
          if (effectiveUserId) {
            client.subscribe(`/topic/tenant.${tenantId}.user.${effectiveUserId}.activities`, () => {
              queryClient.invalidateQueries({ queryKey: ['activities-stream'] });
              queryClient.invalidateQueries({ queryKey: ['activities-heatmap'] });
            });
          } else {
            client.subscribe(`/topic/tenant.${tenantId}.activities`, () => {
              queryClient.invalidateQueries({ queryKey: ['activities-stream'] });
              queryClient.invalidateQueries({ queryKey: ['activities-heatmap'] });
            });
          }
        }
      };

      client.activate();

      return () => {
        client.deactivate();
      };
    } catch (e) {
      console.warn('Real-time STOMP connection note:', e);
    }
  }, [tenant?.id, currentUser?.id, isEmployee, effectiveUserId, queryClient]);

  const handleExportCsv = async () => {
    try {
      const res = await exportApi.activityCsv();
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `workspace-activity-${new Date().toISOString().slice(0, 10)}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      alert('Failed to download activity CSV export');
    }
  };

  const getActivityIcon = (type: string, source: string) => {
    if (source === 'GITHUB') return GitCommit;
    if (type?.includes('TASK')) return CheckCircle;
    if (type?.includes('PROJECT')) return FolderKanban;
    if (type?.includes('ATTENDANCE')) return Clock;
    if (type?.includes('LEAVE')) return Calendar;
    if (type?.includes('DOCUMENT')) return FileText;
    return Activity;
  };

  const getIntensityColor = (intensity: number) => {
    switch (intensity) {
      case 1:
        return 'bg-indigo-950/80 border border-indigo-800/50 hover:border-indigo-400';
      case 2:
        return 'bg-indigo-700/80 border border-indigo-500 hover:border-indigo-300';
      case 3:
        return 'bg-indigo-500 border border-indigo-300 hover:border-white shadow-sm shadow-indigo-500/30';
      case 4:
        return 'bg-emerald-400 border border-emerald-300 hover:border-white shadow-md shadow-emerald-400/40';
      default:
        return 'bg-slate-900/90 border border-slate-800/80 hover:border-slate-700';
    }
  };

  // Group 365 days into 53 weeks (columns) x 7 days (rows: Sun-Sat)
  const days = heatmap.days || [];
  const weeks: any[][] = [];
  let currentWeek: any[] = [];

  days.forEach((day: any, idx: number) => {
    const d = new Date(day.date);
    const dayOfWeek = d.getUTCDay(); // 0 is Sunday

    if (idx === 0) {
      // Pad beginning of first week
      for (let i = 0; i < dayOfWeek; i++) {
        currentWeek.push(null);
      }
    }

    currentWeek.push(day);

    if (currentWeek.length === 7) {
      weeks.push(currentWeek);
      currentWeek = [];
    }
  });

  if (currentWeek.length > 0) {
    while (currentWeek.length < 7) {
      currentWeek.push(null);
    }
    weeks.push(currentWeek);
  }

  return (
    <div className="space-y-8 pb-12">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider border border-indigo-500/20 mb-2">
            <Activity className="w-3.5 h-3.5" />
            Real-Time Audit & Event Stream
          </div>
          <h1 className="text-3xl font-black font-display text-white">Work Activity & Velocity</h1>
          <p className="text-slate-400 text-sm">
            12-month contribution heatmap and normalized stream across tasks, projects, code commits, and documents.
          </p>
        </div>

        <div className="flex items-center gap-3">
          {!isEmployee && (
            <div className="flex items-center gap-2 px-3 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs">
              <Filter className="w-3.5 h-3.5 text-slate-400" />
              <select
                value={selectedUserId}
                onChange={(e) => setSelectedUserId(e.target.value)}
                className="bg-transparent text-white focus:outline-none cursor-pointer"
              >
                <option value="" className="bg-slate-900 text-white">
                  {isAdmin ? 'All Workspace Members' : 'All Authorized Members'}
                </option>
                {users.map((u: any) => (
                  <option key={u.id} value={u.id} className="bg-slate-900 text-white">
                    {u.fullName || u.email}
                  </option>
                ))}
              </select>
            </div>
          )}

          <button
            onClick={handleExportCsv}
            className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold transition-colors inline-flex items-center gap-2 border border-slate-700"
          >
            <Download className="w-3.5 h-3.5 text-indigo-400" />
            <span>Export CSV</span>
          </button>
        </div>
      </div>

      {/* Contribution Metrics Overview */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="p-5 rounded-2xl bg-gradient-to-br from-slate-900/90 to-slate-900/40 border border-slate-800 flex items-center gap-4">
          <div className="p-3.5 rounded-xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
            <Zap className="w-5 h-5" />
          </div>
          <div>
            <div className="text-2xl font-black text-white font-mono">{heatmap.totalActivities}</div>
            <div className="text-xs text-slate-400">Total Activities (365 Days)</div>
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-gradient-to-br from-slate-900/90 to-slate-900/40 border border-slate-800 flex items-center gap-4">
          <div className="p-3.5 rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <Flame className="w-5 h-5" />
          </div>
          <div>
            <div className="text-2xl font-black text-white font-mono">{heatmap.activeDays}</div>
            <div className="text-xs text-slate-400">Active Work Days</div>
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-gradient-to-br from-slate-900/90 to-slate-900/40 border border-slate-800 flex items-center gap-4">
          <div className="p-3.5 rounded-xl bg-amber-500/10 text-amber-400 border border-amber-500/20">
            <TrendingUp className="w-5 h-5" />
          </div>
          <div>
            <div className="text-2xl font-black text-white font-mono">{heatmap.maxDayCount}</div>
            <div className="text-xs text-slate-400">Peak Daily Activity Velocity</div>
          </div>
        </div>
      </div>

      {/* GitHub-Style 12-Month Contribution Heatmap */}
      <div className="glass-panel p-6 sm:p-8 rounded-3xl space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-indigo-400" />
            <h3 className="text-base font-bold text-white">12-Month Contribution Activity Map</h3>
          </div>
          <div className="flex items-center gap-2 text-xs text-slate-400">
            <span>Less</span>
            <div className="flex items-center gap-1">
              <span className="w-3 h-3 rounded-sm bg-slate-900 border border-slate-800"></span>
              <span className="w-3 h-3 rounded-sm bg-indigo-950 border border-indigo-800/50"></span>
              <span className="w-3 h-3 rounded-sm bg-indigo-700"></span>
              <span className="w-3 h-3 rounded-sm bg-indigo-500"></span>
              <span className="w-3 h-3 rounded-sm bg-emerald-400"></span>
            </div>
            <span>More</span>
          </div>
        </div>

        {heatmapLoading ? (
          <div className="py-12 text-center text-slate-500 text-xs">Loading contribution map...</div>
        ) : (
          <div className="relative overflow-x-auto pb-2">
            <div className="min-w-[760px]">
              {/* Heatmap Grid (7 rows x Weeks columns) */}
              <div className="flex gap-1.5">
                {weeks.map((week, wIdx) => (
                  <div key={wIdx} className="flex flex-col gap-1.5">
                    {week.map((day, dIdx) => {
                      if (!day) {
                        return <div key={`empty-${dIdx}`} className="w-3.5 h-3.5 opacity-0" />;
                      }
                      return (
                        <div
                          key={day.date}
                          onMouseEnter={() => setHoveredDay(day)}
                          onMouseLeave={() => setHoveredDay(null)}
                          className={`w-3.5 h-3.5 rounded-sm transition-all cursor-pointer ${getIntensityColor(day.intensity)}`}
                        />
                      );
                    })}
                  </div>
                ))}
              </div>

              {/* Tooltip display */}
              <div className="h-6 mt-3 text-xs text-slate-400 font-mono flex items-center justify-between">
                {hoveredDay ? (
                  <div className="text-white font-semibold flex items-center gap-2">
                    <span className="text-indigo-400">{hoveredDay.count} contribution{hoveredDay.count === 1 ? '' : 's'}</span>
                    <span>on {new Date(hoveredDay.date).toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' })}</span>
                  </div>
                ) : (
                  <span className="text-slate-500">Hover over any day tile to inspect details</span>
                )}
                <span className="text-slate-500 text-[11px]">Last 365 Days Aggregation</span>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Real-time Activity Timeline Feed */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-bold text-white">Live Workspace Activity Feed</h3>
          <span className="text-xs text-slate-400">{activities.length} recent entries</span>
        </div>

        {activitiesLoading ? (
          <div className="text-center py-20 text-slate-500 text-sm">Loading activity feed...</div>
        ) : activities.length === 0 ? (
          <div className="glass-panel p-12 text-center rounded-3xl">
            <Activity className="w-12 h-12 text-slate-600 mx-auto mb-3" />
            <h3 className="text-lg font-bold text-white font-display">No Activities Yet</h3>
            <p className="text-sm text-slate-400 mt-1">Actions performed across the workspace will appear here.</p>
          </div>
        ) : (
          <div className="glass-panel p-6 sm:p-8 rounded-3xl space-y-6">
            <div className="relative pl-6 space-y-6 before:absolute before:left-2.5 before:top-3 before:bottom-3 before:w-0.5 before:bg-slate-800">
              {activities.map((item) => {
                const Icon = getActivityIcon(item.activityType, item.source);
                return (
                  <div key={item.id} className="relative flex items-start gap-4">
                    <div className="absolute -left-6 p-1.5 rounded-full bg-slate-900 border border-slate-700 text-indigo-400">
                      <Icon className="w-3.5 h-3.5" />
                    </div>

                    <div className="p-4 rounded-2xl bg-slate-900/70 border border-slate-800 flex-1 space-y-1">
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-sm font-bold text-white">{item.title}</span>
                        <span className="text-xs text-slate-500 font-mono shrink-0">
                          {new Date(item.createdAt).toLocaleString()}
                        </span>
                      </div>

                      {item.description && (
                        <p className="text-xs text-slate-400">{item.description}</p>
                      )}

                      <div className="flex items-center gap-2 pt-1">
                        <span className="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-indigo-300 font-mono font-semibold">
                          {item.source}
                        </span>
                        <span className="text-[10px] text-slate-500 font-mono">
                          {item.activityType}
                        </span>
                        {item.externalUrl && (
                          <a
                            href={item.externalUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-[10px] text-indigo-400 hover:text-indigo-300 underline ml-auto font-mono"
                          >
                            Open Link ↗
                          </a>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
