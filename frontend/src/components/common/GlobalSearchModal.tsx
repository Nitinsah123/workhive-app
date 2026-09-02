import React, { useState, useEffect } from 'react';
import { useUIStore } from '../../store/uiStore';
import { searchApi } from '../../api/services';
import { Search, X, FolderKanban, CheckSquare, Users, FileText } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const GlobalSearchModal: React.FC = () => {
  const { searchModalOpen, setSearchModalOpen } = useUIStore();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
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

  useEffect(() => {
    if (!query.trim()) {
      setResults(null);
      return;
    }
    const timer = setTimeout(async () => {
      setLoading(true);
      try {
        const res = await searchApi.search(query);
        setResults(res.data);
      } catch (err) {
        console.error('Search error', err);
      } finally {
        setLoading(false);
      }
    }, 250);
    return () => clearTimeout(timer);
  }, [query]);

  if (!searchModalOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center pt-20 bg-black/70 backdrop-blur-sm p-4">
      <div className="bg-slate-900 border border-slate-700/80 rounded-2xl w-full max-w-2xl shadow-2xl overflow-hidden flex flex-col max-h-[80vh]">
        {/* Search Input Bar */}
        <div className="p-4 border-b border-slate-800 flex items-center gap-3">
          <Search className="w-5 h-5 text-indigo-400" />
          <input
            type="text"
            placeholder="Search projects, tasks, people, documents..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            autoFocus
            className="flex-1 bg-transparent border-none text-slate-100 placeholder-slate-500 focus:outline-none text-base"
          />
          <button
            onClick={() => setSearchModalOpen(false)}
            className="text-slate-500 hover:text-slate-300 p-1 rounded-lg hover:bg-slate-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Results List */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {loading && (
            <div className="text-center py-8 text-slate-400 text-sm">Searching...</div>
          )}

          {!loading && results && (
            <>
              {/* Projects */}
              {results.projects?.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-2 flex items-center gap-1.5">
                    <FolderKanban className="w-3.5 h-3.5 text-indigo-400" /> Projects
                  </h4>
                  <div className="space-y-1">
                    {results.projects.map((p: any) => (
                      <button
                        key={p.id}
                        onClick={() => {
                          setSearchModalOpen(false);
                          navigate(`/projects/${p.id}`);
                        }}
                        className="w-full text-left p-2.5 rounded-lg hover:bg-slate-800 flex items-center justify-between transition-colors"
                      >
                        <span className="text-sm font-medium text-slate-200">{p.name}</span>
                        <span className="text-xs px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                          {p.status}
                        </span>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Tasks */}
              {results.tasks?.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-2 flex items-center gap-1.5">
                    <CheckSquare className="w-3.5 h-3.5 text-emerald-400" /> Tasks
                  </h4>
                  <div className="space-y-1">
                    {results.tasks.map((t: any) => (
                      <button
                        key={t.id}
                        onClick={() => {
                          setSearchModalOpen(false);
                          navigate('/tasks');
                        }}
                        className="w-full text-left p-2.5 rounded-lg hover:bg-slate-800 flex items-center justify-between transition-colors"
                      >
                        <span className="text-sm font-medium text-slate-200">{t.title}</span>
                        <span className="text-xs px-2 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700">
                          {t.status}
                        </span>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* People */}
              {results.users?.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-2 flex items-center gap-1.5">
                    <Users className="w-3.5 h-3.5 text-amber-400" /> People
                  </h4>
                  <div className="space-y-1">
                    {results.users.map((u: any) => (
                      <div
                        key={u.id}
                        className="p-2.5 rounded-lg bg-slate-800/40 flex items-center justify-between"
                      >
                        <div>
                          <div className="text-sm font-medium text-slate-200">{u.fullName}</div>
                          <div className="text-xs text-slate-500 font-mono">{u.employeeCode} • {u.email}</div>
                        </div>
                        <span className="text-xs px-2 py-0.5 rounded bg-slate-800 text-slate-400">
                          {u.role}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Documents */}
              {results.documents?.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-2 flex items-center gap-1.5">
                    <FileText className="w-3.5 h-3.5 text-sky-400" /> Documents
                  </h4>
                  <div className="space-y-1">
                    {results.documents.map((d: any) => (
                      <button
                        key={d.id}
                        onClick={() => {
                          setSearchModalOpen(false);
                          navigate('/documents');
                        }}
                        className="w-full text-left p-2.5 rounded-lg hover:bg-slate-800 flex items-center justify-between transition-colors"
                      >
                        <span className="text-sm font-medium text-slate-200">{d.name}</span>
                        <span className="text-xs text-slate-500">{d.contentType}</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}

          {!loading && results && results.projects?.length === 0 && results.tasks?.length === 0 && results.users?.length === 0 && (
            <div className="text-center py-8 text-slate-500 text-sm">
              No results found for "{query}"
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
