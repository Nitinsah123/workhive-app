import React, { useState, useEffect, useMemo, useRef } from 'react';
import { useUIStore } from '../../store/uiStore';
import { searchApi } from '../../api/services';
import {
  Search,
  X,
  FolderKanban,
  CheckSquare,
  Users,
  FileText,
  Building2,
  Briefcase,
  ArrowRight,
  CornerDownLeft,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface SearchItem {
  id: string;
  type: 'PROJECT' | 'TASK' | 'USER' | 'DOCUMENT' | 'DEPARTMENT' | 'TEAM';
  title: string;
  subtitle?: string;
  badge?: string;
  path: string;
}

export const GlobalSearchModal: React.FC = () => {
  const { searchModalOpen, setSearchModalOpen } = useUIStore();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement>(null);

  // Global Cmd+K / Ctrl+K listener
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        setSearchModalOpen(true);
      }
      if (e.key === 'Escape') {
        setSearchModalOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [setSearchModalOpen]);

  // Focus input on open
  useEffect(() => {
    if (searchModalOpen) {
      setTimeout(() => inputRef.current?.focus(), 50);
      setSelectedIndex(0);
    } else {
      setQuery('');
      setResults(null);
    }
  }, [searchModalOpen]);

  // Debounced search query
  useEffect(() => {
    if (!query.trim()) {
      setResults(null);
      setLoading(false);
      return;
    }
    const timer = setTimeout(async () => {
      setLoading(true);
      try {
        const res = await searchApi.search(query);
        setResults(res.data);
        setSelectedIndex(0);
      } catch (err) {
        console.error('Search error', err);
      } finally {
        setLoading(false);
      }
    }, 200);
    return () => clearTimeout(timer);
  }, [query]);

  // Flatten results for keyboard navigation
  const flatItems = useMemo<SearchItem[]>(() => {
    if (!results) return [];
    const items: SearchItem[] = [];

    if (results.users?.length > 0) {
      results.users.forEach((u: any) => {
        items.push({
          id: `user-${u.id}`,
          type: 'USER',
          title: u.fullName,
          subtitle: `${u.employeeCode || 'EMP'} • ${u.email}`,
          badge: u.role || 'Member',
          path: `/employees/${u.id}/360`,
        });
      });
    }

    if (results.projects?.length > 0) {
      results.projects.forEach((p: any) => {
        items.push({
          id: `project-${p.id}`,
          type: 'PROJECT',
          title: p.name,
          subtitle: p.description || 'Active project workspace',
          badge: p.status || 'Active',
          path: `/projects/${p.id}`,
        });
      });
    }

    if (results.tasks?.length > 0) {
      results.tasks.forEach((t: any) => {
        items.push({
          id: `task-${t.id}`,
          type: 'TASK',
          title: t.title,
          subtitle: `Priority: ${t.priority || 'MEDIUM'} • Due: ${t.dueDate || 'No deadline'}`,
          badge: t.status || 'TODO',
          path: `/tasks?taskId=${t.id}`,
        });
      });
    }

    if (results.departments?.length > 0) {
      results.departments.forEach((d: any) => {
        items.push({
          id: `dept-${d.id}`,
          type: 'DEPARTMENT',
          title: d.name,
          subtitle: d.description || 'Department roster & live workforce',
          badge: d.status || 'ACTIVE',
          path: `/departments/${d.id}/members`,
        });
      });
    }

    if (results.teams?.length > 0) {
      results.teams.forEach((tm: any) => {
        items.push({
          id: `team-${tm.id}`,
          type: 'TEAM',
          title: tm.name,
          subtitle: tm.description || 'Team live unit roster',
          badge: tm.status || 'ACTIVE',
          path: `/teams/${tm.id}/members`,
        });
      });
    }

    if (results.documents?.length > 0) {
      results.documents.forEach((doc: any) => {
        items.push({
          id: `doc-${doc.id}`,
          type: 'DOCUMENT',
          title: doc.name,
          subtitle: doc.description || doc.contentType || 'Stored document',
          badge: doc.entityType || 'ORG',
          path: `/documents?docId=${doc.id}`,
        });
      });
    }

    return items;
  }, [results]);

  const handleNavigate = (path: string) => {
    setSearchModalOpen(false);
    navigate(path);
  };

  const handleInputKeyDown = (e: React.KeyboardEvent) => {
    if (flatItems.length === 0) return;

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedIndex((prev) => (prev + 1) % flatItems.length);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedIndex((prev) => (prev - 1 + flatItems.length) % flatItems.length);
    } else if (e.key === 'Enter') {
      e.preventDefault();
      const target = flatItems[selectedIndex] || flatItems[0];
      if (target) {
        handleNavigate(target.path);
      }
    }
  };

  if (!searchModalOpen) return null;

  const totalResultsCount = flatItems.length;

  return (
    <div
      className="fixed inset-0 z-50 flex items-start justify-center pt-16 sm:pt-20 bg-black/75 backdrop-blur-md p-4 animate-fade-in"
      onClick={() => setSearchModalOpen(false)}
    >
      <div
        className="bg-slate-900 border border-slate-700/80 rounded-2xl w-full max-w-2xl shadow-2xl overflow-hidden flex flex-col max-h-[82vh] transition-all"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Search Input Bar */}
        <div className="p-4 border-b border-slate-800 flex items-center gap-3 bg-slate-900/90">
          <Search className="w-5 h-5 text-indigo-400 shrink-0" />
          <input
            ref={inputRef}
            type="text"
            placeholder="Search tasks, projects, people, departments, teams, docs..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={handleInputKeyDown}
            className="flex-1 bg-transparent border-none text-slate-100 placeholder-slate-500 focus:outline-none text-sm sm:text-base font-medium"
          />
          {query && (
            <button
              onClick={() => {
                setQuery('');
                setResults(null);
                inputRef.current?.focus();
              }}
              className="text-slate-500 hover:text-slate-300 p-1 rounded-lg"
              title="Clear search"
            >
              <X className="w-4 h-4" />
            </button>
          )}
          <kbd className="hidden sm:inline-flex items-center gap-1 text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700">
            ESC
          </kbd>
        </div>

        {/* Results List */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {loading && (
            <div className="flex items-center justify-center py-10 space-x-2 text-slate-400 text-sm">
              <div className="w-4 h-4 border-2 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin" />
              <span>Searching workspace records...</span>
            </div>
          )}

          {!loading && results && (
            <>
              {/* People Section */}
              {results.users?.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-amber-400 mb-2 flex items-center gap-1.5 px-1">
                    <Users className="w-3.5 h-3.5" /> People & Members
                  </h4>
                  <div className="space-y-1">
                    {results.users.map((u: any) => {
                      const itemIndex = flatItems.findIndex((fi) => fi.id === `user-${u.id}`);
                      const isSelected = selectedIndex === itemIndex;
                      return (
                        <div
                          key={u.id}
                          role="button"
                          tabIndex={0}
                          onMouseDown={(e) => {
                            e.preventDefault();
                            handleNavigate(`/employees/${u.id}/360`);
                          }}
                          onClick={() => handleNavigate(`/employees/${u.id}/360`)}
                          onMouseEnter={() => setSelectedIndex(itemIndex)}
                          className={`w-full text-left p-3 rounded-xl flex items-center justify-between cursor-pointer transition-all border ${
                            isSelected
                              ? 'bg-indigo-600/20 border-indigo-500/40 shadow-sm'
                              : 'bg-slate-800/40 border-transparent hover:bg-slate-800 hover:border-slate-700/60'
                          }`}
                        >
                          <div className="flex items-center gap-3">
                            <div className="w-8 h-8 rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/20 flex items-center justify-center font-bold text-xs">
                              {u.fullName?.charAt(0) || 'U'}
                            </div>
                            <div>
                              <div className="text-sm font-semibold text-white flex items-center gap-2">
                                <span>{u.fullName}</span>
                                <span className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-800 text-slate-400 border border-slate-700">
                                  {u.employeeCode}
                                </span>
                              </div>
                              <div className="text-xs text-slate-400">{u.email}</div>
                            </div>
                          </div>
                          <div className="flex items-center gap-2">
                            <span className="text-xs px-2 py-0.5 rounded-lg bg-slate-800 text-slate-300 border border-slate-700 font-medium">
                              {u.role}
                            </span>
                            <ArrowRight className="w-4 h-4 text-slate-500 group-hover:text-white" />
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Tasks Section */}
              {results.tasks?.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-emerald-400 mb-2 flex items-center gap-1.5 px-1">
                    <CheckSquare className="w-3.5 h-3.5" /> Kanban Tasks
                  </h4>
                  <div className="space-y-1">
                    {results.tasks.map((t: any) => {
                      const itemIndex = flatItems.findIndex((fi) => fi.id === `task-${t.id}`);
                      const isSelected = selectedIndex === itemIndex;
                      return (
                        <div
                          key={t.id}
                          role="button"
                          tabIndex={0}
                          onMouseDown={(e) => {
                            e.preventDefault();
                            handleNavigate(`/tasks?taskId=${t.id}`);
                          }}
                          onClick={() => handleNavigate(`/tasks?taskId=${t.id}`)}
                          onMouseEnter={() => setSelectedIndex(itemIndex)}
                          className={`w-full text-left p-3 rounded-xl flex items-center justify-between cursor-pointer transition-all border ${
                            isSelected
                              ? 'bg-indigo-600/20 border-indigo-500/40 shadow-sm'
                              : 'bg-slate-800/40 border-transparent hover:bg-slate-800 hover:border-slate-700/60'
                          }`}
                        >
                          <div className="space-y-0.5">
                            <span className="text-sm font-semibold text-white block">{t.title}</span>
                            <span className="text-xs text-slate-400 block">
                              Priority: <strong className="text-slate-300">{t.priority || 'MEDIUM'}</strong> • Due: {t.dueDate || 'No deadline'}
                            </span>
                          </div>
                          <div className="flex items-center gap-2">
                            <span
                              className={`text-xs px-2 py-0.5 rounded-lg font-bold border ${
                                t.status === 'COMPLETED' || t.status === 'DONE'
                                  ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                                  : t.status === 'IN_PROGRESS'
                                  ? 'bg-blue-500/10 text-blue-400 border-blue-500/20'
                                  : 'bg-slate-800 text-slate-400 border-slate-700'
                              }`}
                            >
                              {t.status}
                            </span>
                            <ArrowRight className="w-4 h-4 text-slate-500" />
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Projects Section */}
              {results.projects?.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-indigo-400 mb-2 flex items-center gap-1.5 px-1">
                    <FolderKanban className="w-3.5 h-3.5" /> Projects
                  </h4>
                  <div className="space-y-1">
                    {results.projects.map((p: any) => {
                      const itemIndex = flatItems.findIndex((fi) => fi.id === `project-${p.id}`);
                      const isSelected = selectedIndex === itemIndex;
                      return (
                        <div
                          key={p.id}
                          role="button"
                          tabIndex={0}
                          onMouseDown={(e) => {
                            e.preventDefault();
                            handleNavigate(`/projects/${p.id}`);
                          }}
                          onClick={() => handleNavigate(`/projects/${p.id}`)}
                          onMouseEnter={() => setSelectedIndex(itemIndex)}
                          className={`w-full text-left p-3 rounded-xl flex items-center justify-between cursor-pointer transition-all border ${
                            isSelected
                              ? 'bg-indigo-600/20 border-indigo-500/40 shadow-sm'
                              : 'bg-slate-800/40 border-transparent hover:bg-slate-800 hover:border-slate-700/60'
                          }`}
                        >
                          <div>
                            <span className="text-sm font-semibold text-white block">{p.name}</span>
                            <span className="text-xs text-slate-400 line-clamp-1">
                              {p.description || 'Active project workspace'}
                            </span>
                          </div>
                          <div className="flex items-center gap-2">
                            <span className="text-xs px-2 py-0.5 rounded-lg bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 font-bold">
                              {p.status || 'Active'}
                            </span>
                            <ArrowRight className="w-4 h-4 text-slate-500" />
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Departments Section */}
              {results.departments?.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-cyan-400 mb-2 flex items-center gap-1.5 px-1">
                    <Building2 className="w-3.5 h-3.5" /> Departments
                  </h4>
                  <div className="space-y-1">
                    {results.departments.map((d: any) => {
                      const itemIndex = flatItems.findIndex((fi) => fi.id === `dept-${d.id}`);
                      const isSelected = selectedIndex === itemIndex;
                      return (
                        <div
                          key={d.id}
                          role="button"
                          tabIndex={0}
                          onMouseDown={(e) => {
                            e.preventDefault();
                            handleNavigate(`/departments/${d.id}/members`);
                          }}
                          onClick={() => handleNavigate(`/departments/${d.id}/members`)}
                          onMouseEnter={() => setSelectedIndex(itemIndex)}
                          className={`w-full text-left p-3 rounded-xl flex items-center justify-between cursor-pointer transition-all border ${
                            isSelected
                              ? 'bg-indigo-600/20 border-indigo-500/40 shadow-sm'
                              : 'bg-slate-800/40 border-transparent hover:bg-slate-800 hover:border-slate-700/60'
                          }`}
                        >
                          <div>
                            <span className="text-sm font-semibold text-white block">{d.name}</span>
                            <span className="text-xs text-slate-400 line-clamp-1">
                              {d.description || 'Department roster and member performance'}
                            </span>
                          </div>
                          <div className="flex items-center gap-2">
                            <span className="text-xs px-2 py-0.5 rounded-lg bg-cyan-500/10 text-cyan-400 border border-cyan-500/20 font-bold">
                              Roster
                            </span>
                            <ArrowRight className="w-4 h-4 text-slate-500" />
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Teams Section */}
              {results.teams?.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-purple-400 mb-2 flex items-center gap-1.5 px-1">
                    <Briefcase className="w-3.5 h-3.5" /> Teams
                  </h4>
                  <div className="space-y-1">
                    {results.teams.map((tm: any) => {
                      const itemIndex = flatItems.findIndex((fi) => fi.id === `team-${tm.id}`);
                      const isSelected = selectedIndex === itemIndex;
                      return (
                        <div
                          key={tm.id}
                          role="button"
                          tabIndex={0}
                          onMouseDown={(e) => {
                            e.preventDefault();
                            handleNavigate(`/teams/${tm.id}/members`);
                          }}
                          onClick={() => handleNavigate(`/teams/${tm.id}/members`)}
                          onMouseEnter={() => setSelectedIndex(itemIndex)}
                          className={`w-full text-left p-3 rounded-xl flex items-center justify-between cursor-pointer transition-all border ${
                            isSelected
                              ? 'bg-indigo-600/20 border-indigo-500/40 shadow-sm'
                              : 'bg-slate-800/40 border-transparent hover:bg-slate-800 hover:border-slate-700/60'
                          }`}
                        >
                          <div>
                            <span className="text-sm font-semibold text-white block">{tm.name}</span>
                            <span className="text-xs text-slate-400 line-clamp-1">
                              {tm.description || 'Team unit live activity roster'}
                            </span>
                          </div>
                          <div className="flex items-center gap-2">
                            <span className="text-xs px-2 py-0.5 rounded-lg bg-purple-500/10 text-purple-400 border border-purple-500/20 font-bold">
                              Team Roster
                            </span>
                            <ArrowRight className="w-4 h-4 text-slate-500" />
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Documents Section */}
              {results.documents?.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-sky-400 mb-2 flex items-center gap-1.5 px-1">
                    <FileText className="w-3.5 h-3.5" /> Documents & Files
                  </h4>
                  <div className="space-y-1">
                    {results.documents.map((doc: any) => {
                      const itemIndex = flatItems.findIndex((fi) => fi.id === `doc-${doc.id}`);
                      const isSelected = selectedIndex === itemIndex;
                      return (
                        <div
                          key={doc.id}
                          role="button"
                          tabIndex={0}
                          onMouseDown={(e) => {
                            e.preventDefault();
                            handleNavigate(`/documents?docId=${doc.id}`);
                          }}
                          onClick={() => handleNavigate(`/documents?docId=${doc.id}`)}
                          onMouseEnter={() => setSelectedIndex(itemIndex)}
                          className={`w-full text-left p-3 rounded-xl flex items-center justify-between cursor-pointer transition-all border ${
                            isSelected
                              ? 'bg-indigo-600/20 border-indigo-500/40 shadow-sm'
                              : 'bg-slate-800/40 border-transparent hover:bg-slate-800 hover:border-slate-700/60'
                          }`}
                        >
                          <div>
                            <span className="text-sm font-semibold text-white block">{doc.name}</span>
                            <span className="text-xs text-slate-400">{doc.contentType || 'Document file'}</span>
                          </div>
                          <div className="flex items-center gap-2">
                            <span className="text-xs px-2 py-0.5 rounded-lg bg-slate-800 text-slate-400 border border-slate-700">
                              {doc.entityType || 'ORG'}
                            </span>
                            <ArrowRight className="w-4 h-4 text-slate-500" />
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </>
          )}

          {!loading && results && totalResultsCount === 0 && (
            <div className="text-center py-12 text-slate-500 text-sm">
              No matching records found for "{query}"
            </div>
          )}
        </div>

        {/* Footer Shortcut Bar */}
        <div className="px-4 py-2.5 bg-slate-950/60 border-t border-slate-800 flex items-center justify-between text-[11px] text-slate-500">
          <div className="flex items-center gap-3">
            <span className="flex items-center gap-1">
              <kbd className="px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700">↑↓</kbd> Navigate
            </span>
            <span className="flex items-center gap-1">
              <kbd className="px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700 flex items-center gap-0.5">
                <CornerDownLeft className="w-2.5 h-2.5" /> Enter
              </kbd> Open
            </span>
            <span className="flex items-center gap-1">
              <kbd className="px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700">ESC</kbd> Close
            </span>
          </div>

          <span>{totalResultsCount} result{totalResultsCount === 1 ? '' : 's'}</span>
        </div>
      </div>
    </div>
  );
};
