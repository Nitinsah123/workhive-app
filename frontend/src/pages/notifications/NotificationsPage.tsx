import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { notificationApi } from '../../api/services';
import { useNavigate } from 'react-router-dom';
import {
  Bell,
  CheckCheck,
  Clock,
  ArrowRight,
  Inbox,
  CheckSquare,
  Calendar,
  Megaphone,
} from 'lucide-react';

export const NotificationsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const { data: notifsData, isLoading } = useQuery({
    queryKey: ['notifications-all'],
    queryFn: () => notificationApi.getMy(),
  });

  const markReadMutation = useMutation({
    mutationFn: (id: string) => notificationApi.markRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications-all'] });
      queryClient.invalidateQueries({ queryKey: ['notifications-unread-count'] });
    },
  });

  const markAllReadMutation = useMutation({
    mutationFn: () => notificationApi.markAllRead(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications-all'] });
      queryClient.invalidateQueries({ queryKey: ['notifications-unread-count'] });
    },
  });

  const notifications = notifsData?.data?.content || [];

  const handleNotificationClick = (item: any) => {
    if (!item.read) {
      markReadMutation.mutate(item.id);
    }
    if (item.actionUrl) {
      navigate(item.actionUrl);
    }
  };

  return (
    <div className="space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider border border-indigo-500/20 mb-2">
            <Bell className="w-3.5 h-3.5" />
            Alerts & Activity
          </div>
          <h1 className="text-3xl font-black font-display text-white">Notifications</h1>
          <p className="text-slate-400 text-sm">
            Real-time updates, task assignments, leave reviews, and announcements.
          </p>
        </div>

        {notifications.some((n) => !n.read) && (
          <button
            onClick={() => markAllReadMutation.mutate()}
            disabled={markAllReadMutation.isPending}
            className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold border border-slate-700 flex items-center gap-1.5 transition-all self-start sm:self-auto"
          >
            <CheckCheck className="w-4 h-4 text-indigo-400" />
            <span>Mark All as Read</span>
          </button>
        )}
      </div>

      {isLoading ? (
        <div className="text-center py-20 text-slate-500 text-sm">Loading notifications...</div>
      ) : notifications.length === 0 ? (
        <div className="glass-panel p-12 text-center rounded-3xl">
          <Bell className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <h3 className="text-lg font-bold text-white font-display">No Notifications</h3>
          <p className="text-sm text-slate-400 mt-1">You are all caught up on your workspace updates!</p>
        </div>
      ) : (
        <div className="space-y-3">
          {notifications.map((item) => (
            <div
              key={item.id}
              onClick={() => handleNotificationClick(item)}
              className={`p-5 rounded-2xl border transition-all cursor-pointer flex items-center justify-between gap-4 ${
                !item.read
                  ? 'bg-slate-900/90 border-indigo-500/30 shadow-lg shadow-indigo-500/5'
                  : 'bg-slate-900/40 border-slate-800/80 hover:bg-slate-800/40'
              }`}
            >
              <div className="flex items-start gap-4">
                <div
                  className={`p-2.5 rounded-xl border flex-shrink-0 ${
                    !item.read
                      ? 'bg-indigo-500/10 text-indigo-400 border-indigo-500/30'
                      : 'bg-slate-800 text-slate-400 border-slate-700'
                  }`}
                >
                  <Bell className="w-5 h-5" />
                </div>

                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold text-white">{item.title}</span>
                    {!item.read && (
                      <span className="w-2 h-2 rounded-full bg-indigo-500" />
                    )}
                  </div>
                  {item.message && (
                    <p className="text-xs text-slate-400">{item.message}</p>
                  )}
                  <div className="text-[10px] text-slate-500 font-mono">
                    {new Date(item.createdAt).toLocaleString()}
                  </div>
                </div>
              </div>

              {item.actionUrl && (
                <ArrowRight className="w-4 h-4 text-slate-500 flex-shrink-0" />
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
