import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { integrationApi } from '../../api/services';
import { useAuthStore } from '../../store/authStore';
import { Modal } from '../../components/common/Modal';
import {
  GitBranch,
  Github,
  Gitlab,
  CheckCircle,
  Plus,
  Trash2,
  Plug,
  ExternalLink,
  Shield,
  Layers,
  RefreshCw,
  AlertCircle,
  Send,
  MessageSquare,
  FileCode,
  FolderGit2,
  GitPullRequest,
  CheckSquare,
  Globe,
  Radio,
  Clock,
} from 'lucide-react';

export const IntegrationsPage: React.FC = () => {
  const [connectModalOpen, setConnectModalOpen] = useState(false);
  const [explorerModalOpen, setExplorerModalOpen] = useState(false);
  const [selectedProvider, setSelectedProvider] = useState<'GITHUB' | 'GITLAB' | 'JIRA' | 'SLACK'>('JIRA');
  const [activeIntegration, setActiveIntegration] = useState<any | null>(null);
  const [explorerTab, setExplorerTab] = useState<string>('default');

  // Connect Form State
  const [instanceUrl, setInstanceUrl] = useState('');
  const [externalUsername, setExternalUsername] = useState('');
  const [accessToken, setAccessToken] = useState('');
  const [defaultChannel, setDefaultChannel] = useState('general');
  const [connectError, setConnectError] = useState<string | null>(null);

  // Slack Message Form State
  const [slackTargetChannel, setSlackTargetChannel] = useState('');
  const [slackMessageText, setSlackMessageText] = useState('Hello from WorkHive!');
  const [slackSendResult, setSlackSendResult] = useState<string | null>(null);

  const [syncingId, setSyncingId] = useState<string | null>(null);

  const { user: currentUser } = useAuthStore();
  const queryClient = useQueryClient();

  const { data: integrationsData, isLoading } = useQuery({
    queryKey: ['integrations-list'],
    queryFn: () => integrationApi.getAll(),
  });

  // Queries for Remote Explorer Data
  const { data: jiraProjectsData, isLoading: jiraProjectsLoading } = useQuery({
    queryKey: ['jira-projects', activeIntegration?.id],
    queryFn: () => integrationApi.getJiraProjects(activeIntegration.id),
    enabled: !!activeIntegration && activeIntegration.provider === 'JIRA' && explorerModalOpen,
  });

  const { data: jiraIssuesData, isLoading: jiraIssuesLoading } = useQuery({
    queryKey: ['jira-issues', activeIntegration?.id],
    queryFn: () => integrationApi.getJiraIssues(activeIntegration.id),
    enabled: !!activeIntegration && activeIntegration.provider === 'JIRA' && explorerModalOpen,
  });

  const { data: gitlabProjectsData, isLoading: gitlabProjectsLoading } = useQuery({
    queryKey: ['gitlab-projects', activeIntegration?.id],
    queryFn: () => integrationApi.getGitLabProjects(activeIntegration.id),
    enabled: !!activeIntegration && activeIntegration.provider === 'GITLAB' && explorerModalOpen,
  });

  const { data: slackChannelsData, isLoading: slackChannelsLoading } = useQuery({
    queryKey: ['slack-channels', activeIntegration?.id],
    queryFn: () => integrationApi.getSlackChannels(activeIntegration.id),
    enabled: !!activeIntegration && activeIntegration.provider === 'SLACK' && explorerModalOpen,
  });

  const { data: githubReposData, isLoading: githubReposLoading } = useQuery({
    queryKey: ['github-repos', activeIntegration?.id],
    queryFn: () => integrationApi.getGitHubRepositories(activeIntegration.id),
    enabled: !!activeIntegration && activeIntegration.provider === 'GITHUB' && explorerModalOpen,
  });

  const { data: githubOverviewData, isLoading: githubOverviewLoading } = useQuery({
    queryKey: ['github-overview', activeIntegration?.id],
    queryFn: () => integrationApi.getGitHubOverview(activeIntegration.id),
    enabled: !!activeIntegration && activeIntegration.provider === 'GITHUB' && explorerModalOpen,
  });

  const { data: githubCommitsData, isLoading: githubCommitsLoading } = useQuery({
    queryKey: ['github-commits', activeIntegration?.id],
    queryFn: () => integrationApi.getGitHubCommits(activeIntegration.id),
    enabled: !!activeIntegration && activeIntegration.provider === 'GITHUB' && explorerModalOpen,
  });

  const { data: githubPrsData, isLoading: githubPrsLoading } = useQuery({
    queryKey: ['github-prs', activeIntegration?.id],
    queryFn: () => integrationApi.getGitHubPullRequests(activeIntegration.id),
    enabled: !!activeIntegration && activeIntegration.provider === 'GITHUB' && explorerModalOpen,
  });

  const { data: githubIssuesData, isLoading: githubIssuesLoading } = useQuery({
    queryKey: ['github-issues', activeIntegration?.id],
    queryFn: () => integrationApi.getGitHubIssues(activeIntegration.id),
    enabled: !!activeIntegration && activeIntegration.provider === 'GITHUB' && explorerModalOpen,
  });

  const connectMutation = useMutation({
    mutationFn: (payload: any) => integrationApi.connect(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['integrations-list'] });
      setConnectModalOpen(false);
      resetForm();
    },
    onError: (err: any) => {
      setConnectError(err.response?.data?.message || 'Failed to authenticate integration');
    },
  });

  const disconnectMutation = useMutation({
    mutationFn: (id: string) => integrationApi.disconnect(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['integrations-list'] });
    },
  });

  const syncMutation = useMutation({
    mutationFn: (id: string) => integrationApi.sync(id),
    onSuccess: (res: any) => {
      queryClient.invalidateQueries({ queryKey: ['integrations-list'] });
      queryClient.invalidateQueries({ queryKey: ['activities-recent'] });
      alert(`Synchronized ${res.data?.itemsSynced || 1} items from ${res.data?.provider || 'provider'}`);
      setSyncingId(null);
    },
    onError: (err: any) => {
      alert(`Sync failed: ${err.response?.data?.message || err.message}`);
      setSyncingId(null);
    },
  });

  const sendSlackMessageMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: any }) => integrationApi.postSlackMessage(id, data),
    onSuccess: (res: any) => {
      setSlackSendResult('Message dispatched successfully to Slack!');
      setTimeout(() => setSlackSendResult(null), 3000);
    },
    onError: (err: any) => {
      setSlackSendResult(`Error: ${err.response?.data?.message || 'Failed to send'}`);
    },
  });

  const resetForm = () => {
    setInstanceUrl('');
    setExternalUsername('');
    setAccessToken('');
    setDefaultChannel('general');
    setConnectError(null);
  };

  const handleOpenConnect = (provider: 'GITHUB' | 'GITLAB' | 'JIRA' | 'SLACK') => {
    setSelectedProvider(provider);
    resetForm();
    if (provider === 'JIRA') {
      setInstanceUrl('https://');
    } else if (provider === 'GITLAB') {
      setInstanceUrl('https://gitlab.com');
    }
    setConnectModalOpen(true);
  };

  const handleConnectSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setConnectError(null);
    connectMutation.mutate({
      provider: selectedProvider,
      accessToken,
      instanceUrl: instanceUrl.trim(),
      externalUsername: externalUsername.trim(),
      defaultChannel,
    });
  };

  const handleSync = (integration: any) => {
    setSyncingId(integration.id);
    syncMutation.mutate(integration.id);
  };

  const handleOpenExplorer = (integration: any) => {
    setActiveIntegration(integration);
    if (integration.provider === 'JIRA') setExplorerTab('projects');
    else if (integration.provider === 'GITLAB') setExplorerTab('repos');
    else if (integration.provider === 'SLACK') setExplorerTab('channels');
    else if (integration.provider === 'GITHUB') setExplorerTab('repos');
    else setExplorerTab('default');
    setExplorerModalOpen(true);
  };

  const integrations = integrationsData?.data || [];

  const providers = [
    {
      id: 'JIRA' as const,
      name: 'Jira Software',
      icon: Layers,
      color: 'from-blue-500/20 to-indigo-600/10 text-blue-400 border-blue-500/30',
      desc: 'Synchronize Jira Epics, Stories, and active Sprints directly into WorkHive project tasks and activity signals.',
    },
    {
      id: 'GITLAB' as const,
      name: 'GitLab',
      icon: Gitlab,
      color: 'from-orange-500/20 to-amber-600/10 text-orange-400 border-orange-500/30',
      desc: 'Connect GitLab repositories, track Merge Requests, commits, pipelines, and developer contributions.',
    },
    {
      id: 'SLACK' as const,
      name: 'Slack',
      icon: Plug,
      color: 'from-emerald-500/20 to-teal-600/10 text-emerald-400 border-emerald-500/30',
      desc: 'Dispatch automated notifications, Action Center approvals, and project alerts directly to Slack channels.',
    },
    {
      id: 'GITHUB' as const,
      name: 'GitHub',
      icon: Github,
      color: 'from-purple-500/20 to-indigo-600/10 text-purple-400 border-purple-500/30',
      desc: 'Sync commits, pull requests, issues, and developer activities automatically into WorkHive.',
    },
  ];

  return (
    <div className="space-y-8">
      <div>
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider border border-indigo-500/20 mb-2">
          <GitBranch className="w-3.5 h-3.5" />
          Integration Hub
        </div>
        <h1 className="text-3xl font-black font-display text-white">External Integrations & APIs</h1>
        <p className="text-slate-400 text-sm">
          Connect external development and collaboration platforms. Live statuses, synchronization, and automated activity normalization.
        </p>
      </div>

      {/* Provider Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {providers.map((prov) => {
          const Icon = prov.icon;
          const conn = integrations.find(
            (i: any) => i.provider === prov.id && i.connectedBy === currentUser?.id && i.status !== 'DISCONNECTED'
          );
          const isConnected = conn && conn.status === 'CONNECTED';
          const isError = conn && conn.status === 'ERROR';

          return (
            <div
              key={prov.id}
              className="glass-panel p-6 rounded-3xl border border-slate-800 flex flex-col justify-between"
            >
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <div className={`p-3 rounded-2xl bg-gradient-to-br ${prov.color} border`}>
                    <Icon className="w-6 h-6" />
                  </div>

                  {isConnected ? (
                    <span className="px-3 py-1 rounded-full text-xs font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center gap-1.5">
                      <CheckCircle className="w-3.5 h-3.5" /> Connected
                    </span>
                  ) : isError ? (
                    <span className="px-3 py-1 rounded-full text-xs font-bold bg-rose-500/10 text-rose-400 border border-rose-500/20 flex items-center gap-1.5">
                      <AlertCircle className="w-3.5 h-3.5" /> Connection Error
                    </span>
                  ) : (
                    <span className="px-3 py-1 rounded-full text-xs font-bold bg-slate-800 text-slate-400">
                      Not Connected
                    </span>
                  )}
                </div>

                <div>
                  <h3 className="text-xl font-bold text-white font-display">{prov.name}</h3>
                  <p className="text-xs text-slate-400 mt-1">{prov.desc}</p>
                </div>

                {conn && (
                  <div className="p-3.5 rounded-2xl bg-slate-900/80 border border-slate-800 space-y-1.5 text-xs">
                    <div className="flex items-center justify-between">
                      <span className="text-slate-500">Identity:</span>
                      <span className="font-semibold text-indigo-300 truncate max-w-[200px]">
                        {conn.externalUsername || 'Connected'}
                      </span>
                    </div>
                    {conn.externalUserId && (
                      <div className="flex items-center justify-between">
                        <span className="text-slate-500">Domain / URL:</span>
                        <span className="font-mono text-[11px] text-slate-400 truncate max-w-[200px]">
                          {conn.externalUserId}
                        </span>
                      </div>
                    )}
                    <div className="flex items-center justify-between">
                      <span className="text-slate-500">Last Synced:</span>
                      <span className="text-slate-400 font-mono text-[11px]">
                        {conn.lastSyncAt ? new Date(conn.lastSyncAt).toLocaleTimeString() : 'Pending'}
                      </span>
                    </div>
                    {conn.syncError && (
                      <div className="text-[11px] text-rose-400 font-mono pt-1 border-t border-slate-800">
                        {conn.syncError}
                      </div>
                    )}
                  </div>
                )}
              </div>

              <div className="mt-6 pt-4 border-t border-slate-800 flex flex-wrap items-center justify-between gap-2">
                {isConnected || isError ? (
                  <div className="flex items-center gap-2 w-full justify-between">
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => handleSync(conn)}
                        disabled={syncingId === conn.id}
                        className="px-3.5 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold border border-slate-700 flex items-center gap-1.5 transition-all disabled:opacity-50"
                      >
                        <RefreshCw className={`w-3.5 h-3.5 text-indigo-400 ${syncingId === conn.id ? 'animate-spin' : ''}`} />
                        <span>{syncingId === conn.id ? 'Syncing...' : 'Sync Now'}</span>
                      </button>

                      <button
                        onClick={() => handleOpenExplorer(conn)}
                        className="px-3.5 py-1.5 rounded-xl bg-indigo-600/15 hover:bg-indigo-600/30 text-indigo-300 text-xs font-bold transition-colors flex items-center gap-1.5"
                      >
                        <ExternalLink className="w-3.5 h-3.5" />
                        <span>View Data</span>
                      </button>
                    </div>

                    <button
                      onClick={() => {
                        if (window.confirm(`Disconnect ${prov.name} integration?`)) {
                          disconnectMutation.mutate(conn.id);
                        }
                      }}
                      className="p-2 rounded-xl text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
                      title="Disconnect integration"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                ) : (
                  <button
                    onClick={() => handleOpenConnect(prov.id)}
                    className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs shadow-lg shadow-indigo-600/25 transition-all flex items-center gap-2"
                  >
                    <Plus className="w-4 h-4" />
                    <span>Connect {prov.name}</span>
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Connect Integration Modal */}
      <Modal
        isOpen={connectModalOpen}
        onClose={() => setConnectModalOpen(false)}
        title={`Connect ${selectedProvider} Integration`}
      >
        <form onSubmit={handleConnectSubmit} className="space-y-4">
          {connectError && (
            <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{connectError}</span>
            </div>
          )}

          {/* JIRA SPECIFIC FIELDS */}
          {selectedProvider === 'JIRA' && (
            <>
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                  Jira Workspace Domain / URL *
                </label>
                <input
                  type="url"
                  required
                  placeholder="https://your-company.atlassian.net"
                  value={instanceUrl}
                  onChange={(e) => setInstanceUrl(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
                />
                <span className="text-[10px] text-slate-500 mt-1 block">Your Atlassian Cloud or Jira Server URL</span>
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                  Jira Account Email *
                </label>
                <input
                  type="email"
                  required
                  placeholder="admin@company.com"
                  value={externalUsername}
                  onChange={(e) => setExternalUsername(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                  Jira API Token *
                </label>
                <input
                  type="password"
                  required
                  placeholder="ATATT3xFfGF0..."
                  value={accessToken}
                  onChange={(e) => setAccessToken(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm font-mono"
                />
                <span className="text-[10px] text-slate-500 mt-1 block">Generated in Atlassian Account Settings &gt; Security &gt; API Tokens</span>
              </div>
            </>
          )}

          {/* GITLAB SPECIFIC FIELDS */}
          {selectedProvider === 'GITLAB' && (
            <>
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                  GitLab Instance URL *
                </label>
                <input
                  type="url"
                  required
                  placeholder="https://gitlab.com"
                  value={instanceUrl}
                  onChange={(e) => setInstanceUrl(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                  GitLab Username (Optional)
                </label>
                <input
                  type="text"
                  placeholder="gitlab-username"
                  value={externalUsername}
                  onChange={(e) => setExternalUsername(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                  Personal Access Token (PAT) *
                </label>
                <input
                  type="password"
                  required
                  placeholder="glpat-xxxxxxxxxxxxxxxxxxxx"
                  value={accessToken}
                  onChange={(e) => setAccessToken(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm font-mono"
                />
                <span className="text-[10px] text-slate-500 mt-1 block">Scopes required: api, read_user, read_repository</span>
              </div>
            </>
          )}

          {/* SLACK SPECIFIC FIELDS */}
          {selectedProvider === 'SLACK' && (
            <>
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                  Slack Bot User OAuth Token (xoxb-...) or Webhook *
                </label>
                <input
                  type="password"
                  required
                  placeholder="xoxb-1234567890-..."
                  value={accessToken}
                  onChange={(e) => setAccessToken(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm font-mono"
                />
                <span className="text-[10px] text-slate-500 mt-1 block">Found in your Slack App &gt; OAuth &amp; Permissions &gt; Bot User Token</span>
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                  Workspace / Team Name
                </label>
                <input
                  type="text"
                  placeholder="e.g. Acme Engineering"
                  value={externalUsername}
                  onChange={(e) => setExternalUsername(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
                />
              </div>
            </>
          )}

          {/* GITHUB SPECIFIC FIELDS */}
          {selectedProvider === 'GITHUB' && (
            <>
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                  GitHub Account Username
                </label>
                <input
                  type="text"
                  placeholder="e.g. octocat"
                  value={externalUsername}
                  onChange={(e) => setExternalUsername(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                  Personal Access Token (PAT) *
                </label>
                <input
                  type="password"
                  required
                  placeholder="ghp_xxxxxxxxxxxxxxxxxxxx"
                  value={accessToken}
                  onChange={(e) => setAccessToken(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm font-mono"
                />
              </div>
            </>
          )}

          <div className="flex justify-end gap-3 pt-3">
            <button
              type="button"
              onClick={() => setConnectModalOpen(false)}
              className="px-4 py-2.5 rounded-xl bg-slate-800 text-slate-300 text-xs font-bold"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={connectMutation.isPending}
              className="px-5 py-2.5 rounded-xl bg-indigo-600 text-white text-xs font-bold shadow-lg shadow-indigo-600/25 disabled:opacity-50"
            >
              {connectMutation.isPending ? 'Verifying & Connecting...' : 'Authorize & Connect'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Remote Data Explorer Modal */}
      {activeIntegration && (
        <Modal
          isOpen={explorerModalOpen}
          onClose={() => setExplorerModalOpen(false)}
          title={`Remote Data Explorer — ${activeIntegration.provider}`}
        >
          <div className="space-y-6">
            {/* JIRA EXPLORER */}
            {activeIntegration.provider === 'JIRA' && (
              <div className="space-y-4">
                <div className="flex items-center gap-2 border-b border-slate-800 pb-2">
                  <button
                    onClick={() => setExplorerTab('projects')}
                    className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all ${
                      explorerTab === 'projects' ? 'bg-indigo-600 text-white' : 'text-slate-400 hover:text-white hover:bg-slate-800'
                    }`}
                  >
                    Projects ({(jiraProjectsData?.data || []).length})
                  </button>
                  <button
                    onClick={() => setExplorerTab('issues')}
                    className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all ${
                      explorerTab === 'issues' ? 'bg-indigo-600 text-white' : 'text-slate-400 hover:text-white hover:bg-slate-800'
                    }`}
                  >
                    Issues ({(jiraIssuesData?.data || []).length})
                  </button>
                </div>

                {explorerTab === 'projects' && (
                  <div className="space-y-3 max-h-72 overflow-y-auto">
                    {jiraProjectsLoading ? (
                      <div className="text-center py-8 text-xs text-slate-500">Fetching Jira projects...</div>
                    ) : (jiraProjectsData?.data || []).length === 0 ? (
                      <div className="text-center py-8 text-xs text-slate-500">No Jira projects found in this workspace.</div>
                    ) : (
                      (jiraProjectsData?.data || []).map((p: any) => (
                        <div key={p.id} className="p-3.5 rounded-2xl bg-slate-900/80 border border-slate-800 flex items-center justify-between">
                          <div>
                            <div className="text-xs font-bold text-white flex items-center gap-2">
                              <span className="font-mono px-1.5 py-0.5 rounded bg-indigo-500/10 text-indigo-400 text-[10px]">{p.key}</span>
                              <span>{p.name}</span>
                            </div>
                            <div className="text-[10px] text-slate-400 mt-0.5">Lead: {p.lead || 'Admin'} • Type: {p.projectTypeKey}</div>
                          </div>
                          <a href={p.url} target="_blank" rel="noreferrer" className="p-1.5 rounded-lg text-slate-400 hover:text-indigo-400 hover:bg-slate-800">
                            <ExternalLink className="w-3.5 h-3.5" />
                          </a>
                        </div>
                      ))
                    )}
                  </div>
                )}

                {explorerTab === 'issues' && (
                  <div className="space-y-3 max-h-72 overflow-y-auto">
                    {jiraIssuesLoading ? (
                      <div className="text-center py-8 text-xs text-slate-500">Fetching Jira issues...</div>
                    ) : (jiraIssuesData?.data || []).length === 0 ? (
                      <div className="text-center py-8 text-xs text-slate-500">No Jira issues found.</div>
                    ) : (
                      (jiraIssuesData?.data || []).map((iss: any) => (
                        <div key={iss.id} className="p-3.5 rounded-2xl bg-slate-900/80 border border-slate-800 flex items-center justify-between">
                          <div className="space-y-1">
                            <div className="text-xs font-bold text-white flex items-center gap-2">
                              <span className="font-mono text-[10px] text-indigo-400">{iss.key}</span>
                              <span className="truncate max-w-[280px]">{iss.summary}</span>
                            </div>
                            <div className="text-[10px] text-slate-400">
                              Assignee: <span className="text-slate-300">{iss.assigneeName}</span> • Priority: <span className="text-amber-400">{iss.priority}</span>
                            </div>
                          </div>
                          <span className="text-[10px] px-2 py-0.5 rounded font-bold bg-slate-800 text-slate-300 border border-slate-700">
                            {iss.status}
                          </span>
                        </div>
                      ))
                    )}
                  </div>
                )}
              </div>
            )}

            {/* GITLAB EXPLORER */}
            {activeIntegration.provider === 'GITLAB' && (
              <div className="space-y-4">
                <div className="space-y-3 max-h-72 overflow-y-auto">
                  {gitlabProjectsLoading ? (
                    <div className="text-center py-8 text-xs text-slate-500">Fetching GitLab repositories...</div>
                  ) : (gitlabProjectsData?.data || []).length === 0 ? (
                    <div className="text-center py-8 text-xs text-slate-500">No GitLab projects found for this account.</div>
                  ) : (
                    (gitlabProjectsData?.data || []).map((repo: any) => (
                      <div key={repo.id} className="p-3.5 rounded-2xl bg-slate-900/80 border border-slate-800 flex items-center justify-between">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5">
                            <FolderGit2 className="w-3.5 h-3.5 text-orange-400" />
                            <span>{repo.pathWithNamespace || repo.name}</span>
                          </div>
                          <div className="text-[10px] text-slate-400 mt-0.5">
                            Branch: <span className="font-mono text-indigo-300">{repo.defaultBranch}</span> • Visibility: {repo.visibility}
                          </div>
                        </div>
                        <a href={repo.webUrl} target="_blank" rel="noreferrer" className="p-1.5 rounded-lg text-slate-400 hover:text-orange-400 hover:bg-slate-800">
                          <ExternalLink className="w-3.5 h-3.5" />
                        </a>
                      </div>
                    ))
                  )}
                </div>
              </div>
            )}

            {/* GITHUB DATA HUB EXPLORER */}
            {activeIntegration.provider === 'GITHUB' && (
              <div className="space-y-4">
                {/* Navigation Tabs */}
                <div className="flex items-center gap-2 border-b border-slate-800 pb-2 overflow-x-auto">
                  {[
                    { id: 'overview', label: 'Overview' },
                    { id: 'repos', label: `Repositories (${(githubReposData?.data || []).length})` },
                    { id: 'prs', label: `Pull Requests (${(githubPrsData?.data || []).length})` },
                    { id: 'issues', label: `Issues (${(githubIssuesData?.data || []).length})` },
                    { id: 'commits', label: `Activity (${(githubCommitsData?.data || []).length})` },
                  ].map((tab) => (
                    <button
                      key={tab.id}
                      onClick={() => setExplorerTab(tab.id)}
                      className={`px-3 py-1.5 rounded-lg text-xs font-bold whitespace-nowrap transition-all ${
                        (explorerTab === 'default' ? 'overview' : explorerTab) === tab.id
                          ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/25'
                          : 'text-slate-400 hover:text-white hover:bg-slate-800'
                      }`}
                    >
                      {tab.label}
                    </button>
                  ))}
                </div>

                {/* Tab: Overview */}
                {(explorerTab === 'default' || explorerTab === 'overview') && (
                  <div className="space-y-4">
                    {/* Stats Grid */}
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                      <div className="p-3 rounded-2xl bg-slate-900/90 border border-slate-800 text-center">
                        <div className="text-xl font-bold font-mono text-white">
                          {githubOverviewData?.data?.repositoriesCount || (githubReposData?.data || []).length}
                        </div>
                        <div className="text-[10px] uppercase font-bold text-slate-400 mt-0.5">Repositories</div>
                      </div>
                      <div className="p-3 rounded-2xl bg-slate-900/90 border border-slate-800 text-center">
                        <div className="text-xl font-bold font-mono text-indigo-400">
                          {githubOverviewData?.data?.pullRequestsCount || (githubPrsData?.data || []).length}
                        </div>
                        <div className="text-[10px] uppercase font-bold text-slate-400 mt-0.5">Pull Requests</div>
                      </div>
                      <div className="p-3 rounded-2xl bg-slate-900/90 border border-slate-800 text-center">
                        <div className="text-xl font-bold font-mono text-amber-400">
                          {githubOverviewData?.data?.issuesCount || (githubIssuesData?.data || []).length}
                        </div>
                        <div className="text-[10px] uppercase font-bold text-slate-400 mt-0.5">Issues</div>
                      </div>
                      <div className="p-3 rounded-2xl bg-slate-900/90 border border-slate-800 text-center">
                        <div className="text-xl font-bold font-mono text-cyan-400">
                          {githubOverviewData?.data?.commitsCount || (githubCommitsData?.data || []).length}
                        </div>
                        <div className="text-[10px] uppercase font-bold text-slate-400 mt-0.5">Recent Commits</div>
                      </div>
                    </div>

                    {/* Account Info Card */}
                    <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800 space-y-2">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center text-purple-400 font-bold">
                            <Github className="w-5 h-5" />
                          </div>
                          <div>
                            <div className="text-sm font-bold text-white flex items-center gap-2">
                              <span>@{activeIntegration.externalUsername || 'github-account'}</span>
                              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                                {activeIntegration.status}
                              </span>
                            </div>
                            <div className="text-[11px] text-slate-400 font-mono">
                              Scopes: {activeIntegration.scopes || 'repo, read:user'}
                            </div>
                          </div>
                        </div>

                        <a
                          href={activeIntegration.externalUserId || 'https://github.com'}
                          target="_blank"
                          rel="noreferrer"
                          className="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-indigo-300 text-xs font-bold flex items-center gap-1.5"
                        >
                          <span>GitHub Profile</span>
                          <ExternalLink className="w-3.5 h-3.5" />
                        </a>
                      </div>

                      <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-400">
                        <span className="flex items-center gap-1.5">
                          <Shield className="w-3.5 h-3.5 text-emerald-400" /> Webhook: <strong className="text-slate-200">ACTIVE (HMAC-SHA256)</strong>
                        </span>
                        <span className="text-[11px] text-slate-500">
                          Last Sync: {activeIntegration.lastSyncAt ? new Date(activeIntegration.lastSyncAt).toLocaleString() : 'Just now'}
                        </span>
                      </div>
                    </div>
                  </div>
                )}

                {/* Tab: Repositories */}
                {explorerTab === 'repos' && (
                  <div className="space-y-3 max-h-72 overflow-y-auto">
                    {githubReposLoading ? (
                      <div className="text-center py-8 text-xs text-slate-500">Fetching GitHub repositories...</div>
                    ) : (githubReposData?.data || []).length === 0 ? (
                      <div className="text-center py-8 text-xs text-slate-500">No GitHub repositories found.</div>
                    ) : (
                      (githubReposData?.data || []).map((repo: any) => (
                        <div key={repo.id} className="p-3.5 rounded-2xl bg-slate-900/80 border border-slate-800 flex items-center justify-between">
                          <div>
                            <div className="text-xs font-bold text-white flex items-center gap-1.5">
                              <Github className="w-3.5 h-3.5 text-purple-400" />
                              <span>{repo.fullName || repo.name}</span>
                              {repo.private && <span className="text-[10px] px-1.5 py-0.2 rounded bg-slate-800 text-slate-400 ml-1">Private</span>}
                            </div>
                            <div className="text-[10px] text-slate-400 mt-0.5">
                              Default Branch: <span className="font-mono text-indigo-300">{repo.defaultBranch || 'main'}</span> • Owner: {repo.owner || activeIntegration.externalUsername || 'GitHub'}
                            </div>
                          </div>
                          <a href={repo.htmlUrl} target="_blank" rel="noreferrer" className="p-1.5 rounded-lg text-slate-400 hover:text-purple-400 hover:bg-slate-800">
                            <ExternalLink className="w-3.5 h-3.5" />
                          </a>
                        </div>
                      ))
                    )}
                  </div>
                )}

                {/* Tab: Pull Requests */}
                {explorerTab === 'prs' && (
                  <div className="space-y-3 max-h-72 overflow-y-auto">
                    {githubPrsLoading ? (
                      <div className="text-center py-8 text-xs text-slate-500">Fetching Pull Requests...</div>
                    ) : (githubPrsData?.data || []).length === 0 ? (
                      <div className="text-center py-8 text-xs text-slate-500">No Pull Requests found.</div>
                    ) : (
                      (githubPrsData?.data || []).map((pr: any) => (
                        <div key={pr.number} className="p-3.5 rounded-2xl bg-slate-900/80 border border-slate-800 flex items-center justify-between">
                          <div className="space-y-1">
                            <div className="text-xs font-bold text-white flex items-center gap-2">
                              <GitPullRequest className={`w-3.5 h-3.5 ${pr.state === 'open' ? 'text-emerald-400' : 'text-purple-400'}`} />
                              <span>#{pr.number} {pr.title}</span>
                              <span className={`text-[10px] px-1.5 py-0.2 rounded uppercase font-mono font-bold ${
                                pr.state === 'open' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-purple-500/10 text-purple-400'
                              }`}>
                                {pr.state}
                              </span>
                            </div>
                            <div className="text-[10px] text-slate-400">
                              by <strong className="text-slate-300">@{pr.author}</strong> • <span className="font-mono text-indigo-300">{pr.headBranch}</span> → <span className="font-mono text-slate-400">{pr.baseBranch}</span>
                            </div>
                          </div>
                          <a href={pr.htmlUrl} target="_blank" rel="noreferrer" className="p-1.5 rounded-lg text-slate-400 hover:text-emerald-400 hover:bg-slate-800">
                            <ExternalLink className="w-3.5 h-3.5" />
                          </a>
                        </div>
                      ))
                    )}
                  </div>
                )}

                {/* Tab: Issues */}
                {explorerTab === 'issues' && (
                  <div className="space-y-3 max-h-72 overflow-y-auto">
                    {githubIssuesLoading ? (
                      <div className="text-center py-8 text-xs text-slate-500">Fetching GitHub issues...</div>
                    ) : (githubIssuesData?.data || []).length === 0 ? (
                      <div className="text-center py-8 text-xs text-slate-500">No issues found.</div>
                    ) : (
                      (githubIssuesData?.data || []).map((issue: any) => (
                        <div key={issue.number} className="p-3.5 rounded-2xl bg-slate-900/80 border border-slate-800 flex items-center justify-between">
                          <div className="space-y-1">
                            <div className="text-xs font-bold text-white flex items-center gap-2">
                              <AlertCircle className="w-3.5 h-3.5 text-amber-400" />
                              <span>#{issue.number} {issue.title}</span>
                              <span className="text-[10px] px-1.5 py-0.2 rounded uppercase font-mono font-bold bg-amber-500/10 text-amber-400">
                                {issue.state}
                              </span>
                            </div>
                            <div className="text-[10px] text-slate-400 flex items-center gap-2">
                              <span>opened by @{issue.author}</span>
                              {(issue.labels || []).map((lbl: string) => (
                                <span key={lbl} className="px-1.5 py-0.2 rounded text-[9px] bg-slate-800 text-slate-300 font-mono">
                                  {lbl}
                                </span>
                              ))}
                            </div>
                          </div>
                          <a href={issue.htmlUrl} target="_blank" rel="noreferrer" className="p-1.5 rounded-lg text-slate-400 hover:text-amber-400 hover:bg-slate-800">
                            <ExternalLink className="w-3.5 h-3.5" />
                          </a>
                        </div>
                      ))
                    )}
                  </div>
                )}

                {/* Tab: Activity / Commits */}
                {explorerTab === 'commits' && (
                  <div className="space-y-3 max-h-72 overflow-y-auto">
                    {githubCommitsLoading ? (
                      <div className="text-center py-8 text-xs text-slate-500">Fetching commits...</div>
                    ) : (githubCommitsData?.data || []).length === 0 ? (
                      <div className="text-center py-8 text-xs text-slate-500">No commits found.</div>
                    ) : (
                      (githubCommitsData?.data || []).map((commit: any) => (
                        <div key={commit.sha} className="p-3.5 rounded-2xl bg-slate-900/80 border border-slate-800 flex items-center justify-between">
                          <div className="space-y-1">
                            <div className="text-xs font-bold text-white flex items-center gap-2">
                              <span className="px-1.5 py-0.5 rounded text-[10px] font-mono font-bold bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">
                                {commit.shortSha || commit.sha.substring(0, 7)}
                              </span>
                              <span className="truncate max-w-sm">{commit.message}</span>
                            </div>
                            <div className="text-[10px] text-slate-400">
                              by <strong className="text-slate-300">{commit.authorName}</strong> • {commit.date ? new Date(commit.date).toLocaleDateString() : 'Recent'}
                            </div>
                          </div>
                          <a href={commit.htmlUrl} target="_blank" rel="noreferrer" className="p-1.5 rounded-lg text-slate-400 hover:text-cyan-400 hover:bg-slate-800">
                            <ExternalLink className="w-3.5 h-3.5" />
                          </a>
                        </div>
                      ))
                    )}
                  </div>
                )}
              </div>
            )}

            {/* SLACK EXPLORER */}
            {activeIntegration.provider === 'SLACK' && (
              <div className="space-y-4">
                <div className="flex items-center gap-2 border-b border-slate-800 pb-2">
                  <button
                    onClick={() => setExplorerTab('channels')}
                    className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all ${
                      explorerTab === 'channels' ? 'bg-indigo-600 text-white' : 'text-slate-400 hover:text-white hover:bg-slate-800'
                    }`}
                  >
                    Channels ({(slackChannelsData?.data || []).length})
                  </button>
                  <button
                    onClick={() => setExplorerTab('send')}
                    className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all ${
                      explorerTab === 'send' ? 'bg-indigo-600 text-white' : 'text-slate-400 hover:text-white hover:bg-slate-800'
                    }`}
                  >
                    Send Test Alert
                  </button>
                </div>

                {explorerTab === 'channels' && (
                  <div className="space-y-3 max-h-72 overflow-y-auto">
                    {slackChannelsLoading ? (
                      <div className="text-center py-8 text-xs text-slate-500">Listing Slack channels...</div>
                    ) : (slackChannelsData?.data || []).length === 0 ? (
                      <div className="text-center py-8 text-xs text-slate-500">No channels found in this workspace.</div>
                    ) : (
                      (slackChannelsData?.data || []).map((ch: any) => (
                        <div key={ch.id} className="p-3.5 rounded-2xl bg-slate-900/80 border border-slate-800 flex items-center justify-between">
                          <div>
                            <div className="text-xs font-bold text-white flex items-center gap-1.5">
                              <span className="text-emerald-400 font-bold">#</span>
                              <span>{ch.name}</span>
                              {ch.isPrivate && <span className="text-[10px] px-1.5 py-0.2 rounded bg-slate-800 text-slate-400">Private</span>}
                            </div>
                            <div className="text-[10px] text-slate-400 mt-0.5">
                              {ch.numMembers} members • {ch.topic || 'No topic'}
                            </div>
                          </div>
                          <button
                            onClick={() => {
                              setSlackTargetChannel(ch.name);
                              setExplorerTab('send');
                            }}
                            className="px-2.5 py-1 rounded-lg bg-slate-800 text-indigo-400 hover:bg-slate-700 text-xs font-semibold"
                          >
                            Target
                          </button>
                        </div>
                      ))
                    )}
                  </div>
                )}

                {explorerTab === 'send' && (
                  <div className="space-y-3">
                    {slackSendResult && (
                      <div className="p-3 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 text-xs">
                        {slackSendResult}
                      </div>
                    )}
                    <div>
                      <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                        Target Channel
                      </label>
                      <input
                        type="text"
                        placeholder="e.g. general or engineering-alerts"
                        value={slackTargetChannel || 'general'}
                        onChange={(e) => setSlackTargetChannel(e.target.value)}
                        className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                        Message Content
                      </label>
                      <textarea
                        rows={3}
                        value={slackMessageText}
                        onChange={(e) => setSlackMessageText(e.target.value)}
                        className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
                      />
                    </div>
                    <button
                      onClick={() => {
                        sendSlackMessageMutation.mutate({
                          id: activeIntegration.id,
                          data: { channel: slackTargetChannel || 'general', text: slackMessageText },
                        });
                      }}
                      disabled={sendSlackMessageMutation.isPending}
                      className="w-full py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold shadow-lg shadow-emerald-600/25 flex items-center justify-center gap-2"
                    >
                      <Send className="w-3.5 h-3.5" />
                      <span>{sendSlackMessageMutation.isPending ? 'Sending...' : 'Send to Slack'}</span>
                    </button>
                  </div>
                )}
              </div>
            )}

            <div className="flex justify-end pt-2 border-t border-slate-800">
              <button
                onClick={() => setExplorerModalOpen(false)}
                className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs"
              >
                Close Explorer
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
