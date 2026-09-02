import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { announcementApi, departmentApi, teamApi } from '../../api/services';
import { Modal } from '../../components/common/Modal';
import { useAuthStore } from '../../store/authStore';
import {
  Megaphone,
  Plus,
  Calendar,
  Building,
  Briefcase,
  Globe,
  Sparkles,
} from 'lucide-react';

export const AnnouncementsPage: React.FC = () => {
  const { user } = useAuthStore();
  const isAdmin = user?.role === 'TENANT_ADMIN' || user?.role === 'MANAGER';

  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [targetType, setTargetType] = useState('ORGANIZATION');
  const [targetId, setTargetId] = useState('');
  const [priority, setPriority] = useState('NORMAL');

  const queryClient = useQueryClient();

  const { data: announcementsData, isLoading } = useQuery({
    queryKey: ['announcements-my'],
    queryFn: () => announcementApi.getMy(),
  });

  const { data: deptsData } = useQuery({
    queryKey: ['departments-active'],
    queryFn: () => departmentApi.getAll(),
    enabled: isAdmin,
  });

  const { data: teamsData } = useQuery({
    queryKey: ['teams-active'],
    queryFn: () => teamApi.getAll(),
    enabled: isAdmin,
  });

  const createMutation = useMutation({
    mutationFn: (payload: any) => announcementApi.create(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['announcements-my'] });
      setCreateModalOpen(false);
      setTitle('');
      setContent('');
    },
  });

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    createMutation.mutate({
      title,
      content,
      targetType,
      targetId: targetId || null,
      priority,
    });
  };

  const announcements = announcementsData?.data || [];
  const depts = deptsData?.data || [];
  const teams = teamsData?.data || [];

  return (
    <div className="space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider border border-indigo-500/20 mb-2">
            <Megaphone className="w-3.5 h-3.5" />
            Company Broadcasts
          </div>
          <h1 className="text-3xl font-black font-display text-white">Announcements</h1>
          <p className="text-slate-400 text-sm">
            Audience-targeted organization, department, and team updates.
          </p>
        </div>

        {isAdmin && (
          <button
            onClick={() => setCreateModalOpen(true)}
            className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs shadow-lg shadow-indigo-600/25 flex items-center gap-2 transition-all self-start sm:self-auto"
          >
            <Plus className="w-4 h-4" />
            <span>Post Announcement</span>
          </button>
        )}
      </div>

      {/* Announcements List */}
      {isLoading ? (
        <div className="text-center py-20 text-slate-500 text-sm">Loading broadcasts...</div>
      ) : announcements.length === 0 ? (
        <div className="glass-panel p-12 text-center rounded-3xl">
          <Megaphone className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <h3 className="text-lg font-bold text-white font-display">No Announcements</h3>
          <p className="text-sm text-slate-400 mt-1">There are no active company broadcasts right now.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {announcements.map((item) => (
            <div
              key={item.id}
              className="glass-panel p-6 rounded-3xl border border-slate-800 space-y-3"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold uppercase tracking-wider px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 flex items-center gap-1">
                    {item.targetType === 'ORGANIZATION' ? (
                      <Globe className="w-3 h-3" />
                    ) : item.targetType === 'DEPARTMENT' ? (
                      <Building className="w-3 h-3" />
                    ) : (
                      <Briefcase className="w-3 h-3" />
                    )}
                    {item.targetType}
                  </span>
                  <span className="text-xs text-slate-500 font-mono">
                    {new Date(item.publishedAt).toLocaleDateString()}
                  </span>
                </div>
                {item.priority === 'URGENT' && (
                  <span className="text-[10px] uppercase font-bold px-2 py-0.5 rounded bg-rose-500/10 text-rose-400 border border-rose-500/20">
                    Urgent Notice
                  </span>
                )}
              </div>

              <h3 className="text-xl font-bold text-white font-display">{item.title}</h3>
              <p className="text-sm text-slate-300 leading-relaxed whitespace-pre-line">{item.content}</p>
            </div>
          ))}
        </div>
      )}

      {/* Post Announcement Modal */}
      <Modal isOpen={createModalOpen} onClose={() => setCreateModalOpen(false)} title="Post Announcement">
        <form onSubmit={handleCreate} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Broadcast Title *
            </label>
            <input
              type="text"
              required
              placeholder="e.g. Q3 All-Hands Meeting"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Target Audience
              </label>
              <select
                value={targetType}
                onChange={(e) => setTargetType(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              >
                <option value="ORGANIZATION">Everyone (All Organization)</option>
                <option value="DEPARTMENT">Specific Department</option>
                <option value="TEAM">Specific Team</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Priority
              </label>
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              >
                <option value="NORMAL">Normal</option>
                <option value="URGENT">Urgent</option>
              </select>
            </div>
          </div>

          {targetType === 'DEPARTMENT' && (
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Select Department
              </label>
              <select
                value={targetId}
                onChange={(e) => setTargetId(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              >
                <option value="">Choose department...</option>
                {depts.map((d) => (
                  <option key={d.id} value={d.id}>{d.name}</option>
                ))}
              </select>
            </div>
          )}

          {targetType === 'TEAM' && (
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Select Team
              </label>
              <select
                value={targetId}
                onChange={(e) => setTargetId(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              >
                <option value="">Choose team...</option>
                {teams.map((t) => (
                  <option key={t.id} value={t.id}>{t.name}</option>
                ))}
              </select>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Message Content *
            </label>
            <textarea
              rows={4}
              required
              placeholder="Write the announcement message..."
              value={content}
              onChange={(e) => setContent(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div className="flex justify-end gap-3 pt-3">
            <button
              type="button"
              onClick={() => setCreateModalOpen(false)}
              className="px-4 py-2.5 rounded-xl bg-slate-800 text-slate-300 text-xs font-bold"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="px-5 py-2.5 rounded-xl bg-indigo-600 text-white text-xs font-bold shadow-lg shadow-indigo-600/25 disabled:opacity-50"
            >
              Publish Broadcast
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
