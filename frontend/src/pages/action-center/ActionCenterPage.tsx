import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { actionCenterApi, leaveApi, taskApi } from '../../api/services';
import { ActionItem } from '../../types';
import { Modal } from '../../components/common/Modal';
import {
  Inbox,
  Calendar,
  CheckSquare,
  FileText,
  CheckCircle,
  XCircle,
  Clock,
  AlertCircle,
  User,
  Building,
  RotateCcw,
  ExternalLink,
  GitBranch,
  GitCommit,
  GitPullRequest,
} from 'lucide-react';

export const ActionCenterPage: React.FC = () => {
  const [selectedType, setSelectedType] = useState<string>('ALL');
  const [reviewModalOpen, setReviewModalOpen] = useState(false);
  const [activeItem, setActiveItem] = useState<ActionItem | null>(null);
  const [reviewDecision, setReviewDecision] = useState<'APPROVE' | 'REJECT' | 'CHANGES'>('APPROVE');
  const [reviewComment, setReviewComment] = useState('');

  const queryClient = useQueryClient();

  const { data: itemsData, isLoading } = useQuery({
    queryKey: ['action-center-items', selectedType],
    queryFn: () => actionCenterApi.getPending(selectedType === 'ALL' ? undefined : selectedType),
    refetchInterval: 5000, // Real-time refresh poll fallback
  });

  const { data: summaryData } = useQuery({
    queryKey: ['action-center-summary'],
    queryFn: () => actionCenterApi.getSummary(),
  });

  // Leave Review Mutation
  const reviewLeaveMutation = useMutation({
    mutationFn: (payload: { id: string; status: string; comment?: string }) =>
      leaveApi.review(payload.id, { status: payload.status, reviewComment: payload.comment }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['action-center-items'] });
      queryClient.invalidateQueries({ queryKey: ['action-center-summary'] });
      setReviewModalOpen(false);
      setReviewComment('');
    },
  });

  // Task Review Mutation
  const reviewTaskMutation = useMutation({
    mutationFn: (payload: { id: string; decision: string; comment?: string }) =>
      taskApi.review(payload.id, { decision: payload.decision, comment: payload.comment }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['action-center-items'] });
      queryClient.invalidateQueries({ queryKey: ['action-center-summary'] });
      setReviewModalOpen(false);
      setReviewComment('');
    },
  });

  const openReviewDialog = (item: ActionItem, decision: 'APPROVE' | 'REJECT' | 'CHANGES') => {
    setActiveItem(item);
    setReviewDecision(decision);
    setReviewComment('');
    setReviewModalOpen(true);
  };

  const handleConfirmReview = () => {
    if (!activeItem) return;

    if (activeItem.type === 'LEAVE_REQUEST') {
      const status = reviewDecision === 'APPROVE' ? 'APPROVED' : 'REJECTED';
      reviewLeaveMutation.mutate({ id: activeItem.id, status, comment: reviewComment });
    } else if (activeItem.type === 'TASK_REVIEW') {
      const decision = reviewDecision === 'APPROVE' ? 'APPROVED' : 'CHANGES_REQUESTED';
      reviewTaskMutation.mutate({ id: activeItem.id, decision, comment: reviewComment });
    }
  };

  const items = itemsData?.data || [];
  const summary = summaryData?.data;

  return (
    <div className="space-y-8">
      {/* Header */}
      <div>
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider border border-indigo-500/20 mb-2">
          <Inbox className="w-3.5 h-3.5" />
          Submission & Workflow Hub
        </div>
        <h1 className="text-3xl font-black font-display text-white">Action Center</h1>
        <p className="text-slate-400 text-sm">
          Review, approve, or request changes on employee submissions across leave, tasks, and documents.
        </p>
      </div>

      {/* Filter Tabs & Counts */}
      <div className="flex flex-wrap items-center gap-3 border-b border-slate-800 pb-4">
        {[
          { id: 'ALL', label: 'All Submissions', count: summary?.totalPending || 0 },
          { id: 'LEAVE', label: 'Leave Requests', count: summary?.pendingLeaves || 0 },
          { id: 'TASK', label: 'Task Reviews', count: summary?.pendingTaskReviews || 0 },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setSelectedType(tab.id)}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all flex items-center gap-2 ${
              selectedType === tab.id
                ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/25'
                : 'bg-slate-900 border border-slate-800 text-slate-400 hover:text-slate-200'
            }`}
          >
            <span>{tab.label}</span>
            <span className="px-1.5 py-0.5 rounded-full bg-black/20 text-[10px]">
              {tab.count}
            </span>
          </button>
        ))}
      </div>

      {/* Submissions List */}
      {isLoading ? (
        <div className="text-center py-20 text-slate-500 text-sm">Loading pending submissions...</div>
      ) : items.length === 0 ? (
        <div className="glass-panel p-12 text-center rounded-3xl">
          <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center justify-center mx-auto mb-4">
            <CheckCircle className="w-6 h-6" />
          </div>
          <h3 className="text-lg font-bold text-white font-display">All Caught Up!</h3>
          <p className="text-sm text-slate-400 mt-1">No pending requests require your review right now.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {items.map((item) => (
            <div
              key={item.id}
              className="glass-panel p-6 rounded-3xl border border-slate-800 flex flex-col md:flex-row md:items-center justify-between gap-6 hover:border-slate-700 transition-colors"
            >
              {/* Requester Profile + Summary */}
              <div className="flex items-start gap-4">
                <div className="w-12 h-12 rounded-2xl bg-slate-800 border border-slate-700 flex items-center justify-center text-indigo-400 font-bold text-base flex-shrink-0">
                  {item.requesterName?.charAt(0) || 'U'}
                </div>

                <div className="space-y-1">
                  <div className="flex items-center gap-2.5">
                    <span className="text-base font-bold text-white">{item.requesterName}</span>
                    <span className="text-xs px-2 py-0.5 rounded bg-slate-800 text-indigo-400 font-mono border border-slate-700">
                      {item.requesterEmployeeCode}
                    </span>
                    <span className="text-[10px] px-2 py-0.5 rounded uppercase font-bold tracking-wider bg-amber-500/10 text-amber-400 border border-amber-500/20">
                      {item.type === 'LEAVE_REQUEST' ? 'Leave Request' : 'Task Review'}
                    </span>
                  </div>

                  <div className="text-sm font-semibold text-slate-200 mt-1">{item.title}</div>
                  {item.description && (
                    <div className="text-xs text-slate-400 italic">"{item.description}"</div>
                  )}

                  {/* Codebase & Repository Submission Details for Task Reviews */}
                  {item.type === 'TASK_REVIEW' && item.metadata?.repositoryUrl && (
                    <div className="mt-3 p-3 rounded-2xl bg-slate-900/90 border border-slate-800 space-y-2 max-w-xl">
                      <div className="flex items-center justify-between gap-2 flex-wrap">
                        <div className="flex items-center gap-2">
                          <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                            {item.metadata.provider || 'GIT'}
                          </span>
                          <span className="text-xs font-mono text-slate-300 truncate max-w-xs">
                            {item.metadata.repositoryUrl}
                          </span>
                        </div>
                        <a
                          href={item.metadata.repositoryUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="px-2.5 py-1 rounded-lg bg-indigo-600/20 hover:bg-indigo-600/40 text-indigo-300 text-[11px] font-bold inline-flex items-center gap-1 transition-colors"
                        >
                          <span>Open Repository</span>
                          <ExternalLink className="w-3 h-3" />
                        </a>
                      </div>

                      <div className="flex items-center gap-3 text-[11px] text-slate-400 flex-wrap">
                        {item.metadata.branch && (
                          <span className="flex items-center gap-1 bg-slate-800 px-2 py-0.5 rounded font-mono text-slate-300">
                            <GitBranch className="w-3 h-3 text-amber-400" /> {item.metadata.branch}
                          </span>
                        )}
                        {item.metadata.commitSha && (
                          <span className="flex items-center gap-1 bg-slate-800 px-2 py-0.5 rounded font-mono text-slate-300">
                            <GitCommit className="w-3 h-3 text-cyan-400" /> {item.metadata.commitSha.substring(0, 7)}
                          </span>
                        )}
                        {item.metadata.pullRequestUrl && (
                          <a
                            href={item.metadata.pullRequestUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="flex items-center gap-1 text-emerald-400 hover:underline font-mono"
                          >
                            <GitPullRequest className="w-3 h-3" /> View PR ↗
                          </a>
                        )}
                      </div>

                      {item.metadata.workSummary && (
                        <div className="text-xs text-slate-300 pt-1 border-t border-slate-800/60">
                          <strong className="text-slate-400 font-medium">Work Summary:</strong> {item.metadata.workSummary}
                        </div>
                      )}
                    </div>
                  )}

                  <div className="flex items-center gap-4 text-xs text-slate-500 pt-1">
                    {item.requesterDepartment && (
                      <span className="flex items-center gap-1">
                        <Building className="w-3.5 h-3.5" /> {item.requesterDepartment}
                      </span>
                    )}
                    <span className="flex items-center gap-1">
                      <Clock className="w-3.5 h-3.5" /> {new Date(item.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex items-center gap-3 self-end md:self-auto">
                {item.type === 'TASK_REVIEW' && (
                  <button
                    onClick={() => openReviewDialog(item, 'CHANGES')}
                    className="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-amber-400 border border-slate-700 text-xs font-bold flex items-center gap-1.5 transition-all"
                  >
                    <RotateCcw className="w-3.5 h-3.5" />
                    <span>Request Changes</span>
                  </button>
                )}

                <button
                  onClick={() => openReviewDialog(item, 'REJECT')}
                  className="px-4 py-2.5 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/20 text-xs font-bold flex items-center gap-1.5 transition-all"
                >
                  <XCircle className="w-3.5 h-3.5" />
                  <span>Reject</span>
                </button>

                <button
                  onClick={() => openReviewDialog(item, 'APPROVE')}
                  className="px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold shadow-lg shadow-emerald-600/25 flex items-center gap-1.5 transition-all"
                >
                  <CheckCircle className="w-3.5 h-3.5" />
                  <span>Approve</span>
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Review Dialog Modal */}
      <Modal
        isOpen={reviewModalOpen}
        onClose={() => setReviewModalOpen(false)}
        title={`Confirm ${
          reviewDecision === 'APPROVE'
            ? 'Approval'
            : reviewDecision === 'REJECT'
            ? 'Rejection'
            : 'Changes Request'
        }`}
      >
        <div className="space-y-4">
          <p className="text-sm text-slate-300">
            You are about to{' '}
            <strong className="text-white">
              {reviewDecision === 'APPROVE'
                ? 'approve'
                : reviewDecision === 'REJECT'
                ? 'reject'
                : 'request changes on'}
            </strong>{' '}
            the submission from <strong>{activeItem?.requesterName}</strong>.
          </p>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              {reviewDecision === 'REJECT'
                ? 'Rejection Reason * (Required)'
                : 'Feedback / Comment (Optional)'}
            </label>
            <textarea
              rows={3}
              required={reviewDecision === 'REJECT'}
              placeholder="Provide feedback or rationale for the employee..."
              value={reviewComment}
              onChange={(e) => setReviewComment(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 text-sm"
            />
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button
              onClick={() => setReviewModalOpen(false)}
              className="px-4 py-2.5 rounded-xl bg-slate-800 text-slate-300 text-xs font-bold hover:bg-slate-700"
            >
              Cancel
            </button>
            <button
              onClick={handleConfirmReview}
              disabled={
                (reviewDecision === 'REJECT' && !reviewComment.trim()) ||
                reviewLeaveMutation.isPending ||
                reviewTaskMutation.isPending
              }
              className={`px-5 py-2.5 rounded-xl text-white text-xs font-bold shadow-lg disabled:opacity-50 ${
                reviewDecision === 'APPROVE'
                  ? 'bg-emerald-600 hover:bg-emerald-500'
                  : reviewDecision === 'REJECT'
                  ? 'bg-rose-600 hover:bg-rose-500'
                  : 'bg-amber-600 hover:bg-amber-500'
              }`}
            >
              Confirm {reviewDecision}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
