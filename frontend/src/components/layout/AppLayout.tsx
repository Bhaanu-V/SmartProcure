import React from 'react';
import { Outlet } from 'react-router-dom';
import { Navbar } from './Navbar';
import { Sidebar } from './Sidebar';

interface AppLayoutProps {
  user?: {
    username: string;
    email: string;
    role: string;
    departmentName?: string;
  } | null;
  onLogout?: () => void;
}

export const AppLayout: React.FC<AppLayoutProps> = ({ user, onLogout }) => {
  return (
    <div className="min-h-screen flex flex-col bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 transition-colors">
      <Navbar user={user} onLogout={onLogout} />
      <div className="flex-1 flex overflow-hidden">
        <Sidebar userRole={user?.role} />
        <main className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
          <div className="max-w-7xl mx-auto space-y-6">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};
