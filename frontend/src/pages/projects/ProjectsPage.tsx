import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { projectApi, userApi, departmentApi, teamApi } from '../../api/services';
import { Project } from '../../types';
import { Modal } from '../../components/common/Modal';
import { Link } from 'react-router-dom';
import {
  FolderKanban,
  Plus,
  ArrowRight,
  Clock,
  CheckCircle2,
  AlertTriangle,
  Flame,
  Shield,
} from 'lucide-react';

export const ProjectsPage: React.FC = () => {
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState('MEDIUM');
  const [targetDate, setTargetDate] = useState('');
  const [departmentId, setDepartmentId] = useState('');
  const [teamId, setTeamId] = useState('');

  const queryClient = useQueryClient();

  const { data: projectsData, isLoading } = useQuery({
    queryKey: ['projects-list'],
    queryFn: () => projectApi.getAll(),
  });

  const { data: deptsData } = useQuery({
    queryKey: ['departments-active'],
    queryFn: () => departmentApi.getAll(),
  });

  const { data: teamsData } = useQuery({
    queryKey: ['teams-active'],
    queryFn: () => teamApi.getAll(),
  });

  const createProjectMutation = useMutation({
    mutationFn: (payload: any) => projectApi.create(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects-list'] });
      setCreateModalOpen(false);
      setName('');
      setDescription('');
      setTargetDate('');
    },
  });

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    createProjectMutation.mutate({
      name,
      description,
      priority,
      targetDate: targetDate || null,
      departmentId: departmentId || null,
      teamId: teamId || null,
    });
  };

  const projects = projectsData?.data?.content || [];
  const depts = deptsData?.data || [];
  const teams = teamsData?.data || [];

  const healthBadges = {
    ON_TRACK: { label: 'On Track', color: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' },
    AT_RISK: { label: 'At Risk', color: 'bg-amber-500/10 text-amber-400 border-amber-500/20' },
    OFF_TRACK: { label: 'Off Track', color: 'bg-rose-500/10 text-rose-400 border-rose-500/20' },
    COMPLETED: { label: 'Completed', color: 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20' },
  };

  return (
    <div className="space-y-8">
      {/* Header with Create CTA */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider border border-indigo-500/20 mb-2">
            <FolderKanban className="w-3.5 h-3.5" />
            Project Hub
          </div>
          <h1 className="text-3xl font-black font-display text-white">Projects</h1>
          <p className="text-slate-400 text-sm">
            Track real progress, derived health, milestones, and team assignments.
          </p>
        </div>

        <button
          onClick={() => setCreateModalOpen(true)}
          className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs shadow-lg shadow-indigo-600/25 flex items-center gap-2 self-start sm:self-auto transition-all"
        >
          <Plus className="w-4 h-4" />
          <span>New Project</span>
        </button>
      </div>

      {/* Projects Grid */}
      {isLoading ? (
        <div className="text-center py-20 text-slate-500 text-sm">Loading projects...</div>
      ) : projects.length === 0 ? (
        <div className="glass-panel p-12 text-center rounded-3xl">
          <FolderKanban className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <h3 className="text-lg font-bold text-white font-display">No Projects Yet</h3>
          <p className="text-sm text-slate-400 mt-1">Create your first organization project to get started.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {projects.map((project) => {
            const health = healthBadges[project.health] || healthBadges.ON_TRACK;
            return (
              <Link
                key={project.id}
                to={`/projects/${project.id}`}
                className="glass-panel glass-card-hover p-6 rounded-3xl border border-slate-800 flex flex-col justify-between block group"
              >
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className={`text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded border ${health.color}`}>
                      {health.label}
                    </span>
                    <span className="text-xs text-slate-500 font-mono">
                      {project.priority} Priority
                    </span>
                  </div>

                  <h3 className="text-lg font-bold text-white font-display group-hover:text-indigo-400 transition-colors">
                    {project.name}
                  </h3>

                  <p className="text-xs text-slate-400 line-clamp-2">
                    {project.description || 'No description provided.'}
                  </p>
                </div>

                <div className="mt-6 space-y-4 pt-4 border-t border-slate-800/80">
                  {/* Real Calculated Progress Bar */}
                  <div>
                    <div className="flex items-center justify-between text-xs mb-1.5">
                      <span className="text-slate-400 font-medium">Work Progress</span>
                      <span className="font-bold text-indigo-400 font-display">{project.progress}%</span>
                    </div>
                    <div className="h-2 w-full bg-slate-800 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-gradient-to-r from-indigo-500 to-violet-500 rounded-full transition-all duration-500"
                        style={{ width: `${project.progress}%` }}
                      />
                    </div>
                  </div>

                  <div className="flex items-center justify-between text-xs text-slate-500">
                    <span className="flex items-center gap-1">
                      <Clock className="w-3.5 h-3.5" />
                      {project.targetDate ? `Due ${project.targetDate}` : 'No deadline'}
                    </span>
                    <span className="text-indigo-400 font-semibold flex items-center gap-1 group-hover:translate-x-1 transition-transform">
                      Workspace <ArrowRight className="w-3.5 h-3.5" />
                    </span>
                  </div>
                </div>
              </Link>
            );
          })}
        </div>
      )}

      {/* Create Project Modal */}
      <Modal isOpen={createModalOpen} onClose={() => setCreateModalOpen(false)} title="Create New Project">
        <form onSubmit={handleCreate} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Project Name *
            </label>
            <input
              type="text"
              required
              placeholder="e.g. Mobile App Redesign"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Description
            </label>
            <textarea
              rows={3}
              placeholder="Project goals, scope, and deliverables..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 text-sm"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Priority
              </label>
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white focus:outline-none focus:border-indigo-500 text-sm"
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Target Completion Date
              </label>
              <input
                type="date"
                value={targetDate}
                onChange={(e) => setTargetDate(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white focus:outline-none focus:border-indigo-500 text-sm"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Department (Optional)
              </label>
              <select
                value={departmentId}
                onChange={(e) => setDepartmentId(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white focus:outline-none focus:border-indigo-500 text-sm"
              >
                <option value="">None</option>
                {depts.map((d) => (
                  <option key={d.id} value={d.id}>{d.name}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Team (Optional)
              </label>
              <select
                value={teamId}
                onChange={(e) => setTeamId(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white focus:outline-none focus:border-indigo-500 text-sm"
              >
                <option value="">None</option>
                {teams.map((t) => (
                  <option key={t.id} value={t.id}>{t.name}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-4">
            <button
              type="button"
              onClick={() => setCreateModalOpen(false)}
              className="px-4 py-2.5 rounded-xl bg-slate-800 text-slate-300 text-xs font-bold hover:bg-slate-700"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={createProjectMutation.isPending}
              className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold shadow-lg shadow-indigo-600/25 disabled:opacity-50"
            >
              Create Project
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
