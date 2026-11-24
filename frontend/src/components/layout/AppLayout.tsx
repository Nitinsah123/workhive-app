import React from 'react';
import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Header } from './Header';
import { GlobalSearchModal } from '../common/GlobalSearchModal';

export const AppLayout: React.FC = () => {
  return (
    <div className="min-h-screen bg-slate-950 flex overflow-x-hidden">
      {/* Fixed / Slide-over Drawer Sidebar */}
      <Sidebar />

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col lg:pl-64 pl-0 min-w-0 w-full overflow-x-hidden">
        <Header />
        <main className="flex-1 p-4 sm:p-6 lg:p-8 overflow-y-auto">
          <div className="max-w-7xl mx-auto w-full">
            <Outlet />
          </div>
        </main>
      </div>

      {/* Global Search Dialog */}
      <GlobalSearchModal />
    </div>
  );
};
