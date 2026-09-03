import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { archiveApi } from '../../api/services';
import { Modal } from '../../components/common/Modal';
import {
  Archive,
  RotateCcw,
  Trash2,
  AlertTriangle,
  Users,
  Building2,
  Briefcase,
  FolderKanban,
  CheckSquare,
  Search,
  Clock,
  ShieldAlert,
  Calendar,
  CheckCircle2,
} from 'lucide-react';

interface ArchivedItem {
  id: string;
  type: 'USER' | 'DEPARTMENT' | 'TEAM' | 'PROJECT' | 'TASK';
  title: string;
  description: string;
  status: string;
  archivedAt: string;
  archivedBy?: string;
  metadata?: Record<string, any>;
}

export const ArchivePage: React.FC = () => {
  const [selectedType, setSelectedType] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');

  // Confirmation Modals State
  const [itemToRestore, setItemToRestore] = useState<ArchivedItem | null>(null);
  const [itemToDelete, setItemToDelete] = useState<ArchivedItem | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['archive-summary', selectedType],
    queryFn: async () => {
      const res = await archiveApi.getAll(selectedType);
      return res.data;
    },
  });

  const restoreMutation = useMutation({
    mutationFn: ({ type, id }: { type: string; id: string }) => archiveApi.restore(type, id),
    onSuccess: (_, vars) => {
      queryClient.invalidateQueries({ queryKey: ['archive-summary'] });
      queryClient.invalidateQueries({ queryKey: ['users-all'] });
      queryClient.invalidateQueries({ queryKey: ['departments-active'] });
      queryClient.invalidateQueries({ queryKey: ['teams-active'] });
      queryClient.invalidateQueries({ queryKey: ['projects-all'] });
      queryClient.invalidateQueries({ queryKey: ['tasks-all'] });
      setItemToRestore(null);
      setActionError(null);
      setActionSuccess(`${vars.type} restored successfully and returned to active views.`);
      setTimeout(() => setActionSuccess(null), 4000);
    },
    onError: (err: any) => {
      setActionError(err.response?.data?.message || err.message || 'Failed to restore item');
    },
  });

  const permanentDeleteMutation = useMutation({
    mutationFn: ({ type, id }: { type: string; id: string }) => archiveApi.permanentDelete(type, id),
    onSuccess: (_, vars) => {
      queryClient.invalidateQueries({ queryKey: ['archive-summary'] });
      queryClient.invalidateQueries({ queryKey: ['users-all'] });
      queryClient.invalidateQueries({ queryKey: ['departments-active'] });
      queryClient.invalidateQueries({ queryKey: ['teams-active'] });
      setItemToDelete(null);
      setActionError(null);
      setActionSuccess(`${vars.type} has been permanently deleted.`);
      setTimeout(() => setActionSuccess(null), 4000);
    },
    onError: (err: any) => {
      setActionError(err.response?.data?.message || err.message || 'Failed to permanently delete item');
    },
  });

  const items: ArchivedItem[] = data?.items || [];

  const filteredItems = items.filter((item) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      item.title?.toLowerCase().includes(q) ||
      item.description?.toLowerCase().includes(q) ||
      item.type?.toLowerCase().includes(q)
    );
  });

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'USER':
        return <Users className="w-4 h-4 text-emerald-400" />;
      case 'DEPARTMENT':
        return <Building2 className="w-4 h-4 text-indigo-400" />;
      case 'TEAM':
        return <Briefcase className="w-4 h-4 text-amber-400" />;
      case 'PROJECT':
        return <FolderKanban className="w-4 h-4 text-violet-400" />;
      case 'TASK':
        return <CheckSquare className="w-4 h-4 text-sky-400" />;
      default:
        return <Archive className="w-4 h-4 text-slate-400" />;
    }
  };

  const getTypeBadgeClass = (type: string) => {
    switch (type) {
      case 'USER':
        return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
      case 'DEPARTMENT':
        return 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20';
      case 'TEAM':
        return 'bg-amber-500/10 text-amber-400 border-amber-500/20';
      case 'PROJECT':
        return 'bg-violet-500/10 text-violet-400 border-violet-500/20';
      case 'TASK':
        return 'bg-sky-500/10 text-sky-400 border-sky-500/20';
      default:
        return 'bg-slate-500/10 text-slate-400 border-slate-500/20';
    }
  };

  const tabs = [
    { id: 'ALL', label: 'All Records', count: data?.totalCount ?? 0, icon: Archive },
    { id: 'USER', label: 'Users', count: data?.usersCount ?? 0, icon: Users },
    { id: 'DEPARTMENT', label: 'Departments', count: data?.departmentsCount ?? 0, icon: Building2 },
    { id: 'TEAM', label: 'Teams', count: data?.teamsCount ?? 0, icon: Briefcase },
    { id: 'PROJECT', label: 'Projects', count: data?.projectsCount ?? 0, icon: FolderKanban },
    { id: 'TASK', label: 'Tasks', count: data?.tasksCount ?? 0, icon: CheckSquare },
  ];

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-slate-900/60 p-6 rounded-2xl border border-slate-800/80 backdrop-blur-xl">
        <div className="flex items-center gap-3.5">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-tr from-rose-500/20 to-amber-500/20 border border-rose-500/30 flex items-center justify-center text-rose-400 shadow-lg shadow-rose-500/10">
            <Archive className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-white flex items-center gap-2">
              Workspace Archive
              <span className="text-xs px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 font-semibold border border-slate-700">
                {data?.totalCount ?? 0} Stored
              </span>
            </h1>
            <p className="text-xs text-slate-400 mt-0.5">
              Securely preserve, restore, or permanently purge archived organization records with full audit trail
            </p>
          </div>
        </div>

        {/* Global Success Notification */}
        {actionSuccess && (
          <div className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-semibold animate-fade-in">
            <CheckCircle2 className="w-4 h-4 shrink-0" />
            <span>{actionSuccess}</span>
          </div>
        )}
      </div>

      {/* Filter Tabs & Search Bar */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-3">
        {/* Category Tabs */}
        <div className="flex flex-wrap gap-1.5 p-1 rounded-xl bg-slate-900 border border-slate-800">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            const isSelected = selectedType === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setSelectedType(tab.id)}
                className={`flex items-center gap-2 px-3.5 py-2 rounded-lg text-xs font-semibold transition-all ${
                  isSelected
                    ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/25'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                }`}
              >
                <Icon className="w-3.5 h-3.5" />
                <span>{tab.label}</span>
                <span
                  className={`text-[10px] px-1.5 py-0.2 rounded-full ${
                    isSelected ? 'bg-indigo-700 text-indigo-100' : 'bg-slate-800 text-slate-400'
                  }`}
                >
                  {tab.count}
                </span>
              </button>
            );
          })}
        </div>

        {/* Search */}
        <div className="relative min-w-[240px]">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search archived records..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2 text-xs rounded-xl bg-slate-900 border border-slate-800 text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 transition-colors"
          />
        </div>
      </div>

      {/* Main Content List */}
      {isLoading ? (
        <div className="p-12 text-center text-slate-500 text-sm">Loading archived records...</div>
      ) : filteredItems.length === 0 ? (
        <div className="p-16 text-center rounded-2xl bg-slate-900/40 border border-slate-800/80">
          <div className="w-12 h-12 rounded-full bg-slate-800/60 border border-slate-700/60 flex items-center justify-center text-slate-500 mx-auto mb-3">
            <Archive className="w-5 h-5" />
          </div>
          <p className="text-sm font-semibold text-slate-300">No archived records found</p>
          <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
            Records archived by Tenant Admins will be safely preserved here until restored or permanently deleted.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredItems.map((item) => (
            <div
              key={`${item.type}-${item.id}`}
              className="p-5 rounded-2xl bg-slate-900/70 border border-slate-800/80 hover:border-slate-700 transition-all flex flex-col justify-between group backdrop-blur-sm"
            >
              <div className="space-y-3">
                {/* Header: Type Badge & Status */}
                <div className="flex items-center justify-between gap-2">
                  <span
                    className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-[10px] font-bold uppercase tracking-wider border ${getTypeBadgeClass(
                      item.type
                    )}`}
                  >
                    {getTypeIcon(item.type)}
                    <span>{item.type}</span>
                  </span>

                  <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-rose-500/10 text-rose-400 border border-rose-500/20">
                    ARCHIVED
                  </span>
                </div>

                {/* Title & Description */}
                <div>
                  <h3 className="text-sm font-bold text-white group-hover:text-indigo-400 transition-colors line-clamp-1">
                    {item.title}
                  </h3>
                  <p className="text-xs text-slate-400 mt-1 line-clamp-2">{item.description}</p>
                </div>

                {/* Metadata Pills */}
                {item.metadata && Object.keys(item.metadata).length > 0 && (
                  <div className="flex flex-wrap gap-1.5 pt-1">
                    {Object.entries(item.metadata).map(([k, v]) => {
                      if (!v) return null;
                      return (
                        <span
                          key={k}
                          className="text-[10px] px-2 py-0.5 rounded-md bg-slate-800/80 text-slate-300 border border-slate-700/50"
                        >
                          <span className="text-slate-400 capitalize">{k}: </span>
                          <span className="font-semibold">{String(v)}</span>
                        </span>
                      );
                    })}
                  </div>
                )}
              </div>

              {/* Footer: Archived Date & Action Buttons */}
              <div className="mt-5 pt-3.5 border-t border-slate-800/80 flex items-center justify-between gap-2">
                <div className="flex items-center gap-1.5 text-[11px] text-slate-400">
                  <Clock className="w-3.5 h-3.5 text-slate-400" />
                  <span>
                    {item.archivedAt ? new Date(item.archivedAt).toLocaleDateString() : 'Preserved'}
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  {/* Restore Button */}
                  <button
                    onClick={() => {
                      setActionError(null);
                      setItemToRestore(item);
                    }}
                    className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-indigo-600/15 hover:bg-indigo-600/25 text-indigo-400 border border-indigo-500/30 text-xs font-semibold transition-colors"
                    title="Restore record to active workspace"
                  >
                    <RotateCcw className="w-3.5 h-3.5" />
                    <span>Restore</span>
                  </button>

                  {/* Permanent Delete Button */}
                  <button
                    onClick={() => {
                      setActionError(null);
                      setItemToDelete(item);
                    }}
                    className="flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-rose-600/10 hover:bg-rose-600/20 text-rose-400 border border-rose-500/20 text-xs font-semibold transition-colors"
                    title="Permanently purge record"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                    <span>Delete</span>
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Restore Confirmation Modal */}
      {itemToRestore && (
        <Modal
          isOpen={!!itemToRestore}
          onClose={() => setItemToRestore(null)}
          title={`Restore ${itemToRestore.type} — ${itemToRestore.title}`}
        >
          <div className="space-y-4">
            {actionError && (
              <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-semibold flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 shrink-0" />
                <span>{actionError}</span>
              </div>
            )}
            <p className="text-sm text-slate-300">
              Are you sure you want to unarchive and restore{' '}
              <strong className="text-white">{itemToRestore.title}</strong>?
            </p>
            <div className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800 text-xs text-slate-400 space-y-1.5">
              <div>• Status will be restored to ACTIVE.</div>
              <div>• Record will immediately re-appear across all normal workspace views.</div>
              <div>• This restore action will be logged in the workspace audit trail.</div>
            </div>
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setItemToRestore(null)}
                className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={restoreMutation.isPending}
                onClick={() =>
                  restoreMutation.mutate({ type: itemToRestore.type, id: itemToRestore.id })
                }
                className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs flex items-center gap-1.5 disabled:opacity-50"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>{restoreMutation.isPending ? 'Restoring...' : 'Confirm Restore'}</span>
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* Permanent Delete Confirmation Modal */}
      {itemToDelete && (
        <Modal
          isOpen={!!itemToDelete}
          onClose={() => setItemToDelete(null)}
          title={`Delete Permanently — ${itemToDelete.title}`}
        >
          <div className="space-y-4">
            {actionError && (
              <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-semibold flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 shrink-0" />
                <span>{actionError}</span>
              </div>
            )}
            <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs font-semibold flex items-start gap-2.5">
              <ShieldAlert className="w-4 h-4 shrink-0 mt-0.5 text-rose-400" />
              <div>
                <strong>Warning: Permanent and Irreversible Action</strong>
                <p className="font-normal text-rose-300/80 mt-1">
                  Permanently deleting this {itemToDelete.type.toLowerCase()} removes all authentication credentials,
                  sessions, and identity data. Dependent business records are safely detached to prevent corruption.
                </p>
              </div>
            </div>
            <p className="text-sm text-slate-300">
              Are you completely certain you want to permanently delete{' '}
              <strong className="text-white">{itemToDelete.title}</strong>?
            </p>
            <div className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800 text-xs text-slate-400 space-y-1">
              <div>• Record will NOT appear in Archive.</div>
              <div>• Cannot be undone or recovered.</div>
            </div>
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setItemToDelete(null)}
                className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={permanentDeleteMutation.isPending}
                onClick={() =>
                  permanentDeleteMutation.mutate({ type: itemToDelete.type, id: itemToDelete.id })
                }
                className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs flex items-center gap-1.5 disabled:opacity-50"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>
                  {permanentDeleteMutation.isPending ? 'Deleting...' : 'Delete Permanently'}
                </span>
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
