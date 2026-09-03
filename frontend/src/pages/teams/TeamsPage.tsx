import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { teamApi, departmentApi, userApi } from '../../api/services';
import { Modal } from '../../components/common/Modal';
import {
  Briefcase,
  Plus,
  Building,
  Users,
  CheckCircle,
  Edit2,
  Trash2,
  AlertCircle,
  Archive,
  Save,
  Shield,
  Layers,
} from 'lucide-react';

export const TeamsPage: React.FC = () => {
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [departmentId, setDepartmentId] = useState('');
  const [leadId, setLeadId] = useState('');

  // Tab filter: 'active' | 'all'
  const [filterTab, setFilterTab] = useState<'active' | 'all'>('active');

  // Edit State
  const [editingTeam, setEditingTeam] = useState<any | null>(null);
  const [editName, setEditName] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editDepartmentId, setEditDepartmentId] = useState('');
  const [editLeadId, setEditLeadId] = useState('');
  const [editStatus, setEditStatus] = useState('ACTIVE');
  const [editError, setEditError] = useState<string | null>(null);

  // Delete/Archive State
  const [teamToArchive, setTeamToArchive] = useState<any | null>(null);
  const [archiveError, setArchiveError] = useState<string | null>(null);
  const [teamToDelete, setTeamToDelete] = useState<any | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const queryClient = useQueryClient();

  const { data: teamsData, isLoading } = useQuery({
    queryKey: ['teams-data', filterTab],
    queryFn: () => (filterTab === 'all' ? teamApi.getAllWithInactive() : teamApi.getAll()),
  });

  const { data: deptsData } = useQuery({
    queryKey: ['departments-active'],
    queryFn: () => departmentApi.getAll(),
  });

  const { data: usersData } = useQuery({
    queryKey: ['users-active'],
    queryFn: () => userApi.getActive(),
  });

  const createMutation = useMutation({
    mutationFn: (payload: any) => teamApi.create(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teams-data'] });
      queryClient.invalidateQueries({ queryKey: ['teams-active'] });
      setCreateModalOpen(false);
      setName('');
      setDescription('');
      setDepartmentId('');
      setLeadId('');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: any }) => teamApi.update(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teams-data'] });
      queryClient.invalidateQueries({ queryKey: ['teams-active'] });
      setEditingTeam(null);
      setEditError(null);
    },
    onError: (err: any) => {
      setEditError(err.response?.data?.message || err.message || 'Failed to update team');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => teamApi.archive(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teams-data'] });
      queryClient.invalidateQueries({ queryKey: ['teams-active'] });
      queryClient.invalidateQueries({ queryKey: ['archive-summary'] });
      setTeamToArchive(null);
      setArchiveError(null);
    },
    onError: (err: any) => {
      setArchiveError(err.response?.data?.message || err.message || 'Failed to archive team');
    },
  });

  const permanentDeleteMutation = useMutation({
    mutationFn: (id: string) => teamApi.permanentDelete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teams-data'] });
      queryClient.invalidateQueries({ queryKey: ['teams-active'] });
      queryClient.invalidateQueries({ queryKey: ['archive-summary'] });
      setTeamToDelete(null);
      setDeleteError(null);
    },
    onError: (err: any) => {
      const serverMsg = err.response?.data?.message || err.response?.data?.error || err.message;
      setDeleteError(serverMsg || 'Failed to permanently delete team');
    },
  });

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    createMutation.mutate({
      name: name.trim(),
      description: description.trim(),
      departmentId: departmentId || null,
      leadId: leadId || null,
    });
  };

  const handleOpenEdit = (team: any) => {
    setEditingTeam(team);
    setEditName(team.name || '');
    setEditDescription(team.description || '');
    setEditDepartmentId(team.departmentId || '');
    setEditLeadId(team.leadId || '');
    setEditStatus(team.status || 'ACTIVE');
    setEditError(null);
  };

  const handleSaveEdit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingTeam) return;
    updateMutation.mutate({
      id: editingTeam.id,
      payload: {
        name: editName.trim(),
        description: editDescription.trim(),
        departmentId: editDepartmentId || null,
        leadId: editLeadId || null,
        status: editStatus,
      },
    });
  };

  const teams = teamsData?.data || [];
  const depts = deptsData?.data || [];
  const users = usersData?.data || [];

  const getDeptName = (deptId?: string) => {
    if (!deptId) return null;
    const d = depts.find((x: any) => x.id === deptId);
    return d ? d.name : null;
  };

  const getUserName = (id?: string) => {
    if (!id) return null;
    const u = users.find((x: any) => x.id === id);
    return u ? `${u.fullName} (${u.employeeCode || '—'})` : null;
  };

  const getMemberCount = (teamId: string) => {
    return users.filter((u: any) => u.teamId === teamId).length;
  };

  return (
    <div className="space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider border border-indigo-500/20 mb-2">
            <Briefcase className="w-3.5 h-3.5" />
            Functional Teams
          </div>
          <h1 className="text-3xl font-black font-display text-white">Teams</h1>
          <p className="text-slate-400 text-sm">
            Manage functional working squads, assign team leads, and organize team members.
          </p>
        </div>

        <button
          onClick={() => setCreateModalOpen(true)}
          className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs shadow-lg shadow-indigo-600/25 flex items-center gap-2 transition-all self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>New Team</span>
        </button>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-800 pb-3">
        <button
          onClick={() => setFilterTab('active')}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            filterTab === 'active'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/25'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          Active Squads
        </button>
        <button
          onClick={() => setFilterTab('all')}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            filterTab === 'all'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/25'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          All Squads (Including Archived)
        </button>
      </div>

      {isLoading ? (
        <div className="text-center py-20 text-slate-500 text-sm">Loading teams...</div>
      ) : teams.length === 0 ? (
        <div className="glass-panel p-12 text-center rounded-3xl">
          <Briefcase className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <h3 className="text-lg font-bold text-white font-display">No Teams Found</h3>
          <p className="text-sm text-slate-400 mt-1">Form squads and assign leads.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {teams.map((team: any) => {
            const memberCount = getMemberCount(team.id);
            const deptName = getDeptName(team.departmentId);
            const leadName = getUserName(team.leadId);

            return (
              <div
                key={team.id}
                className="glass-panel p-6 rounded-3xl border border-slate-800 space-y-4 flex flex-col justify-between"
              >
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="p-3 w-fit rounded-2xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                      <Briefcase className="w-6 h-6" />
                    </div>
                    <span
                      className={`text-xs px-2.5 py-0.5 rounded-full font-bold border ${
                        team.status === 'ACTIVE'
                          ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                          : 'bg-slate-800 text-slate-400 border-slate-700'
                      }`}
                    >
                      {team.status || 'ACTIVE'}
                    </span>
                  </div>

                  <div>
                    <h3 className="text-xl font-bold text-white font-display">{team.name}</h3>
                    <p className="text-xs text-slate-400 line-clamp-2 mt-1">
                      {team.description || 'No description provided.'}
                    </p>
                  </div>

                  <div className="space-y-1.5 pt-1 text-xs">
                    {deptName && (
                      <div className="flex items-center gap-1.5 text-slate-300">
                        <Building className="w-3.5 h-3.5 text-indigo-400" />
                        <span>Department: <strong className="text-white">{deptName}</strong></span>
                      </div>
                    )}
                    {leadName && (
                      <div className="flex items-center gap-1.5 text-slate-300">
                        <Shield className="w-3.5 h-3.5 text-purple-400" />
                        <span>Lead: <strong className="text-white">{leadName}</strong></span>
                      </div>
                    )}
                    <div className="flex items-center gap-1.5 text-slate-400">
                      <Users className="w-3.5 h-3.5 text-slate-500" />
                      <span>{memberCount} active member{memberCount === 1 ? '' : 's'} assigned</span>
                    </div>
                  </div>
                </div>

                <div className="pt-4 border-t border-slate-800/80 flex items-center justify-between text-xs">
                  <span className="text-slate-500 font-mono text-[11px]">
                    ID: {team.id.slice(0, 8)}...
                  </span>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => handleOpenEdit(team)}
                      className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold transition-colors inline-flex items-center gap-1 border border-slate-700"
                    >
                      <Edit2 className="w-3 h-3 text-indigo-400" />
                      <span>Edit</span>
                    </button>

                    {team.status === 'ACTIVE' && (
                      <>
                        <button
                          onClick={() => {
                            setArchiveError(null);
                            setTeamToArchive(team);
                          }}
                          className="px-2.5 py-1.5 rounded-lg bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 text-xs font-bold transition-colors inline-flex items-center gap-1 border border-amber-500/20"
                          title="Archive team"
                        >
                          <Archive className="w-3 h-3" />
                          <span>Archive</span>
                        </button>

                        <button
                          onClick={() => {
                            setDeleteError(null);
                            setTeamToDelete(team);
                          }}
                          className="px-2.5 py-1.5 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 text-xs font-bold transition-colors inline-flex items-center gap-1 border border-rose-500/20"
                          title="Delete Permanently"
                        >
                          <Trash2 className="w-3 h-3" />
                          <span>Delete</span>
                        </button>
                      </>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Create Team Modal */}
      <Modal isOpen={createModalOpen} onClose={() => setCreateModalOpen(false)} title="Create New Team">
        <form onSubmit={handleCreate} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Team Name *
            </label>
            <input
              type="text"
              required
              placeholder="e.g. Core Platform Squad"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Department
            </label>
            <select
              value={departmentId}
              onChange={(e) => setDepartmentId(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            >
              <option value="">None</option>
              {depts.map((d: any) => (
                <option key={d.id} value={d.id}>{d.name}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Team Lead
            </label>
            <select
              value={leadId}
              onChange={(e) => setLeadId(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            >
              <option value="">None</option>
              {users.map((u: any) => (
                <option key={u.id} value={u.id}>{u.fullName} ({u.employeeCode || '—'})</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Description
            </label>
            <textarea
              rows={3}
              placeholder="Team focus and deliverables..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
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
              {createMutation.isPending ? 'Creating...' : 'Create Team'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Edit Team Modal */}
      {editingTeam && (
        <Modal
          isOpen={!!editingTeam}
          onClose={() => setEditingTeam(null)}
          title={`Edit Team — ${editingTeam.name}`}
        >
          <form onSubmit={handleSaveEdit} className="space-y-4">
            {editError && (
              <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-semibold flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{editError}</span>
              </div>
            )}

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Team Name *
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
                Department
              </label>
              <select
                value={editDepartmentId}
                onChange={(e) => setEditDepartmentId(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              >
                <option value="">None</option>
                {depts.map((d: any) => (
                  <option key={d.id} value={d.id}>{d.name}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Team Lead
              </label>
              <select
                value={editLeadId}
                onChange={(e) => setEditLeadId(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              >
                <option value="">None</option>
                {users.map((u: any) => (
                  <option key={u.id} value={u.id}>{u.fullName} ({u.employeeCode || '—'})</option>
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
                <option value="INACTIVE">INACTIVE (Archived)</option>
              </select>
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

            <div className="flex justify-end gap-3 pt-3">
              <button
                type="button"
                onClick={() => setEditingTeam(null)}
                className="px-4 py-2.5 rounded-xl bg-slate-800 text-slate-300 text-xs font-bold"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={updateMutation.isPending}
                className="px-5 py-2.5 rounded-xl bg-indigo-600 text-white text-xs font-bold shadow-lg shadow-indigo-600/25 flex items-center gap-1.5 disabled:opacity-50"
              >
                <Save className="w-3.5 h-3.5" />
                <span>{updateMutation.isPending ? 'Saving...' : 'Save Changes'}</span>
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Archive Confirmation Modal */}
      {teamToArchive && (
        <Modal
          isOpen={!!teamToArchive}
          onClose={() => setTeamToArchive(null)}
          title={`Archive Team — ${teamToArchive.name}`}
        >
          <div className="space-y-4">
            {archiveError && (
              <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-semibold flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{archiveError}</span>
              </div>
            )}

            <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-300 text-xs space-y-2">
              <div className="font-bold flex items-center gap-1.5 text-amber-400">
                <Archive className="w-4 h-4" />
                <span>Safe Production Archive</span>
              </div>
              <p>
                Are you sure you want to archive <strong>{teamToArchive.name}</strong>?
                This team will be marked as <strong>INACTIVE</strong>.
              </p>
              <p className="text-amber-300/80">
                Historical project assignments, past tasks, and employee associations will be preserved intact to prevent orphaned records.
              </p>
              {getMemberCount(teamToArchive.id) > 0 && (
                <p className="font-semibold text-amber-200">
                  Note: There are currently {getMemberCount(teamToArchive.id)} active member(s) assigned to this squad.
                </p>
              )}
            </div>

            <div className="flex justify-end gap-3 pt-3">
              <button
                type="button"
                onClick={() => setTeamToArchive(null)}
                className="px-4 py-2.5 rounded-xl bg-slate-800 text-slate-300 text-xs font-bold"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={deleteMutation.isPending}
                onClick={() => deleteMutation.mutate(teamToArchive.id)}
                className="px-5 py-2.5 rounded-xl bg-amber-600 hover:bg-amber-500 text-white text-xs font-bold shadow-lg shadow-amber-600/25 flex items-center gap-1.5 disabled:opacity-50"
              >
                <Archive className="w-3.5 h-3.5" />
                <span>{deleteMutation.isPending ? 'Archiving...' : 'Confirm Archive'}</span>
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* Delete Permanently Modal */}
      {teamToDelete && (
        <Modal
          isOpen={!!teamToDelete}
          onClose={() => setTeamToDelete(null)}
          title={`Delete Permanently — ${teamToDelete.name}`}
        >
          <div className="space-y-4">
            {deleteError && (
              <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-semibold flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{deleteError}</span>
              </div>
            )}

            <div className="p-4 rounded-2xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs space-y-2">
              <div className="font-bold flex items-center gap-1.5 text-rose-400">
                <Trash2 className="w-4 h-4" />
                <span>Permanent Squad Deletion</span>
              </div>
              <p>
                Permanently deleting squad <strong>{teamToDelete.name}</strong> will remove it from the organization entirely.
                All user and project assignments will be safely unlinked.
              </p>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800 text-xs text-slate-400 space-y-1">
              <div>• Record will NOT appear in Archive.</div>
              <div>• Cannot be undone or recovered.</div>
            </div>

            <div className="flex justify-end gap-3 pt-3">
              <button
                type="button"
                onClick={() => setTeamToDelete(null)}
                className="px-4 py-2.5 rounded-xl bg-slate-800 text-slate-300 text-xs font-bold"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={permanentDeleteMutation.isPending}
                onClick={() => permanentDeleteMutation.mutate(teamToDelete.id)}
                className="px-5 py-2.5 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold shadow-lg shadow-rose-600/25 flex items-center gap-1.5 disabled:opacity-50"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>{permanentDeleteMutation.isPending ? 'Deleting...' : 'Delete Permanently'}</span>
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
