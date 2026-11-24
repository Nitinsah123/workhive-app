import React, { useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  ArrowLeft,
  Search,
  Users,
  Building2,
  Briefcase,
  GitCommit,
  GitPullRequest,
  Clock,
  CheckCircle2,
  AlertTriangle,
  Radio,
  ExternalLink,
  Sparkles,
  ChevronRight,
  Filter,
} from 'lucide-react';
import api from '../../api/client';
import { UnitRosterResponseDTO, UnitMemberLiveDTO } from './types';
import { useActivityStream } from './useActivityStream';

interface UnitLiveRosterPageProps {
  unitType: 'DEPARTMENT' | 'TEAM';
}

export const UnitLiveRosterPage: React.FC<UnitLiveRosterPageProps> = ({ unitType }) => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedRoleFilter, setSelectedRoleFilter] = useState<string>('ALL');
  const [presenceFilter, setPresenceFilter] = useState<string>('ALL');

  // SSE Live telemetry connection
  const { isConnected, lastEvent } = useActivityStream();

  // Fetch unit live roster
  const endpoint =
    unitType === 'DEPARTMENT'
      ? `/v1/departments/${id}/members/live`
      : `/v1/teams/${id}/members/live`;

  const {
    data: roster,
    isLoading,
    isError,
    refetch,
  } = useQuery<UnitRosterResponseDTO>({
    queryKey: ['unit-live-roster', unitType, id],
    queryFn: async () => {
      const res = await api.get<UnitRosterResponseDTO>(endpoint);
      return res.data;
    },
    enabled: Boolean(id),
    refetchInterval: 15000, // refresh every 15s in addition to SSE
  });

  // Extract distinct roles for filter pills
  const availableRoles = useMemo(() => {
    if (!roster?.members) return [];
    const roles = new Set<string>();
    roster.members.forEach((m) => {
      if (m.organizationalRole) roles.add(m.organizationalRole);
    });
    return Array.from(roles);
  }, [roster]);

  // Filtered members list
  const filteredMembers = useMemo(() => {
    if (!roster?.members) return [];
    return roster.members.filter((m) => {
      const q = searchQuery.toLowerCase().trim();
      const matchesSearch =
        !q ||
        m.fullName.toLowerCase().includes(q) ||
        m.employeeCode.toLowerCase().includes(q) ||
        m.email.toLowerCase().includes(q) ||
        m.organizationalRole.toLowerCase().includes(q);

      const matchesRole =
        selectedRoleFilter === 'ALL' || m.organizationalRole === selectedRoleFilter;

      const matchesPresence =
        presenceFilter === 'ALL' ||
        (presenceFilter === 'ONLINE' && m.presenceStatus !== 'Offline') ||
        m.presenceStatus.toUpperCase() === presenceFilter.toUpperCase();

      return matchesSearch && matchesRole && matchesPresence;
    });
  }, [roster, searchQuery, selectedRoleFilter, presenceFilter]);

  const getPresenceBadge = (status: string) => {
    switch (status) {
      case 'Coding / Active':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 shadow-sm shadow-emerald-500/10">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            Coding / Active
          </span>
        );
      case 'In Sync':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-cyan-500/10 text-cyan-400 border border-cyan-500/20 shadow-sm shadow-cyan-500/10">
            <span className="w-2 h-2 rounded-full bg-cyan-400 animate-ping" />
            In Sync
          </span>
        );
      case 'Idle':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-amber-500/10 text-amber-400 border border-amber-500/20">
            <span className="w-2 h-2 rounded-full bg-amber-400" />
            Idle
          </span>
        );
      case 'Offline':
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-slate-800 text-slate-400 border border-slate-700">
            <span className="w-2 h-2 rounded-full bg-slate-500" />
            Offline
          </span>
        );
    }
  };

  const backLink = unitType === 'DEPARTMENT' ? '/departments' : '/teams';
  const unitLabel = unitType === 'DEPARTMENT' ? 'Department' : 'Team';

  if (isLoading) {
    return (
      <div className="min-h-[70vh] flex flex-col items-center justify-center space-y-4">
        <div className="w-12 h-12 border-4 border-indigo-500/20 border-t-indigo-500 rounded-full animate-spin" />
        <p className="text-sm text-slate-400 font-medium tracking-wide">
          Connecting to real-time workforce telemetry...
        </p>
      </div>
    );
  }

  if (isError || !roster) {
    return (
      <div className="p-8 max-w-4xl mx-auto space-y-6">
        <button
          onClick={() => navigate(backLink)}
          className="inline-flex items-center gap-2 text-xs font-bold text-slate-400 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to {unitType === 'DEPARTMENT' ? 'Departments' : 'Teams'}</span>
        </button>

        <div className="glass-panel p-8 rounded-3xl border border-rose-500/20 bg-rose-500/5 text-center space-y-4">
          <AlertTriangle className="w-10 h-10 text-rose-400 mx-auto" />
          <h2 className="text-xl font-bold text-white">Unit Roster Not Found</h2>
          <p className="text-sm text-slate-400">
            The requested {unitLabel.toLowerCase()} could not be loaded or you do not have permission.
          </p>
          <button
            onClick={() => refetch()}
            className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold transition-all shadow-lg shadow-indigo-600/30"
          >
            Retry Connection
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 pb-12 animate-fade-in max-w-7xl mx-auto">
      {/* Top Breadcrumb & Live Stream Badge */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <button
          onClick={() => navigate(backLink)}
          className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl bg-slate-900/80 hover:bg-slate-800 text-slate-300 hover:text-white text-xs font-bold transition-all border border-slate-800 shadow-sm"
        >
          <ArrowLeft className="w-4 h-4 text-indigo-400" />
          <span>Back to {unitType === 'DEPARTMENT' ? 'Departments' : 'Teams'}</span>
        </button>

        <div className="flex items-center gap-2.5">
          <div
            className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-bold border transition-all ${
              isConnected
                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20 shadow-sm shadow-emerald-500/10'
                : 'bg-amber-500/10 text-amber-400 border-amber-500/20'
            }`}
          >
            <Radio className={`w-3.5 h-3.5 ${isConnected ? 'animate-pulse text-emerald-400' : 'text-amber-400'}`} />
            <span>{isConnected ? 'Telemetry Stream Active' : 'Connecting Stream...'}</span>
          </div>

          {lastEvent && (
            <span className="hidden md:inline-flex text-[11px] text-slate-400 font-mono bg-slate-900 px-3 py-1.5 rounded-full border border-slate-800">
              Latest: {lastEvent.title?.slice(0, 32)}...
            </span>
          )}
        </div>
      </div>

      {/* Header Hero Card */}
      <div className="relative overflow-hidden rounded-3xl border border-slate-800 bg-[#0B0F19] p-6 sm:p-8 shadow-2xl">
        {/* Glow ambient background */}
        <div className="absolute -top-24 -right-24 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute -bottom-24 -left-24 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl pointer-events-none" />

        <div className="relative z-10 flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          <div className="space-y-3">
            <div className="flex items-center gap-3">
              <div className="p-3.5 rounded-2xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 shadow-inner">
                {unitType === 'DEPARTMENT' ? (
                  <Building2 className="w-7 h-7" />
                ) : (
                  <Briefcase className="w-7 h-7" />
                )}
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="text-[10px] uppercase tracking-widest font-extrabold px-2 py-0.5 rounded-md bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                    Live {unitLabel} Roster
                  </span>
                  <span className="text-xs text-slate-500 font-mono">
                    ID: {roster.unitId?.slice(0, 8)}
                  </span>
                </div>
                <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight font-display mt-0.5">
                  {roster.unitName}
                </h1>
              </div>
            </div>

            <p className="text-sm text-slate-400 max-w-2xl leading-relaxed">
              {roster.description ||
                `Real-time operational dashboard and personnel roster aggregated for ${roster.unitName}. Track active focus, telemetry, and individual 360° metrics.`}
            </p>

            {/* Lead & Meta row */}
            <div className="flex flex-wrap items-center gap-4 pt-1 text-xs">
              <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-slate-900/90 border border-slate-800 text-slate-300">
                <span className="text-slate-500">Lead:</span>
                <span className="font-bold text-white">{roster.leadName || 'Unassigned'}</span>
                <span className="text-[11px] text-indigo-400 font-medium">({roster.leadRole})</span>
              </div>

              {unitType === 'DEPARTMENT' && (
                <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-slate-900/90 border border-slate-800 text-slate-300">
                  <Users className="w-3.5 h-3.5 text-cyan-400" />
                  <span>Aggregated across all departmental teams</span>
                </div>
              )}
            </div>
          </div>

          {/* Quick Metrics KPI Pods */}
          <div className="grid grid-cols-3 gap-3 sm:gap-4 shrink-0">
            <div className="p-4 rounded-2xl bg-slate-900/70 border border-slate-800/80 text-center">
              <span className="text-[11px] uppercase tracking-wider font-semibold text-slate-400">Total Staff</span>
              <p className="text-2xl sm:text-3xl font-extrabold text-white font-display mt-1">
                {roster.totalMembers}
              </p>
              <span className="text-[10px] text-slate-500 font-medium">Assigned</span>
            </div>

            <div className="p-4 rounded-2xl bg-emerald-500/5 border border-emerald-500/20 text-center">
              <span className="text-[11px] uppercase tracking-wider font-semibold text-emerald-400">Live Active</span>
              <p className="text-2xl sm:text-3xl font-extrabold text-emerald-400 font-display mt-1">
                {roster.liveOnlineCount}
              </p>
              <span className="text-[10px] text-emerald-500/80 font-medium">Online now</span>
            </div>

            <div className="p-4 rounded-2xl bg-indigo-500/5 border border-indigo-500/20 text-center">
              <span className="text-[11px] uppercase tracking-wider font-semibold text-indigo-400">Active Tasks</span>
              <p className="text-2xl sm:text-3xl font-extrabold text-indigo-400 font-display mt-1">
                {roster.activeTasksCount}
              </p>
              <span className="text-[10px] text-indigo-500/80 font-medium">In flight</span>
            </div>
          </div>
        </div>
      </div>

      {/* Controls: Search & Filter bar */}
      <div className="space-y-3">
        <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3">
          {/* Search bar */}
          <div className="relative flex-1 max-w-md">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              placeholder="Search member by name, ID (e.g. GR-DEV), email, role..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-white placeholder-slate-500 text-xs focus:outline-none focus:border-indigo-500 transition-colors shadow-inner"
            />
          </div>

          {/* Presence Filter Toggle */}
          <div className="flex items-center gap-1 p-1 rounded-xl bg-slate-900 border border-slate-800 text-xs font-semibold self-start md:self-auto">
            <button
              onClick={() => setPresenceFilter('ALL')}
              className={`px-3 py-1.5 rounded-lg transition-all ${
                presenceFilter === 'ALL'
                  ? 'bg-indigo-600 text-white shadow'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              All ({roster.members.length})
            </button>
            <button
              onClick={() => setPresenceFilter('ONLINE')}
              className={`px-3 py-1.5 rounded-lg transition-all flex items-center gap-1.5 ${
                presenceFilter === 'ONLINE'
                  ? 'bg-emerald-600 text-white shadow'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
              Online ({roster.liveOnlineCount})
            </button>
            <button
              onClick={() => setPresenceFilter('OFFLINE')}
              className={`px-3 py-1.5 rounded-lg transition-all ${
                presenceFilter === 'OFFLINE'
                  ? 'bg-slate-700 text-white shadow'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              Offline ({roster.totalMembers - roster.liveOnlineCount})
            </button>
          </div>
        </div>

        {/* Role Pills */}
        {availableRoles.length > 0 && (
          <div className="flex flex-wrap items-center gap-1.5 pt-1">
            <span className="text-[11px] font-semibold text-slate-500 flex items-center gap-1 mr-1">
              <Filter className="w-3 h-3" /> Role:
            </span>
            <button
              onClick={() => setSelectedRoleFilter('ALL')}
              className={`px-2.5 py-1 rounded-lg text-[11px] font-bold transition-colors ${
                selectedRoleFilter === 'ALL'
                  ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/40'
                  : 'bg-slate-900 text-slate-400 border border-slate-800 hover:text-slate-200'
              }`}
            >
              All Roles
            </button>
            {availableRoles.map((role) => (
              <button
                key={role}
                onClick={() => setSelectedRoleFilter(role)}
                className={`px-2.5 py-1 rounded-lg text-[11px] font-bold transition-colors ${
                  selectedRoleFilter === role
                    ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/40'
                    : 'bg-slate-900 text-slate-400 border border-slate-800 hover:text-slate-200'
                }`}
              >
                {role}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Member Roster Grid */}
      {filteredMembers.length === 0 ? (
        <div className="glass-panel p-12 rounded-3xl border border-slate-800 text-center space-y-3">
          <Users className="w-10 h-10 text-slate-600 mx-auto" />
          <h3 className="text-base font-bold text-white">No members match your criteria</h3>
          <p className="text-xs text-slate-400">
            Try adjusting your search query or role/presence filters.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
          {filteredMembers.map((member) => (
            <div
              key={member.userId}
              onClick={() => navigate(`/employees/${member.userId}/360`)}
              className="group glass-panel rounded-3xl border border-slate-800/90 bg-[#0E1322] hover:border-indigo-500/40 hover:bg-[#11172A] cursor-pointer transition-all duration-300 p-5 flex flex-col justify-between shadow-lg relative overflow-hidden"
              title="Click to view 360° Profile"
            >
              {/* Subtle accent border top */}
              <div
                className={`absolute top-0 left-0 right-0 h-1 ${
                  member.presenceStatus === 'Coding / Active'
                    ? 'bg-gradient-to-r from-emerald-500 to-cyan-500'
                    : member.presenceStatus === 'In Sync'
                    ? 'bg-gradient-to-r from-cyan-500 to-indigo-500'
                    : 'bg-slate-800'
                }`}
              />

              <div className="space-y-4">
                {/* Member Profile Header */}
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3">
                    {/* Avatar with live status dot */}
                    <div className="relative">
                      {member.avatarUrl ? (
                        <img
                          src={member.avatarUrl}
                          alt={member.fullName}
                          className="w-12 h-12 rounded-2xl object-cover border border-slate-700"
                        />
                      ) : (
                        <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-indigo-600/30 to-purple-600/30 border border-indigo-500/30 flex items-center justify-center text-sm font-extrabold text-indigo-300 font-display">
                          {member.fullName
                            .split(' ')
                            .map((n) => n[0])
                            .slice(0, 2)
                            .join('')}
                        </div>
                      )}
                      <span
                        className={`absolute -bottom-0.5 -right-0.5 w-3.5 h-3.5 rounded-full border-2 border-[#0E1322] ${
                          member.presenceStatus === 'Coding / Active'
                            ? 'bg-emerald-400'
                            : member.presenceStatus === 'In Sync'
                            ? 'bg-cyan-400'
                            : member.presenceStatus === 'Idle'
                            ? 'bg-amber-400'
                            : 'bg-slate-500'
                        }`}
                      />
                    </div>

                    <div>
                      <div className="flex items-center gap-1.5">
                        <h3 className="text-base font-bold text-white font-display group-hover:text-indigo-300 transition-colors">
                          {member.fullName}
                        </h3>
                      </div>
                      <div className="flex items-center gap-2 mt-0.5">
                        <span className="text-[11px] font-mono text-indigo-400 font-semibold bg-indigo-500/10 px-1.5 py-0.5 rounded border border-indigo-500/20">
                          {member.employeeCode}
                        </span>
                        <span className="text-xs text-slate-400 truncate max-w-[150px]">
                          {member.email}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Presence indicator badge */}
                  <div>{getPresenceBadge(member.presenceStatus)}</div>
                </div>

                {/* Role & Squad */}
                <div className="px-3.5 py-2 rounded-xl bg-slate-900/80 border border-slate-800 flex items-center justify-between text-xs">
                  <span className="text-slate-400 font-medium">Role:</span>
                  <span className="font-bold text-slate-200">{member.organizationalRole}</span>
                </div>

                {/* Assigned Work & Kanban Stats Strip */}
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="p-2.5 rounded-xl bg-slate-900/50 border border-slate-800/80">
                    <span className="text-[10px] uppercase font-bold text-slate-400 block">Assigned Tasks</span>
                    <div className="flex items-center gap-1.5 mt-1">
                      <span className="text-lg font-bold text-white font-display">
                        {member.assignedTasksCount}
                      </span>
                      <span className="text-[10px] text-slate-500">total</span>
                    </div>
                  </div>

                  <div
                    className={`p-2.5 rounded-xl border ${
                      member.overdueTasksCount > 0
                        ? 'bg-rose-500/10 border-rose-500/30 text-rose-300'
                        : 'bg-slate-900/50 border-slate-800/80 text-slate-300'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] uppercase font-bold text-slate-400 block">Pending</span>
                      {member.overdueTasksCount > 0 && (
                        <span className="text-[10px] font-extrabold text-rose-400 flex items-center gap-0.5">
                          <AlertTriangle className="w-2.5 h-2.5" /> {member.overdueTasksCount} overdue
                        </span>
                      )}
                    </div>
                    <div className="flex items-center gap-1.5 mt-1">
                      <span
                        className={`text-lg font-bold font-display ${
                          member.overdueTasksCount > 0 ? 'text-rose-400' : 'text-amber-400'
                        }`}
                      >
                        {member.pendingTasksCount}
                      </span>
                      <span className="text-[10px] text-slate-500">
                        ({member.completedTasksCount} done)
                      </span>
                    </div>
                  </div>
                </div>

                {/* Top Tasks Preview (if any) */}
                {member.topTaskTitles && member.topTaskTitles.length > 0 && (
                  <div className="space-y-1">
                    <span className="text-[10px] uppercase font-bold text-slate-500 tracking-wider">
                      Current Task Focus:
                    </span>
                    <div className="space-y-1">
                      {member.topTaskTitles.slice(0, 2).map((title, idx) => (
                        <div
                          key={idx}
                          className="text-[11px] text-slate-300 truncate bg-slate-900/90 px-2.5 py-1 rounded-lg border border-slate-800/80 flex items-center gap-1.5"
                        >
                          <span className="w-1.5 h-1.5 rounded-full bg-indigo-400 shrink-0" />
                          <span className="truncate">{title}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Hours & Git Activity Today */}
                <div className="flex items-center justify-between pt-2 border-t border-slate-800/80 text-[11px] text-slate-400">
                  <div className="flex items-center gap-1.5">
                    <Clock className="w-3.5 h-3.5 text-indigo-400" />
                    <span>
                      <strong className="text-white font-semibold">{member.activeHoursToday}h</strong> today
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    <span className="flex items-center gap-1 text-slate-300">
                      <GitCommit className="w-3.5 h-3.5 text-cyan-400" />
                      <strong className="text-white">{member.gitCommitsToday}</strong>
                    </span>
                    <span className="flex items-center gap-1 text-slate-300">
                      <GitPullRequest className="w-3.5 h-3.5 text-purple-400" />
                      <strong className="text-white">{member.gitPrsToday}</strong>
                    </span>
                  </div>
                </div>
              </div>

              {/* Primary Action Button: 360° Profile */}
              <div className="pt-4 mt-3 border-t border-slate-800/60">
                <button
                  onClick={() => navigate(`/employees/${member.userId}/360`)}
                  className="w-full py-2.5 px-4 rounded-xl bg-gradient-to-r from-indigo-600 to-indigo-700 hover:from-indigo-500 hover:to-indigo-600 text-white font-bold text-xs transition-all duration-200 flex items-center justify-center gap-2 shadow-lg shadow-indigo-600/20 group/btn"
                >
                  <Sparkles className="w-3.5 h-3.5 text-indigo-200 group-hover/btn:rotate-12 transition-transform" />
                  <span>360° Profile</span>
                  <ChevronRight className="w-3.5 h-3.5 text-indigo-200 group-hover/btn:translate-x-0.5 transition-transform ml-auto" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
