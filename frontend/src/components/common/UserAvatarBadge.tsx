import React from 'react';

export function getInitials(fullName?: string | null, fallback = '?'): string {
  if (!fullName || !fullName.trim()) return fallback;
  const parts = fullName.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase();
  }
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

export function formatSystemRole(role?: string | null): string {
  if (!role) return 'Employee';
  const r = role.toUpperCase();
  if (r === 'TENANT_ADMIN' || r === 'ADMIN' || r === 'SUPER_ADMIN') return 'Admin';
  if (r === 'MANAGER') return 'Manager';
  return 'Employee';
}

interface UserAvatarProps {
  name?: string | null;
  avatarUrl?: string | null;
  size?: 'xs' | 'sm' | 'md';
  fallback?: string;
}

export const UserAvatar: React.FC<UserAvatarProps> = ({
  name,
  avatarUrl,
  size = 'sm',
  fallback = '?',
}) => {
  const sizeClass =
    size === 'xs'
      ? 'w-5 h-5 text-[9px] rounded-md'
      : size === 'sm'
      ? 'w-6 h-6 text-[10px] rounded-lg'
      : 'w-10 h-10 text-xs rounded-xl';

  if (avatarUrl) {
    return (
      <img
        src={avatarUrl}
        alt={name || 'User'}
        className={`${sizeClass} object-cover border border-indigo-500/30 shrink-0`}
      />
    );
  }

  return (
    <div
      className={`${sizeClass} bg-indigo-500/15 border border-indigo-500/30 flex items-center justify-center text-indigo-300 font-bold shrink-0`}
    >
      {getInitials(name, fallback)}
    </div>
  );
};
