import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { settingsApi, emailConnectionApi } from '../../api/services';
import { Settings, Save, Building, ShieldCheck, CheckCircle2, Mail, Link2, Unlink, RefreshCw, AlertTriangle, Wifi, WifiOff } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';

export const SettingsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();

  const [orgName, setOrgName] = useState('');
  const [industry, setIndustry] = useState('');
  const [timezone, setTimezone] = useState('');
  const [workingDays, setWorkingDays] = useState('');
  const [savedSuccess, setSavedSuccess] = useState(false);
  const [emailConnectMessage, setEmailConnectMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Check for OAuth callback query params
  useEffect(() => {
    const connected = searchParams.get('email_connected');
    const email = searchParams.get('email');
    const error = searchParams.get('email_error');

    if (connected === 'true' && email) {
      setEmailConnectMessage({ type: 'success', text: `Gmail connected successfully: ${email}` });
      queryClient.invalidateQueries({ queryKey: ['email-connection-status'] });
      // Clean up query params
      searchParams.delete('email_connected');
      searchParams.delete('email');
      setSearchParams(searchParams, { replace: true });
    } else if (error) {
      setEmailConnectMessage({ type: 'error', text: `Gmail connection failed: ${error}` });
      searchParams.delete('email_error');
      setSearchParams(searchParams, { replace: true });
    }
  }, [searchParams]);

  // Auto-dismiss messages
  useEffect(() => {
    if (emailConnectMessage) {
      const timer = setTimeout(() => setEmailConnectMessage(null), 8000);
      return () => clearTimeout(timer);
    }
  }, [emailConnectMessage]);

  const { data: settingsData, isLoading } = useQuery({
    queryKey: ['org-settings'],
    queryFn: () => settingsApi.get(),
  });

  const { data: emailStatusData, isLoading: emailStatusLoading } = useQuery({
    queryKey: ['email-connection-status'],
    queryFn: () => emailConnectionApi.getStatus(),
    refetchInterval: 30000, // Refresh every 30s
  });

  useEffect(() => {
    if (settingsData?.data) {
      setOrgName(settingsData.data.organizationName || '');
      setIndustry(settingsData.data.industry || 'Technology');
      setTimezone(settingsData.data.timezone || 'UTC');
      setWorkingDays(settingsData.data.workingDays || 'MON,TUE,WED,THU,FRI');
    }
  }, [settingsData]);

  const updateMutation = useMutation({
    mutationFn: (payload: any) => settingsApi.update(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['org-settings'] });
      setSavedSuccess(true);
      setTimeout(() => setSavedSuccess(false), 3000);
    },
  });

  const connectGmailMutation = useMutation({
    mutationFn: () => emailConnectionApi.connectGmail(),
    onSuccess: (res) => {
      const authUrl = res?.data?.authUrl;
      if (authUrl) {
        window.location.href = authUrl;
      }
    },
    onError: (err: any) => {
      setEmailConnectMessage({
        type: 'error',
        text: err?.response?.data?.message || 'Failed to initiate Gmail connection.',
      });
    },
  });

  const disconnectMutation = useMutation({
    mutationFn: () => emailConnectionApi.disconnect(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['email-connection-status'] });
      setEmailConnectMessage({ type: 'success', text: 'Gmail disconnected successfully.' });
    },
    onError: (err: any) => {
      setEmailConnectMessage({
        type: 'error',
        text: err?.response?.data?.message || 'Failed to disconnect Gmail.',
      });
    },
  });

  const reconnectMutation = useMutation({
    mutationFn: () => emailConnectionApi.reconnect(),
    onSuccess: (res) => {
      const authUrl = res?.data?.authUrl;
      if (authUrl) {
        window.location.href = authUrl;
      }
    },
  });

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    updateMutation.mutate({
      organizationName: orgName,
      industry,
      timezone,
      workingDays,
    });
  };

  const emailStatus = emailStatusData?.data;
  const isConnected = emailStatus?.status === 'CONNECTED';
  const isError = emailStatus?.status === 'ERROR';
  const needsReauth = emailStatus?.status === 'REAUTH_REQUIRED';
  const isDisconnected = emailStatus?.status === 'DISCONNECTED' || emailStatus?.status === 'NOT_CONNECTED';

  const getStatusBadge = () => {
    if (isConnected) return (
      <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/15 text-emerald-400 text-xs font-bold border border-emerald-500/25">
        <Wifi className="w-3 h-3" /> Connected
      </span>
    );
    if (isError) return (
      <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-red-500/15 text-red-400 text-xs font-bold border border-red-500/25">
        <AlertTriangle className="w-3 h-3" /> Error
      </span>
    );
    if (needsReauth) return (
      <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-500/15 text-amber-400 text-xs font-bold border border-amber-500/25">
        <RefreshCw className="w-3 h-3" /> Reauthorize Required
      </span>
    );
    return (
      <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-slate-500/15 text-slate-400 text-xs font-bold border border-slate-500/25">
        <WifiOff className="w-3 h-3" /> Not Connected
      </span>
    );
  };

  return (
    <div className="space-y-8 max-w-4xl">
      <div>
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider border border-indigo-500/20 mb-2">
          <Settings className="w-3.5 h-3.5" />
          Tenant Configuration
        </div>
        <h1 className="text-3xl font-black font-display text-white">Organization Settings</h1>
        <p className="text-slate-400 text-sm">
          Configure tenant profile, email sender, and operational defaults.
        </p>
      </div>

      {savedSuccess && (
        <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-sm flex items-center gap-3">
          <CheckCircle2 className="w-5 h-5 flex-shrink-0" />
          <span>Organization settings updated successfully.</span>
        </div>
      )}

      {emailConnectMessage && (
        <div className={`p-4 rounded-2xl text-sm flex items-center gap-3 ${
          emailConnectMessage.type === 'success'
            ? 'bg-emerald-500/10 border border-emerald-500/20 text-emerald-400'
            : 'bg-red-500/10 border border-red-500/20 text-red-400'
        }`}>
          {emailConnectMessage.type === 'success' ? <CheckCircle2 className="w-5 h-5 flex-shrink-0" /> : <AlertTriangle className="w-5 h-5 flex-shrink-0" />}
          <span>{emailConnectMessage.text}</span>
        </div>
      )}

      {/* ── Email Sender Connection ── */}
      <div className="glass-panel p-8 rounded-3xl border border-slate-800 space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <Mail className="w-5 h-5 text-indigo-400" />
              <h2 className="text-xl font-bold text-white">Email Sender</h2>
            </div>
            <p className="text-slate-400 text-sm">
              Connect your Gmail account to send invitation emails directly from your authorized email address.
            </p>
          </div>
          {getStatusBadge()}
        </div>

        <div className="border-t border-slate-800 pt-6">
          {isConnected && emailStatus?.emailAddress && (
            <div className="space-y-4">
              <div className="flex items-center gap-4 p-4 rounded-2xl bg-slate-900/60 border border-slate-700/50">
                <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white font-bold text-lg shadow-lg shadow-indigo-500/20">
                  {emailStatus.emailAddress.charAt(0).toUpperCase()}
                </div>
                <div className="flex-1">
                  <div className="text-white font-semibold text-sm">{emailStatus.emailAddress}</div>
                  <div className="text-slate-400 text-xs">
                    Gmail • OAuth 2.0 Authorized
                    {emailStatus.lastSendAt && (
                      <span className="ml-2">
                        • Last sent: {new Date(emailStatus.lastSendAt).toLocaleDateString()}
                      </span>
                    )}
                  </div>
                </div>
              </div>

              <div className="flex gap-3">
                <button
                  onClick={() => reconnectMutation.mutate()}
                  disabled={reconnectMutation.isPending}
                  className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold flex items-center gap-2 transition-all border border-slate-700 disabled:opacity-50"
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${reconnectMutation.isPending ? 'animate-spin' : ''}`} />
                  Reauthorize
                </button>
                <button
                  onClick={() => {
                    if (window.confirm('Are you sure you want to disconnect your Gmail? Invitation emails will use the fallback SMTP configuration.')) {
                      disconnectMutation.mutate();
                    }
                  }}
                  disabled={disconnectMutation.isPending}
                  className="px-4 py-2 rounded-xl bg-red-500/10 hover:bg-red-500/20 text-red-400 text-xs font-semibold flex items-center gap-2 transition-all border border-red-500/20 disabled:opacity-50"
                >
                  <Unlink className="w-3.5 h-3.5" />
                  Disconnect
                </button>
              </div>
            </div>
          )}

          {(isError || needsReauth) && (
            <div className="space-y-4">
              {emailStatus?.errorMessage && (
                <div className="p-3 rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 text-xs">
                  {emailStatus.errorMessage}
                </div>
              )}
              <button
                onClick={() => reconnectMutation.mutate()}
                disabled={reconnectMutation.isPending}
                className="px-5 py-3 rounded-xl bg-amber-600 hover:bg-amber-500 text-white font-bold text-xs shadow-lg shadow-amber-600/25 flex items-center gap-2 transition-all disabled:opacity-50"
              >
                <RefreshCw className={`w-4 h-4 ${reconnectMutation.isPending ? 'animate-spin' : ''}`} />
                Reconnect Gmail
              </button>
            </div>
          )}

          {isDisconnected && (
            <div className="space-y-4">
              <div className="p-4 rounded-2xl bg-slate-900/40 border border-slate-700/30">
                <div className="flex items-start gap-3">
                  <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-red-500 to-orange-500 flex items-center justify-center text-white shadow-lg flex-shrink-0">
                    <svg viewBox="0 0 24 24" className="w-5 h-5" fill="currentColor">
                      <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"/>
                      <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                      <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                      <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                    </svg>
                  </div>
                  <div>
                    <h3 className="text-white font-semibold text-sm">Connect Gmail</h3>
                    <p className="text-slate-400 text-xs mt-1">
                      Authorize your Google account to send invitation emails directly from your Gmail address.
                      Your credentials are encrypted and never stored as plain text.
                    </p>
                  </div>
                </div>
              </div>

              <button
                onClick={() => connectGmailMutation.mutate()}
                disabled={connectGmailMutation.isPending}
                className="px-6 py-3 rounded-xl bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white font-bold text-xs shadow-lg shadow-indigo-600/25 flex items-center gap-2 transition-all disabled:opacity-50"
              >
                <Link2 className={`w-4 h-4 ${connectGmailMutation.isPending ? 'animate-spin' : ''}`} />
                Connect Gmail Account
              </button>
            </div>
          )}
        </div>
      </div>

      {/* ── Organization Settings ── */}
      <form onSubmit={handleSave} className="glass-panel p-8 rounded-3xl border border-slate-800 space-y-6">
        <div>
          <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
            Organization Display Name
          </label>
          <input
            type="text"
            required
            value={orgName}
            onChange={(e) => setOrgName(e.target.value)}
            className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Industry
            </label>
            <input
              type="text"
              value={industry}
              onChange={(e) => setIndustry(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Default Timezone
            </label>
            <select
              value={timezone}
              onChange={(e) => setTimezone(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            >
              <option value="UTC">UTC (Universal)</option>
              <option value="America/New_York">Eastern Time (US & Canada)</option>
              <option value="America/Los_Angeles">Pacific Time (US & Canada)</option>
              <option value="Europe/London">London (GMT/BST)</option>
              <option value="Asia/Kolkata">India (IST)</option>
              <option value="Asia/Tokyo">Tokyo (JST)</option>
            </select>
          </div>
        </div>

        <div>
          <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
            Standard Working Days (comma-separated)
          </label>
          <input
            type="text"
            value={workingDays}
            onChange={(e) => setWorkingDays(e.target.value)}
            className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm font-mono"
          />
        </div>

        <div className="pt-4 border-t border-slate-800 flex justify-end">
          <button
            type="submit"
            disabled={updateMutation.isPending}
            className="px-6 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs shadow-lg shadow-indigo-600/25 flex items-center gap-2 transition-all disabled:opacity-50"
          >
            <Save className="w-4 h-4" />
            <span>Save Settings</span>
          </button>
        </div>
      </form>
    </div>
  );
};
