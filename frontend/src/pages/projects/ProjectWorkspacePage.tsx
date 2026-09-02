import React, { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { projectApi, taskApi, userApi, departmentApi, teamApi } from '../../api/services';
import { Modal } from '../../components/common/Modal';
import {
  FolderKanban,
  CheckSquare,
  Users,
  Flag,
  Activity,
  Plus,
  ArrowLeft,
  Clock,
  CheckCircle,
  AlertCircle,
  Calendar,
  Layers,
  Edit2,
  Trash2,
  Save,
} from 'lucide-react';

export const ProjectWorkspacePage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [activeTab, setActiveTab] = useState<'OVERVIEW' | 'TASKS' | 'TEAM' | 'MILESTONES'>('OVERVIEW');
  const [createTaskModal, setCreateTaskModal] = useState(false);
  const [createMilestoneModal, setCreateMilestoneModal] = useState(false);
  const [addMemberModal, setAddMemberModal] = useState(false);

  // Form states
  const [taskTitle, setTaskTitle] = useState('');
  const [taskDesc, setTaskDesc] = useState('');
  const [taskPriority, setTaskPriority] = useState('MEDIUM');
  const [taskDueDate, setTaskDueDate] = useState('');
  const [taskAssignee, setTaskAssignee] = useState('');

  const [milestoneName, setMilestoneName] = useState('');
  const [milestoneDate, setMilestoneDate] = useState('');

  const navigate = useNavigate();
  const [selectedMember, setSelectedMember] = useState('');

  // Project Edit & Archive State
  const [editProjectModalOpen, setEditProjectModalOpen] = useState(false);
  const [archiveProjectModalOpen, setArchiveProjectModalOpen] = useState(false);
  const [editName, setEditName] = useState('');
  const [editDesc, setEditDesc] = useState('');
  const [editPriority, setEditPriority] = useState('MEDIUM');
  const [editStatus, setEditStatus] = useState('PLANNING');
  const [editTargetDate, setEditTargetDate] = useState('');
  const [editManagerId, setEditManagerId] = useState('');
  const [editDeptId, setEditDeptId] = useState('');
  const [editTeamId, setEditTeamId] = useState('');
  const [editError, setEditError] = useState<string | null>(null);
  const [archiveError, setArchiveError] = useState<string | null>(null);

  const queryClient = useQueryClient();

  const { data: projectData } = useQuery({
    queryKey: ['project', id],
    queryFn: () => projectApi.getById(id!),
    enabled: !!id,
  });

  const { data: deptsData } = useQuery({
    queryKey: ['departments-active'],
    queryFn: () => departmentApi.getAll(),
  });

  const { data: teamsData } = useQuery({
    queryKey: ['teams-active'],
    queryFn: () => teamApi.getAll(),
  });

  const { data: tasksData } = useQuery({
    queryKey: ['project-tasks', id],
    queryFn: () => taskApi.getAll(id),
    enabled: !!id,
  });

  const { data: membersData } = useQuery({
    queryKey: ['project-members', id],
    queryFn: () => projectApi.getMembers(id!),
    enabled: !!id,
  });

  const { data: milestonesData } = useQuery({
    queryKey: ['project-milestones', id],
    queryFn: () => projectApi.getMilestones(id!),
    enabled: !!id,
  });

  const { data: allUsersData } = useQuery({
    queryKey: ['users-active'],
    queryFn: () => userApi.getActive(),
  });

  const updateProjectMutation = useMutation({
    mutationFn: (payload: any) => projectApi.update(id!, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', id] });
      queryClient.invalidateQueries({ queryKey: ['projects-list'] });
      setEditProjectModalOpen(false);
      setEditError(null);
    },
    onError: (err: any) => {
      setEditError(err.response?.data?.message || err.message || 'Failed to update project');
    },
  });

  const archiveProjectMutation = useMutation({
    mutationFn: () => projectApi.delete(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects-list'] });
      navigate('/projects');
    },
    onError: (err: any) => {
      setArchiveError(err.response?.data?.message || err.message || 'Failed to archive project');
    },
  });

  const handleOpenEditProject = () => {
    if (!projectData?.data) return;
    const p = projectData.data;
    setEditName(p.name || '');
    setEditDesc(p.description || '');
    setEditPriority(p.priority || 'MEDIUM');
    setEditStatus(p.status || 'PLANNING');
    setEditTargetDate(p.targetDate || '');
    setEditManagerId(p.managerId || '');
    setEditDeptId(p.departmentId || '');
    setEditTeamId(p.teamId || '');
    setEditError(null);
    setEditProjectModalOpen(true);
  };

  const handleSaveEditProject = (e: React.FormEvent) => {
    e.preventDefault();
    updateProjectMutation.mutate({
      name: editName.trim(),
      description: editDesc.trim(),
      priority: editPriority,
      status: editStatus,
      targetDate: editTargetDate || null,
      managerId: editManagerId || null,
      departmentId: editDeptId || null,
      teamId: editTeamId || null,
    });
  };

  const createTaskMutation = useMutation({
    mutationFn: (payload: any) => taskApi.create(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project-tasks', id] });
      queryClient.invalidateQueries({ queryKey: ['project', id] });
      setCreateTaskModal(false);
      setTaskTitle('');
      setTaskDesc('');
    },
  });

  const createMilestoneMutation = useMutation({
    mutationFn: (payload: any) => projectApi.createMilestone(id!, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project-milestones', id] });
      setCreateMilestoneModal(false);
      setMilestoneName('');
      setMilestoneDate('');
    },
  });

  const addMemberMutation = useMutation({
    mutationFn: (userId: string) => projectApi.addMember(id!, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project-members', id] });
      setAddMemberModal(false);
    },
  });

  const handleCreateTask = (e: React.FormEvent) => {
    e.preventDefault();
    createTaskMutation.mutate({
      projectId: id,
      title: taskTitle,
      description: taskDesc,
      priority: taskPriority,
      dueDate: taskDueDate || null,
      assigneeId: taskAssignee || null,
    });
  };

  const handleCreateMilestone = (e: React.FormEvent) => {
    e.preventDefault();
    createMilestoneMutation.mutate({
      name: milestoneName,
      targetDate: milestoneDate || null,
    });
  };

  const project = projectData?.data;
  const tasks = tasksData?.data?.content || [];
  const members = membersData?.data || [];
  const milestones = milestonesData?.data || [];
  const allUsers = allUsersData?.data || [];

  if (!project) {
    return <div className="text-center py-20 text-slate-500">Loading project workspace...</div>;
  }

  return (
    <div className="space-y-8">
      {/* Top Breadcrumb & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <Link
            to="/projects"
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-400 hover:text-indigo-400 mb-2 transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" /> Back to Projects
          </Link>
          <div className="flex items-center gap-3">
            <h1 className="text-3xl font-black font-display text-white">{project.name}</h1>
            <span
              className={`text-xs px-2.5 py-0.5 rounded-full font-bold border ${
                project.health === 'ON_TRACK'
                  ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                  : project.health === 'AT_RISK'
                  ? 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                  : 'bg-rose-500/10 text-rose-400 border-rose-500/20'
              }`}
            >
              {project.health.replace('_', ' ')}
            </span>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleOpenEditProject}
            className="px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold border border-slate-700 flex items-center gap-1.5 transition-colors"
          >
            <Edit2 className="w-3.5 h-3.5 text-indigo-400" />
            <span>Edit Project</span>
          </button>
          <button
            onClick={() => {
              setArchiveError(null);
              setArchiveProjectModalOpen(true);
            }}
            className="px-3 py-2 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 text-xs font-bold border border-rose-500/20 flex items-center gap-1.5 transition-colors"
            title="Archive project"
          >
            <Trash2 className="w-3.5 h-3.5" />
            <span>Archive</span>
          </button>
          <button
            onClick={() => setCreateTaskModal(true)}
            className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs shadow-lg shadow-indigo-600/25 flex items-center gap-1.5 transition-all"
          >
            <Plus className="w-4 h-4" />
            <span>New Task</span>
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-800 pb-3">
        {[
          { id: 'OVERVIEW', label: 'Overview', icon: FolderKanban },
          { id: 'TASKS', label: `Tasks (${tasks.length})`, icon: CheckSquare },
          { id: 'TEAM', label: `Team (${members.length})`, icon: Users },
          { id: 'MILESTONES', label: `Milestones (${milestones.length})`, icon: Flag },
        ].map((tab) => {
          const Icon = tab.icon;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              className={`px-4 py-2 rounded-xl text-xs font-bold transition-all flex items-center gap-2 ${
                activeTab === tab.id
                  ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/25'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
              }`}
            >
              <Icon className="w-4 h-4" />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* TAB CONTENT: OVERVIEW */}
      {activeTab === 'OVERVIEW' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-6">
            <div className="glass-panel p-6 rounded-3xl space-y-4">
              <h3 className="text-base font-bold text-white font-display">Project Description</h3>
              <p className="text-sm text-slate-300 leading-relaxed">
                {project.description || 'No description provided for this project.'}
              </p>

              {/* Progress Detail */}
              <div className="pt-4 border-t border-slate-800 space-y-2">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-slate-400 font-semibold">Calculated Task Progress</span>
                  <span className="text-indigo-400 font-black text-lg font-display">{project.progress}%</span>
                </div>
                <div className="h-3 w-full bg-slate-800 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-gradient-to-r from-indigo-500 to-violet-500 rounded-full transition-all duration-500"
                    style={{ width: `${project.progress}%` }}
                  />
                </div>
              </div>
            </div>

            {/* Recent Tasks preview */}
            <div className="glass-panel p-6 rounded-3xl space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-base font-bold text-white font-display">Recent Tasks</h3>
                <button onClick={() => setActiveTab('TASKS')} className="text-xs font-semibold text-indigo-400 hover:underline">
                  View all
                </button>
              </div>

              {tasks.length === 0 ? (
                <div className="text-center py-6 text-slate-500 text-sm">No tasks added yet</div>
              ) : (
                <div className="space-y-2">
                  {tasks.slice(0, 4).map((t) => (
                    <div key={t.id} className="p-3 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-between">
                      <span className="text-sm font-semibold text-slate-200">{t.title}</span>
                      <span className="text-xs px-2 py-0.5 rounded bg-slate-800 text-slate-400">{t.status}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Right Meta Column */}
          <div className="space-y-6">
            <div className="glass-panel p-6 rounded-3xl space-y-4">
              <h3 className="text-sm font-bold text-white uppercase tracking-wider">Project Details</h3>
              <div className="space-y-3 text-xs">
                <div className="flex justify-between py-1.5 border-b border-slate-800">
                  <span className="text-slate-500">Status</span>
                  <span className="font-semibold text-slate-300">{project.status}</span>
                </div>
                <div className="flex justify-between py-1.5 border-b border-slate-800">
                  <span className="text-slate-500">Priority</span>
                  <span className="font-semibold text-slate-300">{project.priority}</span>
                </div>
                <div className="flex justify-between py-1.5 border-b border-slate-800">
                  <span className="text-slate-500">Health</span>
                  <span className="font-semibold text-slate-300">{project.health}</span>
                </div>
                <div className="flex justify-between py-1.5 border-b border-slate-800">
                  <span className="text-slate-500">Target Date</span>
                  <span className="font-semibold text-slate-300">{project.targetDate || 'None'}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB CONTENT: TASKS */}
      {activeTab === 'TASKS' && (
        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <h3 className="text-lg font-bold font-display text-white">Project Tasks</h3>
            <button
              onClick={() => setCreateTaskModal(true)}
              className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs flex items-center gap-1.5"
            >
              <Plus className="w-4 h-4" /> Add Task
            </button>
          </div>

          {tasks.length === 0 ? (
            <div className="glass-panel p-12 text-center rounded-3xl">
              <CheckSquare className="w-12 h-12 text-slate-600 mx-auto mb-2" />
              <p className="text-sm text-slate-400">No tasks created yet for this project.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {tasks.map((task) => (
                <div
                  key={task.id}
                  className="glass-panel p-5 rounded-2xl border border-slate-800 flex items-center justify-between"
                >
                  <div className="space-y-1">
                    <div className="text-sm font-bold text-white">{task.title}</div>
                    <div className="text-xs text-slate-400">{task.description}</div>
                  </div>
                  <span className="text-xs px-2.5 py-1 rounded-lg bg-slate-800 text-indigo-400 font-semibold border border-slate-700">
                    {task.status}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* TAB CONTENT: TEAM */}
      {activeTab === 'TEAM' && (
        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <h3 className="text-lg font-bold font-display text-white">Assigned Team Members</h3>
            <button
              onClick={() => setAddMemberModal(true)}
              className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs flex items-center gap-1.5"
            >
              <Plus className="w-4 h-4" /> Add Member
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {members.map((m: any) => (
              <div key={m.id} className="glass-panel p-4 rounded-2xl flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-slate-800 flex items-center justify-center text-indigo-400 font-bold text-sm">
                  {m.role?.charAt(0) || 'M'}
                </div>
                <div>
                  <div className="text-sm font-bold text-white">Member</div>
                  <div className="text-xs text-indigo-400 font-mono">{m.role}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* TAB CONTENT: MILESTONES */}
      {activeTab === 'MILESTONES' && (
        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <h3 className="text-lg font-bold font-display text-white">Milestones</h3>
            <button
              onClick={() => setCreateMilestoneModal(true)}
              className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs flex items-center gap-1.5"
            >
              <Plus className="w-4 h-4" /> Add Milestone
            </button>
          </div>

          {milestones.length === 0 ? (
            <div className="glass-panel p-12 text-center rounded-3xl text-slate-400 text-sm">
              No milestones defined yet.
            </div>
          ) : (
            <div className="space-y-3">
              {milestones.map((m) => (
                <div key={m.id} className="glass-panel p-4 rounded-2xl flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Flag className="w-5 h-5 text-indigo-400" />
                    <div>
                      <div className="text-sm font-bold text-white">{m.name}</div>
                      {m.targetDate && <div className="text-xs text-slate-500">Target: {m.targetDate}</div>}
                    </div>
                  </div>
                  <span className={`text-xs px-2.5 py-1 rounded-lg font-semibold ${
                    m.status === 'COMPLETED' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-slate-800 text-slate-400'
                  }`}>
                    {m.status}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Create Task Modal */}
      <Modal isOpen={createTaskModal} onClose={() => setCreateTaskModal(false)} title="Create Task in Project">
        <form onSubmit={handleCreateTask} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Title *</label>
            <input
              type="text"
              required
              placeholder="e.g. Implement OAuth Flow"
              value={taskTitle}
              onChange={(e) => setTaskTitle(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Description</label>
            <textarea
              rows={3}
              placeholder="Task details and expectations..."
              value={taskDesc}
              onChange={(e) => setTaskDesc(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Priority</label>
              <select
                value={taskPriority}
                onChange={(e) => setTaskPriority(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Due Date</label>
              <input
                type="date"
                value={taskDueDate}
                onChange={(e) => setTaskDueDate(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Assignee</label>
            <select
              value={taskAssignee}
              onChange={(e) => setTaskAssignee(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            >
              <option value="">Unassigned</option>
              {allUsers.map((u: any) => (
                <option key={u.id} value={u.id}>{u.fullName} ({u.employeeCode})</option>
              ))}
            </select>
          </div>

          <div className="flex justify-end gap-3 pt-3">
            <button
              type="button"
              onClick={() => setCreateTaskModal(false)}
              className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 text-xs font-bold"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={createTaskMutation.isPending}
              className="px-5 py-2 rounded-xl bg-indigo-600 text-white text-xs font-bold"
            >
              Create Task
            </button>
          </div>
        </form>
      </Modal>

      {/* Add Member Modal */}
      <Modal isOpen={addMemberModal} onClose={() => setAddMemberModal(false)} title="Add Team Member to Project">
        <div className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Select Employee</label>
            <select
              value={selectedMember}
              onChange={(e) => setSelectedMember(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            >
              <option value="">Choose an employee...</option>
              {allUsers.map((u: any) => (
                <option key={u.id} value={u.id}>{u.fullName} ({u.employeeCode})</option>
              ))}
            </select>
          </div>

          <div className="flex justify-end gap-3 pt-3">
            <button
              onClick={() => setAddMemberModal(false)}
              className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 text-xs font-bold"
            >
              Cancel
            </button>
            <button
              onClick={() => selectedMember && addMemberMutation.mutate(selectedMember)}
              disabled={!selectedMember || addMemberMutation.isPending}
              className="px-5 py-2 rounded-xl bg-indigo-600 text-white text-xs font-bold disabled:opacity-50"
            >
              Add to Project
            </button>
          </div>
        </div>
      </Modal>

      {/* Create Milestone Modal */}
      <Modal isOpen={createMilestoneModal} onClose={() => setCreateMilestoneModal(false)} title="Create Project Milestone">
        <form onSubmit={handleCreateMilestone} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Milestone Name *</label>
            <input
              type="text"
              required
              placeholder="e.g. Beta Release 1.0"
              value={milestoneName}
              onChange={(e) => setMilestoneName(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Target Date</label>
            <input
              type="date"
              value={milestoneDate}
              onChange={(e) => setMilestoneDate(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div className="flex justify-end gap-3 pt-3">
            <button
              type="button"
              onClick={() => setCreateMilestoneModal(false)}
              className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 text-xs font-bold"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={createMilestoneMutation.isPending}
              className="px-5 py-2 rounded-xl bg-indigo-600 text-white text-xs font-bold"
            >
              Create Milestone
            </button>
          </div>
        </form>
      </Modal>

      {/* Edit Project Modal */}
      <Modal
        isOpen={editProjectModalOpen}
        onClose={() => setEditProjectModalOpen(false)}
        title={`Edit Project — ${project.name}`}
      >
        <form onSubmit={handleSaveEditProject} className="space-y-4">
          {editError && (
            <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-semibold flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{editError}</span>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Project Name *</label>
            <input
              type="text"
              required
              value={editName}
              onChange={(e) => setEditName(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Description</label>
            <textarea
              rows={3}
              value={editDesc}
              onChange={(e) => setEditDesc(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Priority</label>
              <select
                value={editPriority}
                onChange={(e) => setEditPriority(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              >
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
                <option value="URGENT">URGENT</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Status</label>
              <select
                value={editStatus}
                onChange={(e) => setEditStatus(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              >
                <option value="PLANNING">PLANNING</option>
                <option value="ACTIVE">ACTIVE</option>
                <option value="ON_HOLD">ON_HOLD</option>
                <option value="COMPLETED">COMPLETED</option>
                <option value="ARCHIVED">ARCHIVED</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Target Date</label>
              <input
                type="date"
                value={editTargetDate}
                onChange={(e) => setEditTargetDate(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Project Lead / Manager</label>
              <select
                value={editManagerId}
                onChange={(e) => setEditManagerId(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              >
                <option value="">None</option>
                {allUsers.map((u: any) => (
                  <option key={u.id} value={u.id}>{u.fullName} ({u.employeeCode || 'EMP'})</option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Department</label>
              <select
                value={editDeptId}
                onChange={(e) => setEditDeptId(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              >
                <option value="">None</option>
                {(deptsData?.data || []).map((d: any) => (
                  <option key={d.id} value={d.id}>{d.name}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Team</label>
              <select
                value={editTeamId}
                onChange={(e) => setEditTeamId(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              >
                <option value="">None</option>
                {(teamsData?.data || []).map((t: any) => (
                  <option key={t.id} value={t.id}>{t.name}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-3">
            <button
              type="button"
              onClick={() => setEditProjectModalOpen(false)}
              className="px-4 py-2.5 rounded-xl bg-slate-800 text-slate-300 text-xs font-bold"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={updateProjectMutation.isPending}
              className="px-5 py-2.5 rounded-xl bg-indigo-600 text-white text-xs font-bold shadow-lg shadow-indigo-600/25 flex items-center gap-2 disabled:opacity-50"
            >
              <Save className="w-3.5 h-3.5" />
              <span>{updateProjectMutation.isPending ? 'Saving...' : 'Save Changes'}</span>
            </button>
          </div>
        </form>
      </Modal>

      {/* Archive Project Modal */}
      <Modal
        isOpen={archiveProjectModalOpen}
        onClose={() => setArchiveProjectModalOpen(false)}
        title="Archive Project"
      >
        <div className="space-y-4">
          {archiveError && (
            <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-semibold flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{archiveError}</span>
            </div>
          )}
          <p className="text-sm text-slate-300">
            Are you sure you want to archive project <strong className="text-white">{project.name}</strong>?
          </p>
          <div className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800 text-xs text-slate-400 space-y-1.5">
            <div>• The project status will be marked as ARCHIVED.</div>
            <div>• All historical tasks, milestone completions, work activities, and code reviews will remain preserved.</div>
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={() => setArchiveProjectModalOpen(false)}
              className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs"
            >
              Cancel
            </button>
            <button
              type="button"
              disabled={archiveProjectMutation.isPending}
              onClick={() => archiveProjectMutation.mutate()}
              className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs flex items-center gap-1.5 disabled:opacity-50"
            >
              {archiveProjectMutation.isPending ? 'Archiving...' : 'Confirm Archive'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
