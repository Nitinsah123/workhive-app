import React, { useState, useEffect } from 'react';
import { useAuthStore } from '../../store/authStore';
import { userApi, departmentApi, teamApi } from '../../api/services';
import { useMutation, useQuery } from '@tanstack/react-query';
import { User, Shield, Lock, Save, CheckCircle2, AlertCircle, Building, Users2, UserCheck } from 'lucide-react';

export const ProfilePage: React.FC = () => {
  const { user: authUser, tenant } = useAuthStore();

  const { data: meData } = useQuery({
    queryKey: ['user-me'],
    queryFn: () => userApi.getMe(),
  });

  const { data: deptsData } = useQuery({
    queryKey: ['departments-active'],
    queryFn: () => departmentApi.getAll(),
  });

  const { data: teamsData } = useQuery({
    queryKey: ['teams-active'],
    queryFn: () => teamApi.getAll(),
  });

  const { data: usersData } = useQuery({
    queryKey: ['users-all'],
    queryFn: () => userApi.getAll(0, 100),
  });

  const currentUser = meData?.data || authUser;
  const departments = deptsData?.data || [];
  const teams = teamsData?.data || [];
  const allUsers = usersData?.data?.content || [];

  const currentDept = departments.find((d: any) => d.id === currentUser?.departmentId);
  const currentTeam = teams.find((t: any) => t.id === currentUser?.teamId);
  const currentManager = allUsers.find((u: any) => u.id === currentUser?.managerId);

  const [fullName, setFullName] = useState(currentUser?.fullName || '');
  const [phone, setPhone] = useState(currentUser?.phone || '');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [profileMsg, setProfileMsg] = useState<string | null>(null);
  const [pwdMsg, setPwdMsg] = useState<string | null>(null);
  const [pwdError, setPwdError] = useState<string | null>(null);

  useEffect(() => {
    if (meData?.data) {
      setFullName(meData.data.fullName || '');
      setPhone(meData.data.phone || '');
    }
  }, [meData]);

  const updateProfileMutation = useMutation({
    mutationFn: (payload: any) => userApi.updateProfile(payload),
    onSuccess: () => {
      setProfileMsg('Profile updated successfully.');
      setTimeout(() => setProfileMsg(null), 3000);
    },
  });

  const changePasswordMutation = useMutation({
    mutationFn: (payload: any) => userApi.changePassword(payload),
    onSuccess: () => {
      setPwdMsg('Password updated successfully.');
      setCurrentPassword('');
      setNewPassword('');
      setPwdError(null);
      setTimeout(() => setPwdMsg(null), 3000);
    },
    onError: (err: any) => {
      setPwdError(err.response?.data?.message || 'Failed to update password.');
    },
  });

  const handleUpdateProfile = (e: React.FormEvent) => {
    e.preventDefault();
    updateProfileMutation.mutate({ fullName, phone });
  };

  const handleChangePassword = (e: React.FormEvent) => {
    e.preventDefault();
    setPwdError(null);
    changePasswordMutation.mutate({ currentPassword, newPassword });
  };

  return (
    <div className="space-y-8 max-w-4xl">
      <div>
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider border border-indigo-500/20 mb-2">
          <User className="w-3.5 h-3.5" />
          Personal Account
        </div>
        <h1 className="text-3xl font-black font-display text-white">My Profile</h1>
        <p className="text-slate-400 text-sm">
          Manage your personal details, credentials, and identity within {tenant?.name}.
        </p>
      </div>

      {/* Identity & Org Card */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="glass-panel p-6 rounded-3xl border border-slate-800 flex items-center gap-4">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 flex items-center justify-center text-white font-black text-2xl shadow-xl shadow-indigo-500/25">
            {currentUser?.fullName?.charAt(0) || 'U'}
          </div>
          <div>
            <h2 className="text-xl font-bold text-white font-display">{currentUser?.fullName}</h2>
            <div className="text-xs text-indigo-400 font-mono flex items-center gap-2 mt-0.5">
              <span>Code: {currentUser?.employeeCode}</span>
              <span>•</span>
              <span>Role: {currentUser?.role}</span>
            </div>
            <div className="text-xs text-slate-500">{currentUser?.email}</div>
          </div>
        </div>

        {/* Organizational Assignment Card */}
        <div className="glass-panel p-6 rounded-3xl border border-slate-800 space-y-3">
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">Organizational Assignment</h3>
          <div className="grid grid-cols-3 gap-3 pt-1">
            <div className="p-3 rounded-2xl bg-slate-900/80 border border-slate-800">
              <div className="flex items-center gap-1.5 text-xs text-indigo-400 mb-1">
                <Building className="w-3.5 h-3.5" />
                <span className="font-semibold">Department</span>
              </div>
              <div className="text-sm font-bold text-white truncate">
                {currentDept ? currentDept.name : 'None'}
              </div>
            </div>

            <div className="p-3 rounded-2xl bg-slate-900/80 border border-slate-800">
              <div className="flex items-center gap-1.5 text-xs text-violet-400 mb-1">
                <Users2 className="w-3.5 h-3.5" />
                <span className="font-semibold">Team</span>
              </div>
              <div className="text-sm font-bold text-white truncate">
                {currentTeam ? currentTeam.name : 'None'}
              </div>
            </div>

            <div className="p-3 rounded-2xl bg-slate-900/80 border border-slate-800">
              <div className="flex items-center gap-1.5 text-xs text-emerald-400 mb-1">
                <UserCheck className="w-3.5 h-3.5" />
                <span className="font-semibold">Manager</span>
              </div>
              <div className="text-sm font-bold text-white truncate">
                {currentManager ? currentManager.fullName : 'None'}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* Profile Info Form */}
        <form onSubmit={handleUpdateProfile} className="glass-panel p-6 rounded-3xl border border-slate-800 space-y-4">
          <h3 className="text-base font-bold text-white font-display flex items-center gap-2">
            <User className="w-4 h-4 text-indigo-400" /> Profile Details
          </h3>

          {profileMsg && (
            <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4" />
              <span>{profileMsg}</span>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Full Name
            </label>
            <input
              type="text"
              required
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Phone Number
            </label>
            <input
              type="tel"
              placeholder="+1 555-0123"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <button
            type="submit"
            disabled={updateProfileMutation.isPending}
            className="w-full py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs shadow-lg shadow-indigo-600/25 transition-all"
          >
            Update Profile
          </button>
        </form>

        {/* Change Password Form */}
        <form onSubmit={handleChangePassword} className="glass-panel p-6 rounded-3xl border border-slate-800 space-y-4">
          <h3 className="text-base font-bold text-white font-display flex items-center gap-2">
            <Lock className="w-4 h-4 text-indigo-400" /> Security & Password
          </h3>

          {pwdMsg && (
            <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4" />
              <span>{pwdMsg}</span>
            </div>
          )}

          {pwdError && (
            <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4" />
              <span>{pwdError}</span>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Current Password *
            </label>
            <input
              type="password"
              required
              placeholder="••••••••"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              New Password * (8+ characters)
            </label>
            <input
              type="password"
              required
              minLength={8}
              placeholder="••••••••"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <button
            type="submit"
            disabled={changePasswordMutation.isPending}
            className="w-full py-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-bold text-xs border border-slate-700 transition-all"
          >
            Change Password
          </button>
        </form>
      </div>
    </div>
  );
};
