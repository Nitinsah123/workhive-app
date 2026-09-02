import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { attendanceApi, timeTrackingApi, taskApi } from '../../api/services';
import { Modal } from '../../components/common/Modal';
import {
  Clock,
  Play,
  Square,
  Plus,
  Calendar,
  Timer,
  CheckCircle,
  FileSpreadsheet,
} from 'lucide-react';

export const AttendancePage: React.FC = () => {
  const [logTimeModal, setLogTimeModal] = useState(false);
  const [taskId, setTaskId] = useState('');
  const [durationMinutes, setDurationMinutes] = useState('');
  const [description, setDescription] = useState('');

  const queryClient = useQueryClient();

  const { data: todayData } = useQuery({
    queryKey: ['today-attendance'],
    queryFn: () => attendanceApi.getToday(),
  });

  const { data: myAttendanceData } = useQuery({
    queryKey: ['my-attendance'],
    queryFn: () => attendanceApi.getMy(),
  });

  const { data: myTimeEntriesData } = useQuery({
    queryKey: ['my-time-entries'],
    queryFn: () => timeTrackingApi.getMy(),
  });

  const { data: tasksData } = useQuery({
    queryKey: ['tasks-all'],
    queryFn: () => taskApi.getAll(),
  });

  const checkInMutation = useMutation({
    mutationFn: () => attendanceApi.checkIn(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['today-attendance'] });
      queryClient.invalidateQueries({ queryKey: ['my-attendance'] });
    },
  });

  const checkOutMutation = useMutation({
    mutationFn: () => attendanceApi.checkOut(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['today-attendance'] });
      queryClient.invalidateQueries({ queryKey: ['my-attendance'] });
    },
  });

  const logTimeMutation = useMutation({
    mutationFn: (payload: any) => timeTrackingApi.logTime(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-time-entries'] });
      setLogTimeModal(false);
      setDurationMinutes('');
      setDescription('');
    },
  });

  const handleLogTime = (e: React.FormEvent) => {
    e.preventDefault();
    logTimeMutation.mutate({
      taskId: taskId || null,
      durationMinutes: parseInt(durationMinutes, 10),
      description,
    });
  };

  const today = todayData?.data;
  const attendanceRecords = myAttendanceData?.data?.content || [];
  const timeEntries = myTimeEntriesData?.data?.content || [];
  const tasks = tasksData?.data?.content || [];

  return (
    <div className="space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider border border-indigo-500/20 mb-2">
            <Clock className="w-3.5 h-3.5" />
            Time & Work Tracking
          </div>
          <h1 className="text-3xl font-black font-display text-white">Attendance & Time Logs</h1>
          <p className="text-slate-400 text-sm">
            Daily check-in, check-out, duration logging, and task time tracking.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <a
            href="/api/exports/attendance/xlsx"
            download
            className="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-bold text-xs border border-slate-700 flex items-center gap-2"
          >
            <FileSpreadsheet className="w-4 h-4 text-emerald-400" />
            <span>Export XLSX</span>
          </a>

          <button
            onClick={() => setLogTimeModal(true)}
            className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs shadow-lg shadow-indigo-600/25 flex items-center gap-2 transition-all"
          >
            <Plus className="w-4 h-4" />
            <span>Log Task Time</span>
          </button>
        </div>
      </div>

      {/* Today Widget */}
      <div className="glass-panel p-8 rounded-3xl border border-slate-800 flex flex-col md:flex-row items-center justify-between gap-6">
        <div className="space-y-2 text-center md:text-left">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
            Current Status
          </span>
          <div className="text-2xl font-black font-display text-white">
            {today?.checkedIn
              ? today?.status === 'CHECKED_IN'
                ? '🟢 Active Work Session'
                : '⚪ Session Completed for Today'
              : '🟡 Not Checked In Today'}
          </div>
          {today?.record?.checkIn && (
            <p className="text-xs text-slate-400">
              Checked in at {new Date(today.record.checkIn).toLocaleTimeString()}
              {today.record.durationMinutes ? ` • Duration: ${today.record.durationMinutes} mins` : ''}
            </p>
          )}
        </div>

        <div className="flex items-center gap-3">
          {!today?.checkedIn ? (
            <button
              onClick={() => checkInMutation.mutate()}
              disabled={checkInMutation.isPending}
              className="px-8 py-4 rounded-2xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-base shadow-xl shadow-emerald-600/30 flex items-center gap-2.5 transition-all"
            >
              <Play className="w-5 h-5 fill-current" />
              <span>Check In Now</span>
            </button>
          ) : today?.status === 'CHECKED_IN' ? (
            <button
              onClick={() => checkOutMutation.mutate()}
              disabled={checkOutMutation.isPending}
              className="px-8 py-4 rounded-2xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-base shadow-xl shadow-rose-600/30 flex items-center gap-2.5 transition-all"
            >
              <Square className="w-5 h-5 fill-current" />
              <span>Check Out</span>
            </button>
          ) : (
            <span className="text-xs text-slate-500 italic">Day Completed</span>
          )}
        </div>
      </div>

      {/* Two Grids: Attendance History & Task Time Log History */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Attendance History */}
        <div className="glass-panel p-6 rounded-3xl space-y-4">
          <h3 className="text-base font-bold text-white font-display flex items-center gap-2">
            <Calendar className="w-4 h-4 text-indigo-400" /> Attendance History
          </h3>

          <div className="divide-y divide-slate-800/60 max-h-96 overflow-y-auto">
            {attendanceRecords.length === 0 ? (
              <div className="text-center py-8 text-slate-500 text-sm">No attendance records yet</div>
            ) : (
              attendanceRecords.map((r) => (
                <div key={r.id} className="py-3 flex items-center justify-between">
                  <div>
                    <div className="text-sm font-bold text-white">{r.date}</div>
                    <div className="text-xs text-slate-400">
                      {r.checkIn ? new Date(r.checkIn).toLocaleTimeString() : '-'} →{' '}
                      {r.checkOut ? new Date(r.checkOut).toLocaleTimeString() : '-'}
                    </div>
                  </div>
                  <span className="text-xs font-mono font-bold text-emerald-400">
                    {r.durationMinutes ? `${Math.floor(r.durationMinutes / 60)}h ${r.durationMinutes % 60}m` : 'Active'}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Task Time Entries */}
        <div className="glass-panel p-6 rounded-3xl space-y-4">
          <h3 className="text-base font-bold text-white font-display flex items-center gap-2">
            <Timer className="w-4 h-4 text-indigo-400" /> Task Time Entries
          </h3>

          <div className="divide-y divide-slate-800/60 max-h-96 overflow-y-auto">
            {timeEntries.length === 0 ? (
              <div className="text-center py-8 text-slate-500 text-sm">No time entries logged yet</div>
            ) : (
              timeEntries.map((te) => (
                <div key={te.id} className="py-3 flex items-center justify-between">
                  <div className="space-y-0.5">
                    <div className="text-sm font-bold text-white">
                      {te.description || 'Task work log'}
                    </div>
                    <div className="text-xs text-slate-500">
                      {new Date(te.createdAt).toLocaleDateString()}
                    </div>
                  </div>
                  <span className="text-xs font-mono font-bold text-indigo-400">
                    {te.durationMinutes} mins
                  </span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Log Time Modal */}
      <Modal isOpen={logTimeModal} onClose={() => setLogTimeModal(false)} title="Log Task Time">
        <form onSubmit={handleLogTime} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Associated Task (Optional)
            </label>
            <select
              value={taskId}
              onChange={(e) => setTaskId(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            >
              <option value="">No specific task</option>
              {tasks.map((t) => (
                <option key={t.id} value={t.id}>{t.title}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Duration (Minutes) *
            </label>
            <input
              type="number"
              required
              min={1}
              placeholder="e.g. 60"
              value={durationMinutes}
              onChange={(e) => setDurationMinutes(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Description / Work Done
            </label>
            <textarea
              rows={3}
              placeholder="What did you work on during this time?"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div className="flex justify-end gap-3 pt-3">
            <button
              type="button"
              onClick={() => setLogTimeModal(false)}
              className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 text-xs font-bold"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={logTimeMutation.isPending}
              className="px-5 py-2 rounded-xl bg-indigo-600 text-white text-xs font-bold"
            >
              Log Time
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
