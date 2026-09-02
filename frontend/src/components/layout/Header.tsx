import React, { useState } from 'react';
import { useAuthStore } from '../../store/authStore';
import { useUIStore } from '../../store/uiStore';
import { Search, Bell, LogOut, Building, ShieldCheck, User } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { notificationApi } from '../../api/services';

export const Header: React.FC = () => {
  const { user, tenant, logout } = useAuthStore();
  const { setSearchModalOpen } = useUIStore();
  const navigate = useNavigate();

  const { data: unreadData } = useQuery({
    queryKey: ['notifications-unread-count'],
    queryFn: () => notificationApi.getUnreadCount(),
    refetchInterval: 10000,
  });

  const unreadCount = unreadData?.data?.count || 0;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="h-16 bg-slate-900/80 backdrop-blur border-b border-slate-800/80 px-6 flex items-center justify-between sticky top-0 z-20">
      {/* Left: Tenant identity */}
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-800/60 border border-slate-700/50">
          <Building className="w-4 h-4 text-indigo-400" />
          <span className="text-sm font-semibold text-slate-200">{tenant?.name}</span>
          <span className="text-xs px-1.5 py-0.5 rounded bg-slate-700/50 text-slate-400 font-mono">
            {tenant?.code}
          </span>
        </div>
      </div>

      {/* Center: Global Search Bar trigger */}
      <button
        onClick={() => setSearchModalOpen(true)}
        className="flex items-center gap-3 px-4 py-2 w-80 rounded-xl bg-slate-800/50 border border-slate-700/50 text-slate-400 hover:text-slate-200 hover:border-slate-600 transition-all text-sm group"
      >
        <Search className="w-4 h-4 text-slate-400 group-hover:text-indigo-400 transition-colors" />
        <span className="flex-1 text-left">Search tasks, projects, people...</span>
        <kbd className="text-[10px] px-1.5 py-0.5 rounded bg-slate-700/60 text-slate-400 border border-slate-600">
          ⌘K
        </kbd>
      </button>

      {/* Right: Notifications & Profile actions */}
      <div className="flex items-center gap-3">
        {/* Notifications Icon with live badge */}
        <button
          onClick={() => navigate('/notifications')}
          className="relative p-2 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800/60 transition-colors"
          title="Notifications"
        >
          <Bell className="w-5 h-5" />
          {unreadCount > 0 && (
            <span className="absolute top-1.5 right-1.5 w-4 h-4 rounded-full bg-indigo-500 text-white text-[10px] font-bold flex items-center justify-center animate-pulse">
              {unreadCount > 9 ? '9+' : unreadCount}
            </span>
          )}
        </button>

        {/* User Code badge */}
        <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-lg bg-indigo-950/40 border border-indigo-500/20 text-indigo-300 text-xs font-mono">
          <ShieldCheck className="w-3.5 h-3.5" />
          {user?.employeeCode}
        </div>

        {/* Logout button */}
        <button
          onClick={handleLogout}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-rose-400 hover:bg-rose-500/10 border border-transparent hover:border-rose-500/20 text-xs font-medium transition-colors"
          title="Log Out"
        >
          <LogOut className="w-4 h-4" />
          <span>Logout</span>
        </button>
      </div>
    </header>
  );
};
