import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { leaveApi } from '../../api/services';
import { Modal } from '../../components/common/Modal';
import {
  Calendar,
  Plus,
  Clock,
  CheckCircle2,
  XCircle,
  AlertCircle,
  Sparkles,
  ShieldAlert,
} from 'lucide-react';

export const LeavePage: React.FC = () => {
  const [applyModalOpen, setApplyModalOpen] = useState(false);
  const [leaveTypeId, setLeaveTypeId] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);

  const queryClient = useQueryClient();

  const { data: typesData, isLoading: typesLoading } = useQuery({
    queryKey: ['leave-types'],
    queryFn: () => leaveApi.getTypes(),
  });

  const { data: balancesData, isLoading: balancesLoading } = useQuery({
    queryKey: ['my-leave-balances'],
    queryFn: () => leaveApi.getMyBalances(),
  });

  const { data: myLeavesData, isLoading: leavesLoading } = useQuery({
    queryKey: ['my-leaves'],
    queryFn: () => leaveApi.getMy(),
  });

  const types = typesData?.data || [];
  const balances = balancesData?.data || [];
  const leaves = myLeavesData?.data?.content || [];

  useEffect(() => {
    if (types.length > 0 && !leaveTypeId) {
      setLeaveTypeId(types[0].id);
    }
  }, [types, leaveTypeId]);

  const applyLeaveMutation = useMutation({
    mutationFn: (payload: any) => leaveApi.apply(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-leaves'] });
      queryClient.invalidateQueries({ queryKey: ['my-leave-balances'] });
      queryClient.invalidateQueries({ queryKey: ['action-center-summary'] });
      setApplyModalOpen(false);
      setReason('');
      setStartDate('');
      setEndDate('');
      setError(null);
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || 'Failed to submit leave request');
    },
  });

  const handleApply = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!leaveTypeId && types.length > 0) {
      setLeaveTypeId(types[0].id);
    }
    applyLeaveMutation.mutate({
      leaveTypeId: leaveTypeId || (types[0] ? types[0].id : null),
      startDate,
      endDate,
      reason,
    });
  };

  const getTypeName = (typeId: string) => {
    return types.find((t: any) => t.id === typeId)?.name || 'General Leave';
  };

  return (
    <div className="space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider border border-indigo-500/20 mb-2">
            <Calendar className="w-3.5 h-3.5" />
            Time Off & Leave
          </div>
          <h1 className="text-3xl font-black font-display text-white">Leave Management</h1>
          <p className="text-slate-400 text-sm">
            View allocated balances, submit leave applications, and track manager reviews.
          </p>
        </div>

        <button
          onClick={() => {
            if (types.length > 0 && !leaveTypeId) setLeaveTypeId(types[0].id);
            setError(null);
            setApplyModalOpen(true);
          }}
          className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs shadow-lg shadow-indigo-600/25 flex items-center gap-2 transition-all self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>Apply for Leave</span>
        </button>
      </div>

      {/* Leave Balances Grid */}
      <div>
        <h2 className="text-sm font-bold uppercase tracking-wider text-slate-400 mb-3">Available Leave Balances</h2>
        {balancesLoading ? (
          <div className="text-xs text-slate-500">Loading leave allowances...</div>
        ) : balances.length === 0 ? (
          <div className="glass-panel p-6 rounded-2xl border border-slate-800 text-center text-slate-400 text-xs">
            Standard leave balance allocations active ({new Date().getFullYear()}).
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
            {balances.map((b: any) => {
              const typeName = getTypeName(b.leaveTypeId);
              return (
                <div key={b.id} className="glass-panel p-5 rounded-2xl border border-slate-800 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold uppercase tracking-wider text-indigo-400">
                      {typeName}
                    </span>
                    <span className="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-slate-400 font-mono">
                      {b.year}
                    </span>
                  </div>
                  <div className="text-3xl font-black font-display text-white">{b.remaining} <span className="text-xs font-normal text-slate-400">days left</span></div>
                  <div className="text-xs text-slate-500">
                    {b.used} of {b.total} days utilized
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Leave Application History */}
      <div className="glass-panel p-6 rounded-3xl space-y-4 border border-slate-800">
        <h3 className="text-base font-bold text-white font-display">My Leave Applications</h3>

        {leavesLoading ? (
          <div className="text-center py-10 text-slate-500 text-sm">Loading leave history...</div>
        ) : leaves.length === 0 ? (
          <div className="text-center py-10 text-slate-500 text-sm">
            No leave requests submitted yet.
          </div>
        ) : (
          <div className="space-y-3">
            {leaves.map((leave: any) => (
              <div
                key={leave.id}
                className="p-4 rounded-2xl bg-slate-900/60 border border-slate-800 flex items-center justify-between"
              >
                <div className="space-y-1">
                  <div className="text-sm font-bold text-white flex items-center gap-2">
                    <span>{getTypeName(leave.leaveTypeId)}</span>
                    <span className="text-slate-500 font-normal">•</span>
                    <span className="text-slate-300 font-normal">
                      {leave.startDate} to {leave.endDate} ({leave.days} {leave.days === 1 ? 'day' : 'days'})
                    </span>
                  </div>
                  <div className="text-xs text-slate-400">{leave.reason || 'No reason provided'}</div>
                  {leave.reviewComment && (
                    <div className="text-xs text-indigo-400 font-medium">
                      Reviewer Feedback: "{leave.reviewComment}"
                    </div>
                  )}
                </div>

                <span
                  className={`text-xs px-3 py-1 rounded-lg font-bold border ${
                    leave.status === 'APPROVED'
                      ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                      : leave.status === 'REJECTED'
                      ? 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                      : 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                  }`}
                >
                  {leave.status}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Apply Leave Modal */}
      <Modal isOpen={applyModalOpen} onClose={() => setApplyModalOpen(false)} title="Apply for Leave">
        <form onSubmit={handleApply} className="space-y-4">
          {error && (
            <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Leave Type *
            </label>
            <select
              required
              value={leaveTypeId}
              onChange={(e) => setLeaveTypeId(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            >
              {types.length === 0 ? (
                <option value="">Loading leave categories...</option>
              ) : (
                types.map((t: any) => (
                  <option key={t.id} value={t.id}>{t.name} ({t.defaultBalance} days allowance)</option>
                ))
              )}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                Start Date *
              </label>
              <input
                type="date"
                required
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                End Date *
              </label>
              <input
                type="date"
                required
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Reason for Absence *
            </label>
            <textarea
              rows={3}
              required
              placeholder="Provide reason for time off..."
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div className="flex justify-end gap-3 pt-3">
            <button
              type="button"
              onClick={() => setApplyModalOpen(false)}
              className="px-4 py-2.5 rounded-xl bg-slate-800 text-slate-300 text-xs font-bold hover:bg-slate-700"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={applyLeaveMutation.isPending || types.length === 0}
              className="px-5 py-2.5 rounded-xl bg-indigo-600 text-white text-xs font-bold shadow-lg shadow-indigo-600/25 disabled:opacity-50"
            >
              {applyLeaveMutation.isPending ? 'Submitting...' : 'Submit Application'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
