import React from 'react';
import { Task } from '../../types';
import { Clock, CheckSquare, ArrowRight, Play, CheckCircle2, RotateCcw } from 'lucide-react';

interface KanbanBoardProps {
  tasks: Task[];
  onTaskClick: (task: Task) => void;
  onStatusChange: (taskId: string, newStatus: string) => void;
}

export const KanbanBoard: React.FC<KanbanBoardProps> = ({
  tasks,
  onTaskClick,
  onStatusChange,
}) => {
  const columns = [
    { id: 'TODO', label: 'To Do', color: 'border-slate-700' },
    { id: 'IN_PROGRESS', label: 'In Progress', color: 'border-amber-500/40' },
    { id: 'REVIEW', label: 'In Review', color: 'border-indigo-500/40' },
    { id: 'COMPLETED', label: 'Completed', color: 'border-emerald-500/40' },
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 items-start">
      {columns.map((col) => {
        const colTasks = tasks.filter((t) => t.status === col.id);
        return (
          <div
            key={col.id}
            className={`glass-panel p-4 rounded-3xl border ${col.color} bg-slate-900/40 flex flex-col gap-3 min-h-[600px]`}
          >
            {/* Column Header */}
            <div className="flex items-center justify-between px-2 py-1">
              <span className="text-sm font-bold text-white font-display flex items-center gap-2">
                {col.label}
              </span>
              <span className="text-xs font-mono px-2 py-0.5 rounded-full bg-slate-800 text-slate-400 font-bold">
                {colTasks.length}
              </span>
            </div>

            {/* Task Cards */}
            <div className="space-y-3 flex-1">
              {colTasks.map((task) => (
                <div
                  key={task.id}
                  onClick={() => onTaskClick(task)}
                  className="glass-card glass-card-hover p-4 rounded-2xl border border-slate-800/80 cursor-pointer space-y-3"
                >
                  <div className="flex items-center justify-between text-[10px]">
                    <span className="px-2 py-0.5 rounded font-mono font-bold uppercase bg-slate-800 text-slate-300">
                      {task.priority}
                    </span>
                    {task.dueDate && (
                      <span className="text-slate-500 flex items-center gap-1 font-mono">
                        <Clock className="w-3 h-3" /> {task.dueDate}
                      </span>
                    )}
                  </div>

                  <h4 className="text-sm font-bold text-white leading-snug">{task.title}</h4>
                  {task.description && (
                    <p className="text-xs text-slate-400 line-clamp-2">{task.description}</p>
                  )}

                  {/* Quick Action Progression Buttons */}
                  <div className="pt-2 border-t border-slate-800/60 flex items-center justify-between">
                    <span className="text-[10px] text-slate-500 font-mono">
                      {task.estimatedHours ? `${task.estimatedHours}h est` : 'No est'}
                    </span>

                    <div className="flex items-center gap-1">
                      {col.id === 'TODO' && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            onStatusChange(task.id, 'IN_PROGRESS');
                          }}
                          className="px-2 py-1 rounded bg-indigo-600/20 hover:bg-indigo-600/40 text-indigo-300 text-[10px] font-bold"
                        >
                          Start
                        </button>
                      )}
                      {col.id === 'IN_PROGRESS' && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            onStatusChange(task.id, 'REVIEW');
                          }}
                          className="px-2 py-1 rounded bg-amber-500/20 hover:bg-amber-500/40 text-amber-300 text-[10px] font-bold"
                        >
                          Submit
                        </button>
                      )}
                      {col.id === 'REVIEW' && (
                        <span className="text-[10px] text-indigo-400 italic">In Review</span>
                      )}
                      {col.id === 'COMPLETED' && (
                        <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        );
      })}
    </div>
  );
};
