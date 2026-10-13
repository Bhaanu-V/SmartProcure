import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  FileText,
  PlusCircle,
  CheckSquare,
  ShoppingCart,
  Building2,
  DollarSign,
  Sparkles,
} from 'lucide-react';

interface SidebarProps {
  userRole?: string;
}

export const Sidebar: React.FC<SidebarProps> = ({ userRole }) => {
  const isApprover =
    userRole === 'ROLE_MANAGER' ||
    userRole === 'ROLE_FINANCE' ||
    userRole === 'ROLE_ADMIN' ||
    userRole === 'MANAGER' ||
    userRole === 'FINANCE' ||
    userRole === 'ADMIN';

  const isProcurementOrAdmin =
    userRole === 'ROLE_PROCUREMENT' ||
    userRole === 'ROLE_ADMIN' ||
    userRole === 'PROCUREMENT' ||
    userRole === 'ADMIN';

  const navItems = [
    {
      label: 'Executive Dashboard',
      path: '/',
      icon: LayoutDashboard,
      visible: true,
    },
    {
      label: 'Purchase Requests',
      path: '/requests',
      icon: FileText,
      visible: true,
    },
    {
      label: 'Create Requisition',
      path: '/requests/new',
      icon: PlusCircle,
      visible: true,
    },
    {
      label: 'Approval Queue',
      path: '/approvals',
      icon: CheckSquare,
      visible: isApprover,
      highlight: true,
    },
    {
      label: 'Purchase Orders',
      path: '/orders',
      icon: ShoppingCart,
      visible: true,
    },
    {
      label: 'Suppliers & Catalog',
      path: '/suppliers',
      icon: Building2,
      visible: isProcurementOrAdmin,
    },
    {
      label: 'Department Budgets',
      path: '/budgets',
      icon: DollarSign,
      visible: isApprover,
    },
    {
      label: 'AI Risk Advisory',
      path: '/ai-status',
      icon: Sparkles,
      visible: true,
    },
  ];

  return (
    <aside className="w-64 bg-white dark:bg-slate-900 border-r border-slate-200 dark:border-slate-800 flex flex-col shrink-0 min-h-[calc(100vh-4rem)]">
      <div className="p-4 flex-1 space-y-1">
        <div className="px-3 py-2 text-[11px] font-semibold uppercase tracking-wider text-slate-400 dark:text-slate-500">
          Main Navigation
        </div>
        {navItems
          .filter((item) => item.visible)
          .map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-blue-50 text-blue-700 dark:bg-blue-950/60 dark:text-blue-300 shadow-xs'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100 dark:text-slate-400 dark:hover:text-slate-200 dark:hover:bg-slate-800/60'
                  }`
                }
              >
                <Icon className="w-4 h-4 shrink-0" />
                <span className="truncate">{item.label}</span>
                {item.highlight && (
                  <span className="ml-auto w-2 h-2 rounded-full bg-amber-500" />
                )}
              </NavLink>
            );
          })}
      </div>

      {/* Compliance footer info */}
      <div className="p-4 border-t border-slate-200 dark:border-slate-800 text-[11px] text-slate-400 dark:text-slate-500 space-y-1">
        <p className="font-medium text-slate-600 dark:text-slate-400">SmartProcure v1.0.0</p>
        <p>PostgreSQL 18 & Gemini AI</p>
        <p className="text-[10px] text-slate-400">Human-In-The-Loop Certified</p>
      </div>
    </aside>
  );
};
