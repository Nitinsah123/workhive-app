import React from 'react';
import { Link } from 'react-router-dom';
import {
  ShieldCheck,
  Zap,
  FolderKanban,
  CheckSquare,
  Clock,
  Calendar,
  Lock,
  GitBranch,
  Building,
  ArrowRight,
  Sparkles,
  BarChart3,
  Layers,
} from 'lucide-react';

export const LandingPage: React.FC = () => {
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col selection:bg-indigo-500 selection:text-white">
      {/* Top Navbar */}
      <header className="h-20 border-b border-slate-800/80 px-8 flex items-center justify-between max-w-7xl mx-auto w-full">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 flex items-center justify-center text-white font-black text-xl shadow-lg shadow-indigo-500/25">
            W
          </div>
          <span className="font-display font-black text-2xl tracking-tight text-white">
            WorkHive
          </span>
        </div>

        <div className="flex items-center gap-4">
          <Link
            to="/login"
            className="px-5 py-2.5 rounded-xl text-sm font-semibold text-slate-300 hover:text-white hover:bg-slate-900 transition-all"
          >
            Sign In
          </Link>
          <Link
            to="/create-workspace"
            className="px-5 py-2.5 rounded-xl text-sm font-semibold bg-indigo-600 hover:bg-indigo-500 text-white shadow-lg shadow-indigo-600/30 transition-all flex items-center gap-2"
          >
            <span>Create Workspace</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      </header>

      {/* Hero Section */}
      <section className="flex-1 flex flex-col items-center justify-center text-center px-6 py-20 max-w-5xl mx-auto">
        <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 text-xs font-semibold uppercase tracking-wider mb-8">
          <Sparkles className="w-4 h-4" />
          Clean-Slate Multi-Tenant B2B SaaS Platform
        </div>

        <h1 className="font-display font-black text-5xl sm:text-6xl md:text-7xl tracking-tight text-white leading-tight max-w-4xl">
          The Operating System for <span className="gradient-text">Modern Enterprise Teams</span>
        </h1>

        <p className="mt-6 text-lg sm:text-xl text-slate-400 max-w-2xl font-normal leading-relaxed">
          Isolated workspaces, projects, kanban, attendance, leaves, generalized submission hub, documents, and real-time workflows — all built for high-performance organizations.
        </p>

        <div className="mt-10 flex flex-col sm:flex-row items-center gap-4">
          <Link
            to="/create-workspace"
            className="w-full sm:w-auto px-8 py-4 rounded-2xl bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 text-white font-bold text-base shadow-xl shadow-indigo-500/25 transition-all flex items-center justify-center gap-3"
          >
            <span>Launch Your Workspace</span>
            <ArrowRight className="w-5 h-5" />
          </Link>
          <Link
            to="/login"
            className="w-full sm:w-auto px-8 py-4 rounded-2xl bg-slate-900 border border-slate-800 hover:border-slate-700 text-slate-300 hover:text-white font-semibold text-base transition-all"
          >
            Employee Sign In
          </Link>
        </div>

        {/* Feature Grid */}
        <div className="mt-24 grid grid-cols-1 md:grid-cols-3 gap-6 text-left w-full">
          <div className="glass-panel p-6 rounded-2xl">
            <div className="p-3 w-fit rounded-xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 mb-4">
              <ShieldCheck className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-bold text-white font-display">Strict Tenant Isolation</h3>
            <p className="mt-2 text-sm text-slate-400">
              Every organization is an isolated fortress. No super admin, no data leakage, thread-local TenantContext security.
            </p>
          </div>

          <div className="glass-panel p-6 rounded-2xl">
            <div className="p-3 w-fit rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 mb-4">
              <Layers className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-bold text-white font-display">Generalized Action Center</h3>
            <p className="mt-2 text-sm text-slate-400">
              One unified hub for reviewing leave applications, task approval submissions, and document workflows.
            </p>
          </div>

          <div className="glass-panel p-6 rounded-2xl">
            <div className="p-3 w-fit rounded-xl bg-violet-500/10 text-violet-400 border border-violet-500/20 mb-4">
              <Zap className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-bold text-white font-display">Real-Time STOMP & Outbox</h3>
            <p className="mt-2 text-sm text-slate-400">
              Live updates without refreshing. Transactional outbox guarantees reliable notifications and events.
            </p>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-slate-900 py-8 text-center text-xs text-slate-600">
        WorkHive SaaS Platform © 2026. Built with React, TypeScript, Spring Boot & PostgreSQL.
      </footer>
    </div>
  );
};
