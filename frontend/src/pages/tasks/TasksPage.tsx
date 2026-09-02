import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { taskApi, projectApi, userApi } from '../../api/services';
import { Task } from '../../types';
import { KanbanBoard } from './KanbanBoard';
import { Modal } from '../../components/common/Modal';
import {
  CheckSquare,
  Columns,
  List,
  Plus,
  Clock,
  MessageSquare,
  CheckCircle2,
  AlertCircle,
  Play,
  RotateCcw,
  Send,
  User,
  ExternalLink,
  GitBranch,
  GitCommit,
  GitPullRequest,
  Code2,
  Edit2,
  Trash2,
  Save,
} from 'lucide-react';

export const TasksPage: React.FC = () => {
  const [viewMode, setViewMode] = useState<'LIST' | 'KANBAN'>('KANBAN');
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [activeTask, setActiveTask] = useState<Task | null>(null);

  // Task Edit & Archive State
  const [isEditingTask, setIsEditingTask] = useState(false);
  const [editTaskTitle, setEditTaskTitle] = useState('');
  const [editTaskDesc, setEditTaskDesc] = useState('');
  const [editTaskPriority, setEditTaskPriority] = useState('MEDIUM');
  const [editTaskStatus, setEditTaskStatus] = useState('TODO');
  const [editTaskAssigneeId, setEditTaskAssigneeId] = useState('');
  const [editTaskProjectId, setEditTaskProjectId] = useState('');
  const [editTaskDueDate, setEditTaskDueDate] = useState('');
  const [editTaskEstimatedHours, setEditTaskEstimatedHours] = useState('');
  const [editTaskError, setEditTaskError] = useState<string | null>(null);
  const [archiveTaskModalOpen, setArchiveTaskModalOpen] = useState(false);
  const [archiveTaskError, setArchiveTaskError] = useState<string | null>(null);

  // Review submission modal state
  const [submitReviewModalOpen, setSubmitReviewModalOpen] = useState(false);
  const [reviewTaskId, setReviewTaskId] = useState<string | null>(null);
  const [reviewTaskTitle, setReviewTaskTitle] = useState<string>('');
  const [workSummary, setWorkSummary] = useState('');
  const [repoUrl, setRepoUrl] = useState('');
  const [branchName, setBranchName] = useState('');
  const [prUrl, setPrUrl] = useState('');
  const [commitSha, setCommitSha] = useState('');

  // Form states
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [projectId, setProjectId] = useState('');
  const [assigneeId, setAssigneeId] = useState('');
  const [priority, setPriority] = useState('MEDIUM');
  const [dueDate, setDueDate] = useState('');
  const [estimatedHours, setEstimatedHours] = useState('');
  const [subtasksText, setSubtasksText] = useState('');

  // Comment state
  const [commentText, setCommentText] = useState('');

  const queryClient = useQueryClient();

  const { data: tasksData, isLoading } = useQuery({
    queryKey: ['tasks-all'],
    queryFn: () => taskApi.getAll(),
  });

  const { data: projectsData } = useQuery({
    queryKey: ['projects-list'],
    queryFn: () => projectApi.getAll(),
  });

  const { data: usersData } = useQuery({
    queryKey: ['users-active'],
    queryFn: () => userApi.getActive(),
  });

  // Task Details Queries
  const { data: subtasksData } = useQuery({
    queryKey: ['task-subtasks', activeTask?.id],
    queryFn: () => taskApi.getSubtasks(activeTask!.id),
    enabled: !!activeTask?.id && detailModalOpen,
  });

  const { data: commentsData } = useQuery({
    queryKey: ['task-comments', activeTask?.id],
    queryFn: () => taskApi.getComments(activeTask!.id),
    enabled: !!activeTask?.id && detailModalOpen,
  });

  const { data: submissionsData } = useQuery({
    queryKey: ['task-submissions', activeTask?.id],
    queryFn: () => taskApi.getSubmissions(activeTask!.id),
    enabled: !!activeTask?.id && detailModalOpen,
  });

  // Mutations
  const createTaskMutation = useMutation({
    mutationFn: (payload: any) => taskApi.create(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks-all'] });
      queryClient.invalidateQueries({ queryKey: ['projects-list'] });
      setCreateModalOpen(false);
      setTitle('');
      setDescription('');
      setSubtasksText('');
    },
  });

  const updateStatusMutation = useMutation({
    mutationFn: (payload: { id: string; status: string }) =>
      taskApi.updateStatus(payload.id, { status: payload.status }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks-all'] });
      queryClient.invalidateQueries({ queryKey: ['projects-list'] });
    },
  });

  const submitReviewMutation = useMutation({
    mutationFn: (payload: { id: string; workSummary: string; repositoryUrl: string; branch?: string; pullRequestUrl?: string; commitSha?: string }) =>
      taskApi.submitReview(payload.id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks-all'] });
      queryClient.invalidateQueries({ queryKey: ['task-submissions', activeTask?.id] });
      setSubmitReviewModalOpen(false);
      setWorkSummary('');
      setRepoUrl('');
      setBranchName('');
      setPrUrl('');
      setCommitSha('');
    },
  });

  const toggleSubtaskMutation = useMutation({
    mutationFn: (subtaskId: string) => taskApi.toggleSubtask(subtaskId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['task-subtasks', activeTask?.id] });
    },
  });

  const addCommentMutation = useMutation({
    mutationFn: (payload: { id: string; content: string }) =>
      taskApi.addComment(payload.id, { content: payload.content }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['task-comments', activeTask?.id] });
      setCommentText('');
    },
  });

  const updateTaskMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: any }) => taskApi.update(id, payload),
    onSuccess: (res: any) => {
      queryClient.invalidateQueries({ queryKey: ['tasks-all'] });
      queryClient.invalidateQueries({ queryKey: ['projects-list'] });
      setActiveTask(res.data);
      setIsEditingTask(false);
      setEditTaskError(null);
    },
    onError: (err: any) => {
      setEditTaskError(err.response?.data?.message || err.message || 'Failed to update task');
    },
  });

  const archiveTaskMutation = useMutation({
    mutationFn: (id: string) => taskApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks-all'] });
      queryClient.invalidateQueries({ queryKey: ['projects-list'] });
      setArchiveTaskModalOpen(false);
      setDetailModalOpen(false);
      setActiveTask(null);
    },
    onError: (err: any) => {
      setArchiveTaskError(err.response?.data?.message || err.message || 'Failed to archive task');
    },
  });

  const handleOpenEditTask = (task: Task) => {
    setEditTaskTitle(task.title || '');
    setEditTaskDesc(task.description || '');
    setEditTaskPriority(task.priority || 'MEDIUM');
    setEditTaskStatus(task.status || 'TODO');
    setEditTaskAssigneeId(task.assigneeId || '');
    setEditTaskProjectId(task.projectId || '');
    setEditTaskDueDate(task.dueDate || '');
    setEditTaskEstimatedHours(task.estimatedHours != null ? String(task.estimatedHours) : '');
    setEditTaskError(null);
    setIsEditingTask(true);
  };

  const handleSaveEditTask = (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeTask) return;
    updateTaskMutation.mutate({
      id: activeTask.id,
      payload: {
        title: editTaskTitle.trim(),
        description: editTaskDesc.trim(),
        priority: editTaskPriority,
        status: editTaskStatus,
        assigneeId: editTaskAssigneeId || null,
        projectId: editTaskProjectId || null,
        dueDate: editTaskDueDate || null,
        estimatedHours: editTaskEstimatedHours ? parseFloat(editTaskEstimatedHours) : null,
      },
    });
  };

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    const subtasks = subtasksText
      .split('\n')
      .map((s) => s.trim())
      .filter(Boolean);

    createTaskMutation.mutate({
      title,
      description,
      projectId: projectId || null,
      assigneeId: assigneeId || null,
      priority,
      dueDate: dueDate || null,
      estimatedHours: estimatedHours ? parseFloat(estimatedHours) : null,
      subtasks,
    });
  };

  const tasks = tasksData?.data?.content || [];
  const projects = projectsData?.data?.content || [];
  const users = usersData?.data || [];
  const subtasks = subtasksData?.data || [];
  const comments = commentsData?.data?.content || [];

  return (
    <div className="space-y-8">
      {/* Header with View Toggle & Create */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider border border-indigo-500/20 mb-2">
            <CheckSquare className="w-3.5 h-3.5" />
            Task Management
          </div>
          <h1 className="text-3xl font-black font-display text-white">Tasks & Workflows</h1>
          <p className="text-slate-400 text-sm">
            Track checklists, review submissions, and drag statuses across the Kanban board.
          </p>
        </div>

        <div className="flex items-center gap-3">
          {/* View Toggle */}
          <div className="p-1 rounded-xl bg-slate-900 border border-slate-800 flex items-center gap-1">
            <button
              onClick={() => setViewMode('KANBAN')}
              className={`p-2 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5 ${
                viewMode === 'KANBAN' ? 'bg-indigo-600 text-white' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Columns className="w-4 h-4" />
              <span>Kanban</span>
            </button>
            <button
              onClick={() => setViewMode('LIST')}
              className={`p-2 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5 ${
                viewMode === 'LIST' ? 'bg-indigo-600 text-white' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <List className="w-4 h-4" />
              <span>List</span>
            </button>
          </div>

          <button
            onClick={() => setCreateModalOpen(true)}
            className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs shadow-lg shadow-indigo-600/25 flex items-center gap-2 transition-all"
          >
            <Plus className="w-4 h-4" />
            <span>New Task</span>
          </button>
        </div>
      </div>

      {/* Main Content Area: Kanban or List View */}
      {isLoading ? (
        <div className="text-center py-20 text-slate-500 text-sm">Loading tasks...</div>
      ) : viewMode === 'KANBAN' ? (
        <KanbanBoard
          tasks={tasks}
          onTaskClick={(task) => {
            setActiveTask(task);
            setDetailModalOpen(true);
          }}
          onStatusChange={(taskId, newStatus) =>
            updateStatusMutation.mutate({ id: taskId, status: newStatus })
          }
        />
      ) : (
        /* List View */
        <div className="glass-panel rounded-3xl overflow-hidden border border-slate-800">
          <table className="w-full text-left text-sm text-slate-300">
            <thead className="bg-slate-900/80 text-xs font-bold uppercase tracking-wider text-slate-400 border-b border-slate-800">
              <tr>
                <th className="px-6 py-4">Title</th>
                <th className="px-6 py-4">Priority</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4">Due Date</th>
                <th className="px-6 py-4">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {tasks.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-6 py-10 text-center text-slate-500">
                    No tasks found. Create one above!
                  </td>
                </tr>
              ) : (
                tasks.map((task) => (
                  <tr
                    key={task.id}
                    onClick={() => {
                      setActiveTask(task);
                      setDetailModalOpen(true);
                    }}
                    className="hover:bg-slate-800/40 cursor-pointer transition-colors"
                  >
                    <td className="px-6 py-4 font-semibold text-white">{task.title}</td>
                    <td className="px-6 py-4">
                      <span className="text-xs font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-300">
                        {task.priority}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <span
                        className={`text-xs px-2.5 py-1 rounded-lg font-bold border ${
                          task.status === 'COMPLETED'
                            ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                            : task.status === 'REVIEW'
                            ? 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20'
                            : task.status === 'IN_PROGRESS'
                            ? 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                            : 'bg-slate-800 text-slate-400 border-slate-700'
                        }`}
                      >
                        {task.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-slate-400 text-xs">
                      {task.dueDate || 'No date'}
                    </td>
                    <td className="px-6 py-4 text-xs">
                      {task.status === 'TODO' && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            updateStatusMutation.mutate({ id: task.id, status: 'IN_PROGRESS' });
                          }}
                          className="px-3 py-1.5 rounded-lg bg-indigo-600/20 text-indigo-300 hover:bg-indigo-600/40 font-bold"
                        >
                          Start
                        </button>
                      )}
                      {task.status === 'IN_PROGRESS' && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            setReviewTaskId(task.id);
                            setReviewTaskTitle(task.title);
                            setSubmitReviewModalOpen(true);
                          }}
                          className="px-3 py-1.5 rounded-lg bg-amber-500/20 text-amber-300 hover:bg-amber-500/40 font-bold flex items-center gap-1"
                        >
                          <Code2 className="w-3.5 h-3.5" />
                          <span>Submit Review</span>
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Task Detail & Comments Modal */}
      <Modal
        isOpen={detailModalOpen}
        onClose={() => {
          setDetailModalOpen(false);
          setIsEditingTask(false);
        }}
        title={activeTask?.title || 'Task Details'}
        maxWidth="max-w-3xl"
      >
        {activeTask && (
          <div className="space-y-6">
            {/* Header Action Bar */}
            <div className="flex items-center justify-between gap-4 p-4 rounded-2xl bg-slate-900 border border-slate-800">
              <div className="flex items-center gap-2">
                <span className={`text-xs px-2.5 py-1 rounded-lg font-bold border ${
                  activeTask.status === 'COMPLETED'
                    ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                    : activeTask.status === 'REVIEW'
                    ? 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20'
                    : activeTask.status === 'IN_PROGRESS'
                    ? 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                    : 'bg-slate-800 text-slate-400 border-slate-700'
                }`}>
                  {activeTask.status}
                </span>
                <span className="text-xs font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-300">
                  {activeTask.priority}
                </span>
                {activeTask.dueDate && (
                  <span className="text-xs text-slate-400 flex items-center gap-1">
                    <Clock className="w-3 h-3" /> Due {activeTask.dueDate}
                  </span>
                )}
              </div>

              <div className="flex items-center gap-2">
                {!isEditingTask ? (
                  <button
                    onClick={() => handleOpenEditTask(activeTask)}
                    className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold border border-slate-700 flex items-center gap-1.5 transition-colors"
                  >
                    <Edit2 className="w-3.5 h-3.5 text-indigo-400" />
                    <span>Edit Task</span>
                  </button>
                ) : (
                  <button
                    onClick={() => setIsEditingTask(false)}
                    className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-bold border border-slate-700 transition-colors"
                  >
                    Cancel Edit
                  </button>
                )}

                <button
                  onClick={() => {
                    setArchiveTaskError(null);
                    setArchiveTaskModalOpen(true);
                  }}
                  className="px-3 py-1.5 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 text-xs font-bold border border-rose-500/20 flex items-center gap-1.5 transition-colors"
                  title="Archive task"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  <span>Archive</span>
                </button>
              </div>
            </div>

            {/* Edit Mode Form */}
            {isEditingTask ? (
              <form onSubmit={handleSaveEditTask} className="space-y-4 p-4 rounded-2xl bg-slate-900/90 border border-slate-700">
                {editTaskError && (
                  <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-semibold flex items-center gap-2">
                    <AlertCircle className="w-4 h-4 shrink-0" />
                    <span>{editTaskError}</span>
                  </div>
                )}

                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Title *</label>
                  <input
                    type="text"
                    required
                    value={editTaskTitle}
                    onChange={(e) => setEditTaskTitle(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Description</label>
                  <textarea
                    rows={3}
                    value={editTaskDesc}
                    onChange={(e) => setEditTaskDesc(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-white text-sm"
                  />
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Status</label>
                    <select
                      value={editTaskStatus}
                      onChange={(e) => setEditTaskStatus(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs"
                    >
                      <option value="TODO">TODO</option>
                      <option value="IN_PROGRESS">IN_PROGRESS</option>
                      <option value="REVIEW">REVIEW</option>
                      <option value="COMPLETED">COMPLETED</option>
                      <option value="BLOCKED">BLOCKED</option>
                      <option value="ARCHIVED">ARCHIVED</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Priority</label>
                    <select
                      value={editTaskPriority}
                      onChange={(e) => setEditTaskPriority(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs"
                    >
                      <option value="LOW">LOW</option>
                      <option value="MEDIUM">MEDIUM</option>
                      <option value="HIGH">HIGH</option>
                      <option value="URGENT">URGENT</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Due Date</label>
                    <input
                      type="date"
                      value={editTaskDueDate}
                      onChange={(e) => setEditTaskDueDate(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Est. Hours</label>
                    <input
                      type="number"
                      step="0.5"
                      value={editTaskEstimatedHours}
                      onChange={(e) => setEditTaskEstimatedHours(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Assignee</label>
                    <select
                      value={editTaskAssigneeId}
                      onChange={(e) => setEditTaskAssigneeId(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs"
                    >
                      <option value="">Unassigned</option>
                      {users.map((u: any) => (
                        <option key={u.id} value={u.id}>{u.fullName} ({u.employeeCode || 'EMP'})</option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Project</label>
                    <select
                      value={editTaskProjectId}
                      onChange={(e) => setEditTaskProjectId(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-white text-xs"
                    >
                      <option value="">No Project</option>
                      {projects.map((p: any) => (
                        <option key={p.id} value={p.id}>{p.name}</option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="flex justify-end gap-3 pt-2">
                  <button
                    type="button"
                    onClick={() => setIsEditingTask(false)}
                    className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 text-xs font-bold"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={updateTaskMutation.isPending}
                    className="px-5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold shadow-lg shadow-indigo-600/25 flex items-center gap-1.5 disabled:opacity-50"
                  >
                    <Save className="w-3.5 h-3.5" />
                    <span>{updateTaskMutation.isPending ? 'Saving...' : 'Save Changes'}</span>
                  </button>
                </div>
              </form>
            ) : (
              <div className="space-y-2">
                <span className="text-xs text-slate-400 uppercase font-semibold">Description</span>
                <p className="text-sm text-slate-200">{activeTask.description || 'No description provided.'}</p>
              </div>
            )}

            {/* Codebase / Repository Submissions Section */}
            <div className="space-y-3 pt-4 border-t border-slate-800">
              <div className="flex items-center justify-between">
                <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-2">
                  <Code2 className="w-3.5 h-3.5 text-indigo-400" /> Codebase & Review Submissions ({(submissionsData?.data || []).length})
                </h4>
                {activeTask.status === 'IN_PROGRESS' && (
                  <button
                    onClick={() => {
                      setReviewTaskId(activeTask.id);
                      setReviewTaskTitle(activeTask.title);
                      setSubmitReviewModalOpen(true);
                    }}
                    className="px-3 py-1 rounded-lg bg-amber-500/20 hover:bg-amber-500/40 text-amber-300 text-xs font-bold flex items-center gap-1"
                  >
                    <Plus className="w-3 h-3" /> Submit Work
                  </button>
                )}
              </div>

              {(submissionsData?.data || []).length === 0 ? (
                <div className="text-xs text-slate-500 p-3 rounded-xl bg-slate-900/60 border border-slate-800">
                  No repository or codebase submissions recorded yet. When you submit work for review, your repository URL and branch will appear here.
                </div>
              ) : (
                <div className="space-y-2.5 max-h-56 overflow-y-auto">
                  {(submissionsData?.data || []).map((sub: any) => (
                    <div key={sub.id} className="p-3.5 rounded-2xl bg-slate-900 border border-slate-800 space-y-2">
                      <div className="flex items-center justify-between gap-2 flex-wrap">
                        <div className="flex items-center gap-2">
                          <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                            v{sub.version || 1} • {sub.provider || 'GIT'}
                          </span>
                          <span className="text-xs font-mono text-slate-200 truncate max-w-sm">
                            {sub.repositoryUrl}
                          </span>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider ${
                            sub.reviewStatus === 'APPROVED'
                              ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                              : sub.reviewStatus === 'CHANGES_REQUESTED'
                              ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                              : 'bg-indigo-500/10 text-indigo-400 border border-indigo-500/20'
                          }`}>
                            {sub.reviewStatus}
                          </span>
                          <a
                            href={sub.repositoryUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="px-2.5 py-1 rounded-lg bg-indigo-600/20 hover:bg-indigo-600/40 text-indigo-300 text-xs font-bold inline-flex items-center gap-1 transition-colors"
                          >
                            <span>Open Repository</span>
                            <ExternalLink className="w-3 h-3" />
                          </a>
                        </div>
                      </div>

                      <div className="flex items-center gap-3 text-xs text-slate-400 flex-wrap">
                        {sub.branch && (
                          <span className="flex items-center gap-1 bg-slate-800 px-2 py-0.5 rounded font-mono text-slate-300">
                            <GitBranch className="w-3 h-3 text-amber-400" /> {sub.branch}
                          </span>
                        )}
                        {sub.commitSha && (
                          <span className="flex items-center gap-1 bg-slate-800 px-2 py-0.5 rounded font-mono text-slate-300">
                            <GitCommit className="w-3 h-3 text-cyan-400" /> {sub.commitSha.substring(0, 7)}
                          </span>
                        )}
                        {sub.pullRequestUrl && (
                          <a
                            href={sub.pullRequestUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="flex items-center gap-1 text-emerald-400 hover:underline font-mono"
                          >
                            <GitPullRequest className="w-3 h-3" /> PR ↗
                          </a>
                        )}
                        <span className="text-[10px] text-slate-500 ml-auto">
                          Submitted: {new Date(sub.submittedAt).toLocaleString()}
                        </span>
                      </div>

                      {sub.workSummary && (
                        <div className="text-xs text-slate-300 pt-1 border-t border-slate-800/60">
                          <strong className="text-slate-400 font-medium">Summary:</strong> {sub.workSummary}
                        </div>
                      )}
                      {sub.reviewComment && (
                        <div className="text-xs text-amber-300/90 bg-amber-500/10 p-2 rounded-xl border border-amber-500/20">
                          <strong>Feedback:</strong> {sub.reviewComment}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Subtasks / Checklist */}
            <div className="space-y-3 pt-4 border-t border-slate-800">
              <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                Checklist / Subtasks ({subtasks.filter((s) => s.completed).length}/{subtasks.length})
              </h4>
              {subtasks.length === 0 ? (
                <div className="text-xs text-slate-500">No subtasks on this task.</div>
              ) : (
                <div className="space-y-2">
                  {subtasks.map((sub) => (
                    <label
                      key={sub.id}
                      className="flex items-center gap-3 p-2.5 rounded-xl bg-slate-900/80 border border-slate-800 cursor-pointer hover:border-slate-700"
                    >
                      <input
                        type="checkbox"
                        checked={sub.completed}
                        onChange={() => toggleSubtaskMutation.mutate(sub.id)}
                        className="rounded bg-slate-800 border-slate-700 text-indigo-600 focus:ring-0"
                      />
                      <span className={`text-xs font-medium ${sub.completed ? 'line-through text-slate-500' : 'text-slate-200'}`}>
                        {sub.title}
                      </span>
                    </label>
                  ))}
                </div>
              )}
            </div>

            {/* Comments Thread */}
            <div className="space-y-4 pt-4 border-t border-slate-800">
              <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-2">
                <MessageSquare className="w-3.5 h-3.5" /> Comments ({comments.length})
              </h4>

              <div className="space-y-2 max-h-48 overflow-y-auto">
                {comments.map((c) => (
                  <div key={c.id} className="p-3 rounded-xl bg-slate-900 border border-slate-800 text-xs">
                    <div className="text-slate-200">{c.content}</div>
                    <div className="text-[10px] text-slate-500 mt-1">{new Date(c.createdAt).toLocaleString()}</div>
                  </div>
                ))}
              </div>

              <div className="flex gap-2">
                <input
                  type="text"
                  placeholder="Add a comment or review note..."
                  value={commentText}
                  onChange={(e) => setCommentText(e.target.value)}
                  className="flex-1 px-4 py-2.5 rounded-xl bg-slate-900 border border-slate-700 text-white text-xs"
                />
                <button
                  onClick={() =>
                    commentText.trim() &&
                    addCommentMutation.mutate({ id: activeTask.id, content: commentText })
                  }
                  disabled={!commentText.trim() || addCommentMutation.isPending}
                  className="px-4 py-2.5 rounded-xl bg-indigo-600 text-white text-xs font-bold flex items-center gap-1"
                >
                  <Send className="w-3.5 h-3.5" /> Send
                </button>
              </div>
            </div>
          </div>
        )}
      </Modal>

      {/* Submit Work for Review Modal */}
      <Modal
        isOpen={submitReviewModalOpen}
        onClose={() => setSubmitReviewModalOpen(false)}
        title="Submit Work for Review"
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (!reviewTaskId) return;
            submitReviewMutation.mutate({
              id: reviewTaskId,
              workSummary,
              repositoryUrl: repoUrl,
              branch: branchName || undefined,
              pullRequestUrl: prUrl || undefined,
              commitSha: commitSha || undefined,
            });
          }}
          className="space-y-4"
        >
          <div>
            <span className="text-xs text-slate-400 block mb-1">Task</span>
            <div className="text-sm font-bold text-white bg-slate-900 p-2.5 rounded-xl border border-slate-800">
              {reviewTaskTitle || 'Selected Task'}
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Repository / Codebase HTTPS URL *
            </label>
            <input
              type="url"
              required
              placeholder="https://github.com/company/project-repo"
              value={repoUrl}
              onChange={(e) => setRepoUrl(e.target.value)}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm font-mono placeholder-slate-500"
            />
            <span className="text-[10px] text-slate-500 mt-1 block">
              The external codebase remains in your authorized Git repository.
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Branch (Optional)
              </label>
              <input
                type="text"
                placeholder="e.g. main, feature/auth"
                value={branchName}
                onChange={(e) => setBranchName(e.target.value)}
                className="w-full px-4 py-2.5 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm font-mono placeholder-slate-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Commit SHA (Optional)
              </label>
              <input
                type="text"
                placeholder="e.g. 7f9b8c2..."
                value={commitSha}
                onChange={(e) => setCommitSha(e.target.value)}
                className="w-full px-4 py-2.5 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm font-mono placeholder-slate-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Pull Request URL (Optional)
            </label>
            <input
              type="url"
              placeholder="https://github.com/company/project-repo/pull/42"
              value={prUrl}
              onChange={(e) => setPrUrl(e.target.value)}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm font-mono placeholder-slate-500"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Work Summary & Implementation Notes *
            </label>
            <textarea
              rows={3}
              required
              placeholder="Summarize what was built, key architectural changes, test instructions..."
              value={workSummary}
              onChange={(e) => setWorkSummary(e.target.value)}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm placeholder-slate-500"
            />
          </div>

          <div className="flex justify-end gap-3 pt-3 border-t border-slate-800">
            <button
              type="button"
              onClick={() => setSubmitReviewModalOpen(false)}
              className="px-4 py-2.5 rounded-xl bg-slate-800 text-slate-300 text-xs font-bold hover:bg-slate-700"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={!repoUrl.trim() || !workSummary.trim() || submitReviewMutation.isPending}
              className="px-5 py-2.5 rounded-xl bg-amber-600 hover:bg-amber-500 text-white text-xs font-bold shadow-lg shadow-amber-600/25 disabled:opacity-50 flex items-center gap-1.5"
            >
              <Send className="w-3.5 h-3.5" />
              <span>{submitReviewMutation.isPending ? 'Submitting...' : 'Submit for Review'}</span>
            </button>
          </div>
        </form>
      </Modal>

      {/* Create Task Modal */}
      <Modal isOpen={createModalOpen} onClose={() => setCreateModalOpen(false)} title="Create New Task">
        <form onSubmit={handleCreate} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Task Title *
            </label>
            <input
              type="text"
              required
              placeholder="e.g. Implement User Authentication"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Description
            </label>
            <textarea
              rows={3}
              placeholder="Provide context and requirements..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Project (Optional)
              </label>
              <select
                value={projectId}
                onChange={(e) => setProjectId(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              >
                <option value="">No Project</option>
                {projects.map((p) => (
                  <option key={p.id} value={p.id}>{p.name}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Assignee
              </label>
              <select
                value={assigneeId}
                onChange={(e) => setAssigneeId(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              >
                <option value="">Unassigned</option>
                {users.map((u: any) => (
                  <option key={u.id} value={u.id}>{u.fullName} ({u.employeeCode})</option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Priority
              </label>
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Due Date
              </label>
              <input
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Est. Hours
              </label>
              <input
                type="number"
                step="0.5"
                placeholder="4.0"
                value={estimatedHours}
                onChange={(e) => setEstimatedHours(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Subtasks / Checklist (1 per line)
            </label>
            <textarea
              rows={3}
              placeholder="e.g.&#10;Design API endpoints&#10;Write unit tests&#10;Frontend integration"
              value={subtasksText}
              onChange={(e) => setSubtasksText(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm font-mono"
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
              disabled={createTaskMutation.isPending}
              className="px-5 py-2.5 rounded-xl bg-indigo-600 text-white text-xs font-bold"
            >
              Create Task
            </button>
          </div>
        </form>
      </Modal>

      {/* Archive Task Modal */}
      {archiveTaskModalOpen && activeTask && (
        <Modal
          isOpen={archiveTaskModalOpen}
          onClose={() => setArchiveTaskModalOpen(false)}
          title="Archive Task"
        >
          <div className="space-y-4">
            {archiveTaskError && (
              <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-semibold flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{archiveTaskError}</span>
              </div>
            )}
            <p className="text-sm text-slate-300">
              Are you sure you want to archive task <strong className="text-white">{activeTask.title}</strong>?
            </p>
            <div className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800 text-xs text-slate-400 space-y-1.5">
              <div>• The task status will be marked as ARCHIVED.</div>
              <div>• All review history, submissions, checklist subtasks, and comments are preserved.</div>
            </div>
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setArchiveTaskModalOpen(false)}
                className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={archiveTaskMutation.isPending}
                onClick={() => archiveTaskMutation.mutate(activeTask.id)}
                className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs flex items-center gap-1.5 disabled:opacity-50"
              >
                {archiveTaskMutation.isPending ? 'Archiving...' : 'Confirm Archive'}
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
