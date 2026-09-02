import { create } from 'zustand';
import { AuthResponse, Role } from '../types';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: {
    id: string;
    email: string;
    fullName: string;
    role: Role;
    employeeCode: string;
    avatarUrl?: string;
  } | null;
  tenant: {
    id: string;
    name: string;
    code: string;
    logoUrl?: string;
    timezone: string;
  } | null;
  isAuthenticated: boolean;
  setAuth: (data: AuthResponse) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => {
  const savedAccess = localStorage.getItem('workhive_access_token');
  const savedRefresh = localStorage.getItem('workhive_refresh_token');
  const savedUser = localStorage.getItem('workhive_user');
  const savedTenant = localStorage.getItem('workhive_tenant');

  return {
    accessToken: savedAccess,
    refreshToken: savedRefresh,
    user: savedUser ? JSON.parse(savedUser) : null,
    tenant: savedTenant ? JSON.parse(savedTenant) : null,
    isAuthenticated: !!savedAccess,

    setAuth: (data) => {
      localStorage.setItem('workhive_access_token', data.accessToken);
      localStorage.setItem('workhive_refresh_token', data.refreshToken);
      localStorage.setItem('workhive_user', JSON.stringify(data.user));
      localStorage.setItem('workhive_tenant', JSON.stringify(data.tenant));
      set({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        user: data.user,
        tenant: data.tenant,
        isAuthenticated: true,
      });
    },

    logout: () => {
      localStorage.removeItem('workhive_access_token');
      localStorage.removeItem('workhive_refresh_token');
      localStorage.removeItem('workhive_user');
      localStorage.removeItem('workhive_tenant');
      set({
        accessToken: null,
        refreshToken: null,
        user: null,
        tenant: null,
        isAuthenticated: false,
      });
    },
  };
});
