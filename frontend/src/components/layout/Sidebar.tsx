import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import {
  LayoutDashboard,
  Inbox,
  Users,
  FolderKanban,
  CheckSquare,
  Clock,
  Calendar,
  FileText,
  Megaphone,
  Bell,
  BarChart3,
  GitBranch,
  Activity,
  Settings,
  Building2,
  Briefcase,
  UserCheck,
  Archive,
} from 'lucide-react';

export const Sidebar: React.FC = () => {
  const { user, tenant } = useAuthStore();
  const role = user?.role;

  const isAdmin = role === 'TENANT_ADMIN';
  const isManager = role === 'MANAGER';

  const navItems = [
    { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    ...(isAdmin || isManager
      ? [{ to: '/action-center', label: 'Action Center', icon: Inbox, badge: 'Hub' }]
      : []),
    { to: '/projects', label: 'Projects', icon: FolderKanban },
    { to: '/tasks', label: 'Tasks & Kanban', icon: CheckSquare },
    { to: '/attendance', label: 'Attendance', icon: Clock },
    { to: '/leave', label: 'Leave', icon: Calendar },
    { to: '/documents', label: 'Documents', icon: FileText },
    { to: '/announcements', label: 'Announcements', icon: Megaphone },
    { to: '/notifications', label: 'Notifications', icon: Bell },
    { to: '/reports', label: 'Reports & Analytics', icon: BarChart3 },
    { to: '/activity', label: 'Work Activity', icon: Activity },
    { to: '/integrations', label: 'Integrations', icon: GitBranch },
    ...(isAdmin || isManager
      ? [
          { to: '/people', label: 'People', icon: Users },
          { to: '/departments', label: 'Departments', icon: Building2 },
          { to: '/teams', label: 'Teams', icon: Briefcase },
        ]
      : []),
    ...(isAdmin
      ? [
          { to: '/archive', label: 'Archive', icon: Archive },
          { to: '/settings', label: 'Settings', icon: Settings },
        ]
      : []),
  ];

  return (
    <aside className="w-64 bg-slate-900 border-r border-slate-800/80 flex flex-col h-screen fixed left-0 top-0 z-30 select-none">
      {/* Brand Header */}
      <div className="h-16 px-5 flex items-center gap-3 border-b border-slate-800/80 bg-slate-900/50">
        <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-500 flex items-center justify-center text-white font-black text-lg shadow-lg shadow-indigo-500/20">
          W
        </div>
        <div className="flex flex-col overflow-hidden">
          <span className="font-display font-bold text-base tracking-tight text-white flex items-center gap-1.5">
            WorkHive
            <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 font-semibold border border-indigo-500/30">
              SaaS
            </span>
          </span>
          <span className="text-xs text-slate-400 truncate font-medium">
            {tenant?.name || 'Workspace'}
          </span>
        </div>
      </div>

      {/* Navigation Links */}
      <nav className="flex-1 overflow-y-auto px-3 py-4 space-y-1">
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex items-center justify-between px-3 py-2.5 rounded-lg text-sm font-medium transition-all ${
                  isActive
                    ? 'bg-indigo-600/15 text-indigo-400 border border-indigo-500/30 shadow-sm'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                }`
              }
            >
              <div className="flex items-center gap-3">
                <Icon className="w-4 h-4" />
                <span>{item.label}</span>
              </div>
              {item.badge && (
                <span className="text-[10px] uppercase font-bold tracking-wider px-1.5 py-0.5 rounded bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                  {item.badge}
                </span>
              )}
            </NavLink>
          );
        })}
      </nav>

      {/* Current User Pill */}
      <div className="p-3 border-t border-slate-800/80 bg-slate-900/50">
        <NavLink
          to="/profile"
          className="flex items-center gap-3 p-2 rounded-lg hover:bg-slate-800/70 transition-colors"
        >
          <div className="w-9 h-9 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-indigo-400 font-bold text-sm">
            {user?.fullName?.charAt(0) || 'U'}
          </div>
          <div className="flex flex-col min-w-0 flex-1">
            <span className="text-sm font-semibold text-slate-200 truncate">
              {user?.fullName}
            </span>
            <span className="text-xs text-indigo-400 font-mono truncate">
              {user?.employeeCode}
            </span>
          </div>
          <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700">
            {user?.role === 'TENANT_ADMIN' ? 'Admin' : user?.role === 'MANAGER' ? 'Manager' : 'Emp'}
          </span>
        </NavLink>
      </div>
    </aside>
  );
};
