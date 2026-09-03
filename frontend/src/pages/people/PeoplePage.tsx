import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userApi, departmentApi, teamApi, taskApi, reportApi, activityApi, exportApi } from '../../api/services';
import { useAuthStore } from '../../store/authStore';
import { Modal } from '../../components/common/Modal';
import {
  Users,
  Plus,
  Mail,
  Shield,
  Building,
  CheckCircle,
  Copy,
  AlertCircle,
  Send,
  User as UserIcon,
  Phone,
  Calendar,
  CheckSquare,
  Clock,
  Briefcase,
  Activity,
  Layers,
  ExternalLink,
  ChevronRight,
  X,
  Edit2,
  Save,
  UserX,
  UserCheck,
  Download,
  FileText,
  Archive,
  Trash2,
  ShieldAlert,
} from 'lucide-react';

export const PeoplePage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'members' | 'invitations'>('members');
  const [inviteModalOpen, setInviteModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [selectedUser360, setSelectedUser360] = useState<any | null>(null);
  const [active360Tab, setActive360Tab] = useState<'overview' | 'tasks' | 'activity'>('overview');

  // Invite Form State
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('EMPLOYEE');
  const [departmentId, setDepartmentId] = useState('');
  const [teamId, setTeamId] = useState('');
  const [managerId, setManagerId] = useState('');
  const [createdInviteResult, setCreatedInviteResult] = useState<any | null>(null);
  const [copied, setCopied] = useState(false);

  // Edit Form State
  const [editingUserId, setEditingUserId] = useState<string | null>(null);
  const [editFullName, setEditFullName] = useState('');
  const [editRole, setEditRole] = useState('EMPLOYEE');
  const [editStatus, setEditStatus] = useState('ACTIVE');
  const [editDeptId, setEditDeptId] = useState('');
  const [editTeamId, setEditTeamId] = useState('');
  const [editManagerId, setEditManagerId] = useState('');
  const [editPhone, setEditPhone] = useState('');
  const [editTimezone, setEditTimezone] = useState('UTC');
  const [editError, setEditError] = useState<string | null>(null);

  // Deactivate/Reactivate state
  const [userToDeactivate, setUserToDeactivate] = useState<any | null>(null);
  const [userToReactivate, setUserToReactivate] = useState<any | null>(null);
  const [userToDeletePermanently, setUserToDeletePermanently] = useState<any | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [downloadingReport, setDownloadingReport] = useState(false);
  const [copiedInvite, setCopiedInvite] = useState(false);
  const [copiedToken, setCopiedToken] = useState<string | null>(null);

  const copyInviteLink = (token?: string) => {
    const t = token || createdInviteResult?.token;
    if (!t) return;
    const url = `${window.location.origin}/accept-invitation?token=${t}`;
    navigator.clipboard.writeText(url);
    if (token) {
      setCopiedToken(token);
      setTimeout(() => setCopiedToken(null), 2000);
    } else {
      setCopiedInvite(true);
      setTimeout(() => setCopiedInvite(false), 2000);
    }
  };

  const { user: currentUser } = useAuthStore();
  const queryClient = useQueryClient();

  const handleDownloadReport = async (userId: string, userName: string) => {
    setDownloadingReport(true);
    try {
      const res = await exportApi.employeePdf(userId);
      const blob = new Blob([res.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `employee-report-${userName.toLowerCase().replace(/[^a-z0-9]/g, '-')}-${new Date().toISOString().slice(0, 10)}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err: any) {
      alert(`Failed to download report: ${err.response?.data?.message || err.message}`);
    } finally {
      setDownloadingReport(false);
    }
  };

  const { data: usersData, isLoading: usersLoading } = useQuery({
    queryKey: ['users-all'],
    queryFn: () => userApi.getAll(0, 100),
  });

  const { data: invitationsData, isLoading: invitesLoading } = useQuery({
    queryKey: ['invitations-all'],
    queryFn: () => userApi.getInvitations(),
    enabled: activeTab === 'invitations',
  });

  const { data: deptsData } = useQuery({
    queryKey: ['departments-active'],
    queryFn: () => departmentApi.getAll(),
  });

  const { data: teamsData } = useQuery({
    queryKey: ['teams-active'],
    queryFn: () => teamApi.getAll(),
  });

  // Queries for 360 profile
  const { data: userReportData } = useQuery({
    queryKey: ['user-report', selectedUser360?.id],
    queryFn: () => reportApi.getEmployeeReport(selectedUser360!.id),
    enabled: !!selectedUser360?.id,
  });

  const { data: userTasksData } = useQuery({
    queryKey: ['user-tasks-360', selectedUser360?.id],
    queryFn: () => taskApi.getAll(undefined, selectedUser360!.id),
    enabled: !!selectedUser360?.id,
  });

  const { data: userActivitiesData } = useQuery({
    queryKey: ['user-activities-360', selectedUser360?.id],
    queryFn: () => activityApi.getTenant(0, 30),
    enabled: !!selectedUser360?.id,
  });

  const inviteMutation = useMutation({
    mutationFn: (payload: any) => userApi.invite(payload),
    onSuccess: (res: any) => {
      queryClient.invalidateQueries({ queryKey: ['users-all'] });
      queryClient.invalidateQueries({ queryKey: ['invitations-all'] });
      setCreatedInviteResult(res.data);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: any }) => userApi.update(id, payload),
    onSuccess: (res: any) => {
      queryClient.invalidateQueries({ queryKey: ['users-all'] });
      if (selectedUser360 && selectedUser360.id === editingUserId) {
        setSelectedUser360(res.data);
      }
      setEditModalOpen(false);
      setEditingUserId(null);
      setEditError(null);
    },
    onError: (err: any) => {
      setEditError(err.response?.data?.message || 'Failed to update employee');
    },
  });

  const resendMutation = useMutation({
    mutationFn: (id: string) => userApi.resendInvitation(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invitations-all'] });
      alert('Invitation email resent successfully!');
    },
  });

  const revokeMutation = useMutation({
    mutationFn: (id: string) => userApi.revokeInvitation(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invitations-all'] });
    },
  });

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => userApi.deactivate(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users-all'] });
      setUserToDeactivate(null);
      setActionError(null);
    },
    onError: (err: any) => {
      setActionError(err.response?.data?.message || err.message || 'Failed to deactivate user');
    },
  });

  const reactivateMutation = useMutation({
    mutationFn: (id: string) => userApi.reactivate(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users-all'] });
      setUserToReactivate(null);
      setActionError(null);
    },
    onError: (err: any) => {
      setActionError(err.response?.data?.message || err.message || 'Failed to reactivate user');
    },
  });

  const permanentDeleteUserMutation = useMutation({
    mutationFn: (id: string) => userApi.permanentDelete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users-all'] });
      queryClient.invalidateQueries({ queryKey: ['archive-summary'] });
      setUserToDeletePermanently(null);
      setActionError(null);
    },
    onError: (err: any) => {
      const serverMsg = err.response?.data?.message || err.response?.data?.error || err.message;
      setActionError(serverMsg || 'Failed to permanently delete user');
    },
  });

  const handleInvite = (e: React.FormEvent) => {
    e.preventDefault();
    inviteMutation.mutate({
      name: name.trim() || null,
      email: email.trim(),
      role,
      departmentId: departmentId || null,
      teamId: teamId || null,
      managerId: managerId || null,
    });
  };

  const handleOpenEdit = (user: any, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    setEditingUserId(user.id);
    setEditFullName(user.fullName || '');
    setEditRole(user.role || 'EMPLOYEE');
    setEditStatus(user.status || 'ACTIVE');
    setEditDeptId(user.departmentId || '');
    setEditTeamId(user.teamId || '');
    setEditManagerId(user.managerId || '');
    setEditPhone(user.phone || '');
    setEditTimezone(user.timezone || 'UTC');
    setEditError(null);
    setEditModalOpen(true);
  };

  const handleSaveEdit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingUserId) return;
    setEditError(null);
    updateMutation.mutate({
      id: editingUserId,
      payload: {
        fullName: editFullName.trim(),
        role: editRole,
        status: editStatus,
        departmentId: editDeptId || null,
        teamId: editTeamId || null,
        managerId: editManagerId || null,
        phone: editPhone.trim() || null,
        timezone: editTimezone.trim() || 'UTC',
      },
    });
  };

  const users = usersData?.data?.content || [];
  const invitations = invitationsData?.data?.content || [];
  const depts = deptsData?.data || [];
  const teams = teamsData?.data || [];
  const userReport = userReportData?.data;
  const userTasks = userTasksData?.data?.content || [];
  const allActivities = userActivitiesData?.data?.content || [];
  const userActivities = allActivities.filter((a: any) => a.userId === selectedUser360?.id);

  const filteredTeams = departmentId
    ? teams.filter((t: any) => t.departmentId === departmentId)
    : teams;

  const filteredEditTeams = editDeptId
    ? teams.filter((t: any) => t.departmentId === editDeptId)
    : teams;

  const getDeptName = (deptId?: string) => {
    if (!deptId) return 'Unassigned';
    return depts.find((d: any) => d.id === deptId)?.name || '—';
  };

  const getTeamName = (tId?: string) => {
    if (!tId) return 'Unassigned';
    return teams.find((t: any) => t.id === tId)?.name || '—';
  };

  const getManagerName = (mgrId?: string) => {
    if (!mgrId) return 'None (Direct to Org)';
    const mgr = users.find((u: any) => u.id === mgrId);
    return mgr ? `${mgr.fullName} (${mgr.role === 'TENANT_ADMIN' ? 'Admin' : mgr.role})` : '—';
  };

  return (
    <div className="space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider border border-indigo-500/20 mb-2">
            <Users className="w-3.5 h-3.5" />
            Organization Directory
          </div>
          <h1 className="text-3xl font-black font-display text-white">People & Members</h1>
          <p className="text-slate-400 text-sm">
            Manage organization members, view 360° employee performance, and track automated invitations.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => {
              setCreatedInviteResult(null);
              setName('');
              setEmail('');
              setDepartmentId('');
              setTeamId('');
              setManagerId('');
              setInviteModalOpen(true);
            }}
            className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs shadow-lg shadow-indigo-600/25 flex items-center gap-2 transition-all"
          >
            <Plus className="w-4 h-4" />
            <span>Invite Employee</span>
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-800 pb-3">
        <button
          onClick={() => setActiveTab('members')}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            activeTab === 'members'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/25'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          Active Members ({users.length})
        </button>
        <button
          onClick={() => setActiveTab('invitations')}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            activeTab === 'invitations'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/25'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          Pending Invitations ({invitations.length})
        </button>
      </div>

      {/* Users Table */}
      {activeTab === 'members' && (
        usersLoading ? (
          <div className="text-center py-20 text-slate-500 text-sm">Loading people directory...</div>
        ) : (
          <div className="glass-panel rounded-3xl overflow-x-auto border border-slate-800">
            <table className="w-full text-left text-sm text-slate-300 min-w-[950px]">
              <thead className="bg-slate-900/80 text-xs font-bold uppercase tracking-wider text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="px-6 py-4">Employee</th>
                  <th className="px-6 py-4">Employee Code</th>
                  <th className="px-6 py-4">Department</th>
                  <th className="px-6 py-4">Team</th>
                  <th className="px-6 py-4">Role</th>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4 text-right min-w-[340px] whitespace-nowrap">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {users.map((u: any) => (
                  <tr
                    key={u.id}
                    onClick={() => {
                      setSelectedUser360(u);
                      setActive360Tab('overview');
                    }}
                    className="hover:bg-slate-800/60 cursor-pointer transition-colors group"
                  >
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-indigo-400 font-bold text-sm">
                          {u.fullName?.charAt(0) || 'U'}
                        </div>
                        <div>
                          <div className="font-bold text-white group-hover:text-indigo-400 transition-colors flex items-center gap-1.5">
                            <span>{u.fullName}</span>
                          </div>
                          <div className="text-xs text-slate-500">{u.email}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 font-mono text-indigo-400 font-semibold text-xs">
                      {u.employeeCode || '—'}
                    </td>
                    <td className="px-6 py-4 text-xs font-semibold text-slate-300">
                      {getDeptName(u.departmentId)}
                    </td>
                    <td className="px-6 py-4 text-xs text-slate-400">
                      {getTeamName(u.teamId)}
                    </td>
                    <td className="px-6 py-4">
                      <span className="text-xs px-2.5 py-1 rounded-lg font-bold bg-slate-800 text-slate-300 border border-slate-700">
                        {u.role === 'TENANT_ADMIN' ? 'Admin' : u.role}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <span className={`text-xs px-2.5 py-1 rounded-lg font-bold border ${
                        u.status === 'ACTIVE'
                          ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                          : 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                      }`}>
                        {u.status || 'ACTIVE'}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right min-w-[340px] whitespace-nowrap">
                      <div className="flex items-center justify-end gap-2 whitespace-nowrap">
                        <button
                          onClick={(e) => handleOpenEdit(u, e)}
                          className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold transition-colors inline-flex items-center gap-1 shrink-0"
                        >
                          <Edit2 className="w-3 h-3 text-indigo-400" />
                          <span>Edit</span>
                        </button>
                        {u.status === 'ACTIVE' && currentUser?.id !== u.id && (
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setActionError(null);
                              setUserToDeactivate(u);
                            }}
                            className="px-2.5 py-1.5 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 text-xs font-bold transition-colors inline-flex items-center gap-1 border border-rose-500/20 shrink-0"
                            title="Deactivate account"
                          >
                            <UserX className="w-3 h-3" />
                            <span>Deactivate</span>
                          </button>
                        )}
                        {u.status === 'INACTIVE' && (
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setActionError(null);
                              setUserToReactivate(u);
                            }}
                            className="px-2.5 py-1.5 rounded-lg bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 text-xs font-bold transition-colors inline-flex items-center gap-1 border border-emerald-500/20 shrink-0"
                            title="Reactivate account"
                          >
                            <UserCheck className="w-3 h-3" />
                            <span>Reactivate</span>
                          </button>
                        )}
                        {currentUser?.role === 'TENANT_ADMIN' && currentUser?.id !== u.id && (
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setActionError(null);
                              setUserToDeletePermanently(u);
                            }}
                            className="px-2.5 py-1.5 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 text-xs font-bold transition-colors inline-flex items-center gap-1 border border-rose-500/20 shrink-0"
                            title="Delete Permanently"
                          >
                            <Trash2 className="w-3 h-3" />
                            <span>Delete</span>
                          </button>
                        )}
                        <button
                          onClick={() => {
                            setSelectedUser360(u);
                            setActive360Tab('overview');
                          }}
                          className="px-3 py-1.5 rounded-lg bg-indigo-600/10 hover:bg-indigo-600/20 text-indigo-400 text-xs font-bold transition-colors shrink-0"
                        >
                          360° Profile
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}

      {/* Invitations Table */}
      {activeTab === 'invitations' && (
        invitesLoading ? (
          <div className="text-center py-20 text-slate-500 text-sm">Loading pending invitations...</div>
        ) : (
          <div className="glass-panel rounded-3xl overflow-hidden border border-slate-800">
            {invitations.length === 0 ? (
              <div className="text-center py-16 text-slate-500 text-sm">
                No pending invitations. Click "Invite Employee" above to send one.
              </div>
            ) : (
              <table className="w-full text-left text-sm text-slate-300">
                <thead className="bg-slate-900/80 text-xs font-bold uppercase tracking-wider text-slate-400 border-b border-slate-800">
                  <tr>
                    <th className="px-6 py-4">Invited Email</th>
                    <th className="px-6 py-4">Name</th>
                    <th className="px-6 py-4">Role</th>
                    <th className="px-6 py-4">Email Status</th>
                    <th className="px-6 py-4">Status</th>
                    <th className="px-6 py-4">Expires</th>
                    <th className="px-6 py-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {invitations.map((inv: any) => (
                    <tr key={inv.id} className="hover:bg-slate-800/40 transition-colors">
                      <td className="px-6 py-4 font-bold text-white">
                        {inv.email}
                      </td>
                      <td className="px-6 py-4 text-xs text-slate-300">
                        {inv.name || '—'}
                      </td>
                      <td className="px-6 py-4">
                        <span className="text-xs px-2.5 py-1 rounded-lg font-bold bg-slate-800 text-slate-300 border border-slate-700">
                          {inv.role}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <span className={`text-xs px-2.5 py-1 rounded-lg font-bold border ${
                          inv.emailStatus === 'EMAIL_SENT'
                            ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                            : inv.emailStatus === 'EMAIL_FAILED'
                            ? 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                            : 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                        }`}>
                          {inv.emailStatus || 'EMAIL_PENDING'}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <span className="text-xs px-2.5 py-1 rounded-lg font-bold bg-slate-800 text-slate-400">
                          {inv.status}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-xs text-slate-500">
                        {inv.expiresAt ? new Date(inv.expiresAt).toLocaleDateString() : '7 days'}
                      </td>
                      <td className="px-6 py-4 text-right space-x-2 whitespace-nowrap">
                        {inv.token && (
                          <button
                            type="button"
                            onClick={() => copyInviteLink(inv.token)}
                            className="text-xs text-indigo-400 hover:text-indigo-300 font-semibold inline-flex items-center gap-1 mr-2"
                            title="Copy single-use invitation link"
                          >
                            <Copy className="w-3 h-3" />
                            <span>{copiedToken === inv.token ? 'Copied!' : 'Copy Link'}</span>
                          </button>
                        )}
                        <button
                          onClick={() => resendMutation.mutate(inv.id)}
                          className="text-xs text-indigo-400 hover:text-indigo-300 font-semibold"
                        >
                          Resend
                        </button>
                        <button
                          onClick={() => revokeMutation.mutate(inv.id)}
                          className="text-xs text-rose-400 hover:text-rose-300 font-semibold"
                        >
                          Revoke
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )
      )}

      {/* Invite Modal */}
      <Modal isOpen={inviteModalOpen} onClose={() => setInviteModalOpen(false)} title="Invite New Employee">
        {createdInviteResult ? (
          <div className="space-y-5 text-center py-4">
            <div className={`w-16 h-16 rounded-full flex items-center justify-center mx-auto ${
              createdInviteResult.emailStatus === 'EMAIL_FAILED'
                ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
            }`}>
              {createdInviteResult.emailStatus === 'EMAIL_FAILED' ? (
                <AlertCircle className="w-8 h-8" />
              ) : (
                <CheckCircle className="w-8 h-8" />
              )}
            </div>
            <div>
              <h3 className="text-xl font-bold text-white font-display">
                {createdInviteResult.emailStatus === 'EMAIL_FAILED'
                  ? 'Invitation Created (Email Failed)'
                  : 'Invitation Dispatched!'}
              </h3>
              <p className="text-sm text-slate-400 mt-1">
                {createdInviteResult.emailStatus === 'EMAIL_FAILED' ? (
                  <>
                    An invitation was created for{' '}
                    <span className="text-indigo-400 font-semibold">{createdInviteResult.email}</span>,
                    but email delivery failed. Please copy the invitation link below to share directly.
                  </>
                ) : (
                  <>
                    An official WorkHive invitation email was dispatched to{' '}
                    <span className="text-indigo-400 font-semibold">{createdInviteResult.email}</span>.
                  </>
                )}
              </p>
            </div>

            {createdInviteResult.token && (
              <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800 text-left space-y-2">
                <div className="text-xs font-semibold uppercase tracking-wider text-slate-400 flex items-center justify-between">
                  <span>Direct Invitation Link (Single-Use Fallback)</span>
                  {copiedInvite && <span className="text-emerald-400 font-bold lowercase text-[11px]">copied to clipboard</span>}
                </div>
                <div className="flex items-center gap-2">
                  <input
                    type="text"
                    readOnly
                    value={`${window.location.origin}/accept-invitation?token=${createdInviteResult.token}`}
                    className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-300 text-xs font-mono select-all"
                  />
                  <button
                    type="button"
                    onClick={() => copyInviteLink(createdInviteResult.token)}
                    className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs flex items-center gap-1.5 flex-shrink-0 transition-colors"
                  >
                    <Copy className="w-3.5 h-3.5" />
                    <span>{copiedInvite ? 'Copied!' : 'Copy'}</span>
                  </button>
                </div>
              </div>
            )}

            <button
              onClick={() => setInviteModalOpen(false)}
              className="w-full py-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs"
            >
              Done
            </button>
          </div>
        ) : (
          <form onSubmit={handleInvite} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Full Name
              </label>
              <input
                type="text"
                placeholder="e.g. Jane Doe"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Work Email *
              </label>
              <input
                type="email"
                required
                placeholder="jane@company.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                  Role *
                </label>
                <select
                  value={role}
                  onChange={(e) => setRole(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
                >
                  <option value="EMPLOYEE">Employee</option>
                  <option value="MANAGER">Manager</option>
                  <option value="TENANT_ADMIN">Tenant Admin</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                  Department
                </label>
                <select
                  value={departmentId}
                  onChange={(e) => {
                    setDepartmentId(e.target.value);
                    setTeamId('');
                  }}
                  className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
                >
                  <option value="">Select Department</option>
                  {depts.map((d: any) => (
                    <option key={d.id} value={d.id}>{d.name}</option>
                  ))}
                </select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                  Team
                </label>
                <select
                  value={teamId}
                  onChange={(e) => setTeamId(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
                >
                  <option value="">Select Team</option>
                  {filteredTeams.map((t: any) => (
                    <option key={t.id} value={t.id}>{t.name}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                  Reporting Manager
                </label>
                <select
                  value={managerId}
                  onChange={(e) => setManagerId(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
                >
                  <option value="">Select Manager</option>
                  {users.map((u: any) => (
                    <option key={u.id} value={u.id}>{u.fullName} ({u.role})</option>
                  ))}
                </select>
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-3">
              <button
                type="button"
                onClick={() => setInviteModalOpen(false)}
                className="px-4 py-2.5 rounded-xl bg-slate-800 text-slate-300 text-xs font-bold"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={inviteMutation.isPending}
                className="px-5 py-2.5 rounded-xl bg-indigo-600 text-white text-xs font-bold shadow-lg shadow-indigo-600/25 flex items-center gap-2 disabled:opacity-50"
              >
                <Send className="w-3.5 h-3.5" />
                <span>{inviteMutation.isPending ? 'Sending...' : 'Send Invitation'}</span>
              </button>
            </div>
          </form>
        )}
      </Modal>

      {/* Edit Employee Modal */}
      <Modal isOpen={editModalOpen} onClose={() => setEditModalOpen(false)} title="Edit Employee Organization Data">
        <form onSubmit={handleSaveEdit} className="space-y-4">
          {editError && (
            <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{editError}</span>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Full Name *
            </label>
            <input
              type="text"
              required
              value={editFullName}
              onChange={(e) => setEditFullName(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Role *
              </label>
              <select
                value={editRole}
                onChange={(e) => setEditRole(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              >
                <option value="EMPLOYEE">Employee</option>
                <option value="MANAGER">Manager</option>
                <option value="TENANT_ADMIN">Tenant Admin</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Status *
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

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Department
              </label>
              <select
                value={editDeptId}
                onChange={(e) => {
                  setEditDeptId(e.target.value);
                  setEditTeamId('');
                }}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              >
                <option value="">Unassigned</option>
                {depts.map((d: any) => (
                  <option key={d.id} value={d.id}>{d.name}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Team
              </label>
              <select
                value={editTeamId}
                onChange={(e) => setEditTeamId(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              >
                <option value="">Unassigned</option>
                {filteredEditTeams.map((t: any) => (
                  <option key={t.id} value={t.id}>{t.name}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Reporting Manager
              </label>
              <select
                value={editManagerId}
                onChange={(e) => setEditManagerId(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              >
                <option value="">None (Direct to Org)</option>
                {users.filter((u: any) => u.id !== editingUserId).map((u: any) => (
                  <option key={u.id} value={u.id}>{u.fullName} ({u.role})</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Phone Contact
              </label>
              <input
                type="text"
                placeholder="+1 555-0199"
                value={editPhone}
                onChange={(e) => setEditPhone(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Timezone
            </label>
            <input
              type="text"
              placeholder="e.g. America/New_York or UTC"
              value={editTimezone}
              onChange={(e) => setEditTimezone(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div className="flex justify-end gap-3 pt-3">
            <button
              type="button"
              onClick={() => setEditModalOpen(false)}
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
              <span>{updateMutation.isPending ? 'Saving Changes...' : 'Save Changes'}</span>
            </button>
          </div>
        </form>
      </Modal>

      {/* Employee 360° Profile Modal */}
      {selectedUser360 && (
        <Modal
          isOpen={!!selectedUser360}
          onClose={() => setSelectedUser360(null)}
          title={`Employee 360° Profile — ${selectedUser360.fullName}`}
        >
          <div className="space-y-6">
            {/* Header Identity Card */}
            <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex items-center gap-4 min-w-0 flex-1">
                <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white font-bold text-xl shadow-lg shadow-indigo-500/20 flex-shrink-0">
                  {selectedUser360.fullName?.charAt(0) || 'U'}
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="text-lg font-bold text-white truncate max-w-sm sm:max-w-md">{selectedUser360.fullName}</h3>
                    <div className="flex items-center gap-1.5 flex-wrap">
                      <span className="text-xs px-2.5 py-0.5 rounded-full font-bold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 whitespace-nowrap flex-shrink-0">
                        {selectedUser360.role}
                      </span>
                      <span className={`text-xs px-2.5 py-0.5 rounded-full font-bold border whitespace-nowrap flex-shrink-0 ${
                        selectedUser360.status === 'ACTIVE'
                          ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                          : 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                      }`}>
                        {selectedUser360.status || 'ACTIVE'}
                      </span>
                    </div>
                  </div>
                  <div className="text-xs text-slate-400 mt-1 flex flex-wrap items-center gap-x-4 gap-y-1.5">
                    <span className="inline-flex items-center gap-1.5 whitespace-nowrap text-slate-300 min-w-0">
                      <Mail className="w-3.5 h-3.5 flex-shrink-0 text-slate-400" />
                      <span>{selectedUser360.email}</span>
                    </span>
                    <span className="font-mono text-indigo-300 whitespace-nowrap flex-shrink-0">Code: {selectedUser360.employeeCode || '—'}</span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-2 flex-shrink-0 self-start sm:self-start -mt-0.5 sm:-mt-1">
                <button
                  type="button"
                  disabled={downloadingReport}
                  onClick={() => handleDownloadReport(selectedUser360.id, selectedUser360.fullName || selectedUser360.email)}
                  className="px-3.5 py-2 rounded-xl bg-indigo-600/10 hover:bg-indigo-600/20 text-indigo-400 text-xs font-bold border border-indigo-500/20 flex items-center gap-1.5 transition-colors disabled:opacity-50 whitespace-nowrap"
                  title="Download full executive dossier PDF"
                >
                  <Download className="w-3.5 h-3.5 flex-shrink-0" />
                  <span>{downloadingReport ? 'Generating PDF...' : 'Download Report'}</span>
                </button>
                <button
                  onClick={(e) => handleOpenEdit(selectedUser360, e)}
                  className="px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold border border-slate-700 flex items-center gap-1.5 transition-colors whitespace-nowrap"
                >
                  <Edit2 className="w-3.5 h-3.5 text-indigo-400 flex-shrink-0" />
                  <span>Edit Details</span>
                </button>
              </div>
            </div>

            {/* 360 Navigation Tabs */}
            <div className="flex items-center gap-2 border-b border-slate-800 pb-2">
              <button
                onClick={() => setActive360Tab('overview')}
                className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all ${
                  active360Tab === 'overview'
                    ? 'bg-indigo-600 text-white'
                    : 'text-slate-400 hover:text-white hover:bg-slate-800'
                }`}
              >
                Overview & Org
              </button>
              <button
                onClick={() => setActive360Tab('tasks')}
                className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all ${
                  active360Tab === 'tasks'
                    ? 'bg-indigo-600 text-white'
                    : 'text-slate-400 hover:text-white hover:bg-slate-800'
                }`}
              >
                Tasks ({userTasks.length})
              </button>
              <button
                onClick={() => setActive360Tab('activity')}
                className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all ${
                  active360Tab === 'activity'
                    ? 'bg-indigo-600 text-white'
                    : 'text-slate-400 hover:text-white hover:bg-slate-800'
                }`}
              >
                Activity Timeline ({userActivities.length})
              </button>
            </div>

            {/* Tab: Overview */}
            {active360Tab === 'overview' && (
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">Department</span>
                    <div className="text-sm font-bold text-white">{getDeptName(selectedUser360.departmentId)}</div>
                  </div>
                  <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">Team</span>
                    <div className="text-sm font-bold text-white">{getTeamName(selectedUser360.teamId)}</div>
                  </div>
                  <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">Reporting Manager</span>
                    <div className="text-sm font-bold text-white">{getManagerName(selectedUser360.managerId)}</div>
                  </div>
                  <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">Phone Contact</span>
                    <div className="text-sm font-bold text-white">{selectedUser360.phone || 'Not provided'}</div>
                  </div>
                  <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">Timezone</span>
                    <div className="text-sm font-bold text-white">{selectedUser360.timezone || 'UTC'}</div>
                  </div>
                  <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">Account Status</span>
                    <div className="text-sm font-bold text-emerald-400">{selectedUser360.status || 'ACTIVE'}</div>
                  </div>
                </div>

                {/* Productivity Rollup */}
                {userReport && (
                  <div className="p-4 rounded-xl bg-indigo-500/5 border border-indigo-500/20 space-y-3">
                    <div className="text-xs font-bold text-indigo-400 uppercase tracking-wider">
                      Productivity Signals (Last 30 Days)
                    </div>
                    <div className="grid grid-cols-4 gap-3 text-center">
                      <div className="p-2 rounded-lg bg-slate-900">
                        <div className="text-lg font-black text-emerald-400">{userReport.completionRate || 0}%</div>
                        <div className="text-[10px] text-slate-400">Completion</div>
                      </div>
                      <div className="p-2 rounded-lg bg-slate-900">
                        <div className="text-lg font-black text-indigo-400">{userReport.completedTasks || 0}</div>
                        <div className="text-[10px] text-slate-400">Done Tasks</div>
                      </div>
                      <div className="p-2 rounded-lg bg-slate-900">
                        <div className="text-lg font-black text-sky-400">{userReport.daysPresent || 0}</div>
                        <div className="text-[10px] text-slate-400">Days Present</div>
                      </div>
                      <div className="p-2 rounded-lg bg-slate-900">
                        <div className="text-lg font-black text-amber-400">{userReport.leaveDaysTaken || 0}</div>
                        <div className="text-[10px] text-slate-400">Leaves Taken</div>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* Tab: Tasks */}
            {active360Tab === 'tasks' && (
              <div className="space-y-3 max-h-72 overflow-y-auto">
                {userTasks.length === 0 ? (
                  <div className="text-center py-8 text-slate-500 text-xs">No tasks currently assigned to this member.</div>
                ) : (
                  userTasks.map((t: any) => (
                    <div key={t.id} className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center justify-between">
                      <div className="space-y-1">
                        <div className="text-xs font-bold text-white">{t.title}</div>
                        <div className="text-[10px] text-slate-400">Priority: <span className="text-indigo-400 font-semibold">{t.priority}</span> • Due: {t.dueDate || 'No date'}</div>
                      </div>
                      <span className={`text-[10px] px-2 py-0.5 rounded font-bold border ${
                        t.status === 'COMPLETED' ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' :
                        t.status === 'IN_PROGRESS' ? 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20' :
                        t.status === 'REVIEW' ? 'bg-amber-500/10 text-amber-400 border-amber-500/20' :
                        'bg-slate-800 text-slate-400 border-slate-700'
                      }`}>
                        {t.status}
                      </span>
                    </div>
                  ))
                )}
              </div>
            )}

            {/* Tab: Activity Timeline */}
            {active360Tab === 'activity' && (
              <div className="space-y-3 max-h-72 overflow-y-auto">
                {userActivities.length === 0 ? (
                  <div className="text-center py-8 text-slate-500 text-xs">No recent actions recorded for this member.</div>
                ) : (
                  userActivities.map((act: any) => (
                    <div key={act.id} className="p-3 rounded-xl bg-slate-900/40 border border-slate-800/80 space-y-1">
                      <div className="flex items-center justify-between text-[10px]">
                        <span className="font-bold text-indigo-400">{act.activityType}</span>
                        <span className="text-slate-500">{new Date(act.createdAt).toLocaleDateString()}</span>
                      </div>
                      <div className="text-xs text-white">{act.title}</div>
                      {act.details && <div className="text-[10px] text-slate-400">{act.details}</div>}
                    </div>
                  ))
                )}
              </div>
            )}

            <div className="flex justify-end pt-2 border-t border-slate-800">
              <button
                onClick={() => setSelectedUser360(null)}
                className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs"
              >
                Close 360° Profile
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* Deactivate User Modal */}
      {userToDeactivate && (
        <Modal
          isOpen={!!userToDeactivate}
          onClose={() => {
            setUserToDeactivate(null);
            setActionError(null);
          }}
          title="Deactivate Employee Account"
        >
          <div className="space-y-4">
            {actionError && (
              <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-semibold flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{actionError}</span>
              </div>
            )}
            <p className="text-sm text-slate-300">
              Are you sure you want to deactivate <strong className="text-white">{userToDeactivate.fullName || userToDeactivate.email}</strong>?
            </p>
            <div className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800 text-xs text-slate-400 space-y-1.5">
              <div>• The user will no longer be able to log into WorkHive.</div>
              <div>• Any active personal email sessions and integration keys are revoked immediately.</div>
              <div>• Historical tasks, attendance, leaves, activity records, and audit logs remain securely preserved.</div>
            </div>
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => {
                  setUserToDeactivate(null);
                  setActionError(null);
                }}
                className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={deactivateMutation.isPending}
                onClick={() => deactivateMutation.mutate(userToDeactivate.id)}
                className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs flex items-center gap-1.5 disabled:opacity-50"
              >
                {deactivateMutation.isPending ? 'Deactivating...' : 'Confirm Deactivation'}
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* Reactivate User Modal */}
      {userToReactivate && (
        <Modal
          isOpen={!!userToReactivate}
          onClose={() => {
            setUserToReactivate(null);
            setActionError(null);
          }}
          title="Reactivate Employee Account"
        >
          <div className="space-y-4">
            {actionError && (
              <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-semibold flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{actionError}</span>
              </div>
            )}
            <p className="text-sm text-slate-300">
              Reactivate login access and workspace membership for <strong className="text-white">{userToReactivate.fullName || userToReactivate.email}</strong>?
            </p>
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => {
                  setUserToReactivate(null);
                  setActionError(null);
                }}
                className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={reactivateMutation.isPending}
                onClick={() => reactivateMutation.mutate(userToReactivate.id)}
                className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs flex items-center gap-1.5 disabled:opacity-50"
              >
                {reactivateMutation.isPending ? 'Reactivating...' : 'Confirm Reactivation'}
              </button>
            </div>
          </div>
        </Modal>
      )}


      {/* Delete Permanently Modal */}
      {userToDeletePermanently && (
        <Modal
          isOpen={!!userToDeletePermanently}
          onClose={() => {
            setUserToDeletePermanently(null);
            setActionError(null);
          }}
          title={`Delete Permanently — ${userToDeletePermanently.fullName || userToDeletePermanently.email}`}
        >
          <div className="space-y-4">
            {actionError && (
              <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-semibold flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{actionError}</span>
              </div>
            )}
            <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs font-semibold flex items-start gap-2.5">
              <ShieldAlert className="w-4 h-4 shrink-0 mt-0.5 text-rose-400" />
              <div>
                <strong>Warning: Permanent Deletion</strong>
                <p className="font-normal text-rose-300/80 mt-1">
                  This will permanently remove the WorkHive authentication identity, login credentials,
                  and active sessions for this user.
                </p>
              </div>
            </div>
            <p className="text-sm text-slate-300">
              Are you sure you want to permanently delete <strong className="text-white">{userToDeletePermanently.fullName || userToDeletePermanently.email}</strong>?
            </p>
            <div className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800 text-xs text-slate-400 space-y-1">
              <div>• Historical project and task records remain safe with manager/creator cleanly reassigned.</div>
              <div>• Permanently deleted users will NOT appear in Archive.</div>
              <div>• This action cannot be undone.</div>
            </div>
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => {
                  setUserToDeletePermanently(null);
                  setActionError(null);
                }}
                className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={permanentDeleteUserMutation.isPending}
                onClick={() => permanentDeleteUserMutation.mutate(userToDeletePermanently.id)}
                className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs flex items-center gap-1.5 disabled:opacity-50"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>{permanentDeleteUserMutation.isPending ? 'Deleting...' : 'Delete Permanently'}</span>
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
