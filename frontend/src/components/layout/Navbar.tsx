import React from 'react';
import { Sparkles, LogOut, User as UserIcon, Shield, Bell } from 'lucide-react';
import { Badge } from '../common/Badge';

interface NavbarProps {
  user?: {
    username: string;
    email: string;
    role: string;
    departmentName?: string;
  } | null;
  onLogout?: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ user, onLogout }) => {
  const getRoleBadgeVariant = (role?: string) => {
    switch (role) {
      case 'ROLE_ADMIN':
      case 'ADMIN':
        return 'danger';
      case 'ROLE_FINANCE':
      case 'FINANCE':
        return 'success';
      case 'ROLE_MANAGER':
      case 'MANAGER':
        return 'primary';
      case 'ROLE_PROCUREMENT':
      case 'PROCUREMENT':
        return 'purple';
      default:
        return 'default';
    }
  };

  const formatRole = (role?: string) => {
    if (!role) return 'USER';
    return role.replace('ROLE_', '');
  };

  return (
    <header className="sticky top-0 z-30 h-16 bg-white/90 dark:bg-slate-900/90 backdrop-blur-md border-b border-slate-200 dark:border-slate-800 transition-colors">
      <div className="h-full px-4 sm:px-6 lg:px-8 flex items-center justify-between">
        {/* Left branding */}
        <div className="flex items-center gap-3">
          <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 text-white shadow-md shadow-blue-500/20">
            <Shield className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-lg tracking-tight bg-gradient-to-r from-slate-900 via-blue-950 to-indigo-900 dark:from-white dark:via-slate-100 dark:to-blue-200 bg-clip-text text-transparent">
                SmartProcure
              </span>
              <span className="inline-flex items-center gap-1 text-[10px] font-semibold tracking-wider uppercase px-2 py-0.5 rounded-md bg-blue-100 dark:bg-blue-950 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
                <Sparkles className="w-3 h-3 text-blue-500 animate-pulse" />
                AI Enhanced
              </span>
            </div>
            <p className="text-[11px] text-slate-500 dark:text-slate-400 hidden sm:block">
              Intelligent Purchase Order & Workflow Management
            </p>
          </div>
        </div>

        {/* Right actions & user profile */}
        <div className="flex items-center gap-4">
          <button
            title="System notifications"
            className="p-2 rounded-lg text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
          >
            <Bell className="w-5 h-5" />
          </button>

          {user ? (
            <div className="flex items-center gap-3 pl-3 border-l border-slate-200 dark:border-slate-800">
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-full bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 flex items-center justify-center text-slate-700 dark:text-slate-200 font-medium text-sm">
                  <UserIcon className="w-4 h-4 text-slate-500" />
                </div>
                <div className="hidden md:block text-left">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                      {user.username}
                    </span>
                    <Badge variant={getRoleBadgeVariant(user.role)}>
                      {formatRole(user.role)}
                    </Badge>
                  </div>
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    {user.departmentName || user.email}
                  </p>
                </div>
              </div>

              {onLogout && (
                <button
                  onClick={onLogout}
                  title="Sign out of SmartProcure"
                  className="p-2 rounded-lg text-slate-500 hover:text-rose-600 hover:bg-rose-50 dark:text-slate-400 dark:hover:bg-rose-950/30 transition cursor-pointer"
                >
                  <LogOut className="w-4 h-4" />
                </button>
              )}
            </div>
          ) : (
            <div className="text-xs text-slate-500">Guest Access</div>
          )}
        </div>
      </div>
    </header>
  );
};
