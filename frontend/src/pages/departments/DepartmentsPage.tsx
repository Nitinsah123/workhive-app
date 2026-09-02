import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { departmentApi, userApi } from '../../api/services';
import { Modal } from '../../components/common/Modal';
import {
  Building2,
  Plus,
  Users,
  CheckCircle,
  Briefcase,
  Edit2,
  Trash2,
  AlertCircle,
  Archive,
  Save,
} from 'lucide-react';

export const DepartmentsPage: React.FC = () => {
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [managerId, setManagerId] = useState('');

  // Edit State
  const [editingDept, setEditingDept] = useState<any | null>(null);
  const [editName, setEditName] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editManagerId, setEditManagerId] = useState('');
  const [editStatus, setEditStatus] = useState('ACTIVE');
  const [editError, setEditError] = useState<string | null>(null);

  // Delete/Archive State
  const [deptToArchive, setDeptToArchive] = useState<any | null>(null);
  const [archiveError, setArchiveError] = useState<string | null>(null);

  const queryClient = useQueryClient();

  const { data: deptsData, isLoading } = useQuery({
    queryKey: ['departments-active'],
    queryFn: () => departmentApi.getAll(),
  });

  const { data: usersData } = useQuery({
    queryKey: ['users-active'],
    queryFn: () => userApi.getActive(),
  });

  const createMutation = useMutation({
    mutationFn: (payload: any) => departmentApi.create(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['departments-active'] });
      setCreateModalOpen(false);
      setName('');
      setDescription('');
      setManagerId('');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: any }) => departmentApi.update(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['departments-active'] });
      setEditingDept(null);
      setEditError(null);
    },
    onError: (err: any) => {
      setEditError(err.response?.data?.message || err.message || 'Failed to update department');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => departmentApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['departments-active'] });
      setDeptToArchive(null);
      setArchiveError(null);
    },
    onError: (err: any) => {
      setArchiveError(err.response?.data?.message || err.message || 'Failed to archive department');
    },
  });

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    createMutation.mutate({
      name,
      description,
      managerId: managerId || null,
    });
  };

  const handleOpenEdit = (dept: any) => {
    setEditingDept(dept);
    setEditName(dept.name || '');
    setEditDescription(dept.description || '');
    setEditManagerId(dept.managerId || '');
    setEditStatus(dept.status || 'ACTIVE');
    setEditError(null);
  };

  const handleSaveEdit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingDept) return;
    updateMutation.mutate({
      id: editingDept.id,
      payload: {
        name: editName.trim(),
        description: editDescription.trim(),
        managerId: editManagerId || null,
        status: editStatus,
      },
    });
  };

  const departments = deptsData?.data || [];
  const users = usersData?.data || [];

  const getUserName = (id?: string) => {
    if (!id) return null;
    const u = users.find((x: any) => x.id === id);
    return u ? `${u.fullName} (${u.employeeCode || '—'})` : null;
  };

  return (
    <div className="space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider border border-indigo-500/20 mb-2">
            <Building2 className="w-3.5 h-3.5" />
            Organizational Units
          </div>
          <h1 className="text-3xl font-black font-display text-white">Departments</h1>
          <p className="text-slate-400 text-sm">
            Structure your company departments, assign managers, and manage organizational domains.
          </p>
        </div>

        <button
          onClick={() => setCreateModalOpen(true)}
          className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs shadow-lg shadow-indigo-600/25 flex items-center gap-2 transition-all self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>New Department</span>
        </button>
      </div>

      {isLoading ? (
        <div className="text-center py-20 text-slate-500 text-sm">Loading departments...</div>
      ) : departments.length === 0 ? (
        <div className="glass-panel p-12 text-center rounded-3xl">
          <Building2 className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <h3 className="text-lg font-bold text-white font-display">No Departments Created</h3>
          <p className="text-sm text-slate-400 mt-1">Organize your organization by adding departments.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {departments.map((dept) => (
            <div
              key={dept.id}
              className="glass-panel p-6 rounded-3xl border border-slate-800 space-y-4 flex flex-col justify-between"
            >
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <div className="p-3 w-fit rounded-2xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                    <Building2 className="w-6 h-6" />
                  </div>
                  <span className={`text-xs px-2.5 py-0.5 rounded-full font-bold border ${
                    dept.status === 'ACTIVE'
                      ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                      : 'bg-slate-800 text-slate-400 border-slate-700'
                  }`}>
                    {dept.status || 'ACTIVE'}
                  </span>
                </div>

                <h3 className="text-xl font-bold text-white font-display">{dept.name}</h3>
                <p className="text-xs text-slate-400 line-clamp-3">
                  {dept.description || 'No description provided.'}
                </p>

                {dept.managerId && (
                  <div className="text-xs text-indigo-300 font-semibold pt-1">
                    Head: {getUserName(dept.managerId) || 'Assigned'}
                  </div>
                )}
              </div>

              <div className="pt-4 border-t border-slate-800/80 flex items-center justify-between text-xs">
                <span className="text-slate-500 font-mono text-[11px]">
                  ID: {dept.id.slice(0, 8)}...
                </span>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleOpenEdit(dept)}
                    className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold transition-colors inline-flex items-center gap-1 border border-slate-700"
                  >
                    <Edit2 className="w-3 h-3 text-indigo-400" />
                    <span>Edit</span>
                  </button>

                  <button
                    onClick={() => {
                      setArchiveError(null);
                      setDeptToArchive(dept);
                    }}
                    className="px-2.5 py-1.5 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 text-xs font-bold transition-colors inline-flex items-center gap-1 border border-rose-500/20"
                    title="Archive department"
                  >
                    <Trash2 className="w-3 h-3" />
                    <span>Archive</span>
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create Department Modal */}
      <Modal isOpen={createModalOpen} onClose={() => setCreateModalOpen(false)} title="Create Department">
        <form onSubmit={handleCreate} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Department Name *
            </label>
            <input
              type="text"
              required
              placeholder="e.g. Product Engineering"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Description
            </label>
            <textarea
              rows={3}
              placeholder="Responsibilities and domain..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Department Head / Manager
            </label>
            <select
              value={managerId}
              onChange={(e) => setManagerId(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            >
              <option value="">None</option>
              {users.map((u: any) => (
                <option key={u.id} value={u.id}>{u.fullName} ({u.employeeCode || 'EMP'})</option>
              ))}
            </select>
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
              {createMutation.isPending ? 'Creating...' : 'Create Department'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Edit Department Modal */}
      {editingDept && (
        <Modal isOpen={!!editingDept} onClose={() => setEditingDept(null)} title={`Edit Department — ${editingDept.name}`}>
          <form onSubmit={handleSaveEdit} className="space-y-4">
            {editError && (
              <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-semibold flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{editError}</span>
              </div>
            )}

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Department Name *
              </label>
              <input
                type="text"
                required
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Description
              </label>
              <textarea
                rows={3}
                value={editDescription}
                onChange={(e) => setEditDescription(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                  Department Head / Manager
                </label>
                <select
                  value={editManagerId}
                  onChange={(e) => setEditManagerId(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
                >
                  <option value="">None</option>
                  {users.map((u: any) => (
                    <option key={u.id} value={u.id}>{u.fullName} ({u.employeeCode || 'EMP'})</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                  Status
                </label>
                <select
                  value={editStatus}
                  onChange={(e) => setEditStatus(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
                >
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="INACTIVE">INACTIVE</option>
                </select>
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-3">
              <button
                type="button"
                onClick={() => setEditingDept(null)}
                className="px-4 py-2.5 rounded-xl bg-slate-800 text-slate-300 text-xs font-bold"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={updateMutation.isPending}
                className="px-5 py-2.5 rounded-xl bg-indigo-600 text-white text-xs font-bold shadow-lg shadow-indigo-600/25 flex items-center gap-2 disabled:opacity-50"
              >
                <Save className="w-3.5 h-3.5" />
                <span>{updateMutation.isPending ? 'Saving...' : 'Save Changes'}</span>
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Archive Department Modal */}
      {deptToArchive && (
        <Modal
          isOpen={!!deptToArchive}
          onClose={() => setDeptToArchive(null)}
          title="Archive Department"
        >
          <div className="space-y-4">
            {archiveError && (
              <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-semibold flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{archiveError}</span>
              </div>
            )}
            <p className="text-sm text-slate-300">
              Are you sure you want to archive department <strong className="text-white">{deptToArchive.name}</strong>?
            </p>
            <div className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800 text-xs text-slate-400 space-y-1.5">
              <div>• The department will be deactivated.</div>
              <div>• Existing employee assignments and project records remain safely linked and preserved.</div>
            </div>
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setDeptToArchive(null)}
                className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={deleteMutation.isPending}
                onClick={() => deleteMutation.mutate(deptToArchive.id)}
                className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs flex items-center gap-1.5 disabled:opacity-50"
              >
                {deleteMutation.isPending ? 'Archiving...' : 'Confirm Archive'}
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
