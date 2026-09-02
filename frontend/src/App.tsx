import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './store/authStore';
import { AppLayout } from './components/layout/AppLayout';

// Pages
import { LandingPage } from './pages/landing/LandingPage';
import { LoginPage } from './pages/auth/LoginPage';
import { CreateWorkspacePage } from './pages/auth/CreateWorkspacePage';
import { AcceptInvitationPage } from './pages/auth/AcceptInvitationPage';
import { DashboardPage } from './pages/dashboard/DashboardPage';
import { ActionCenterPage } from './pages/action-center/ActionCenterPage';
import { ProjectsPage } from './pages/projects/ProjectsPage';
import { ProjectWorkspacePage } from './pages/projects/ProjectWorkspacePage';
import { TasksPage } from './pages/tasks/TasksPage';
import { AttendancePage } from './pages/attendance/AttendancePage';
import { LeavePage } from './pages/leave/LeavePage';
import { DocumentsPage } from './pages/documents/DocumentsPage';
import { AnnouncementsPage } from './pages/announcements/AnnouncementsPage';
import { NotificationsPage } from './pages/notifications/NotificationsPage';
import { ReportsPage } from './pages/reports/ReportsPage';
import { IntegrationsPage } from './pages/integrations/IntegrationsPage';
import { ActivityFeedPage } from './pages/activity/ActivityFeedPage';
import { PeoplePage } from './pages/people/PeoplePage';
import { DepartmentsPage } from './pages/departments/DepartmentsPage';
import { TeamsPage } from './pages/teams/TeamsPage';
import { SettingsPage } from './pages/settings/SettingsPage';
import { ProfilePage } from './pages/profile/ProfilePage';

// Protected Route Guard
const ProtectedRoute: React.FC<{ children: React.ReactElement }> = ({ children }) => {
  const { isAuthenticated } = useAuthStore();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return children;
};

// Admin Guard
const AdminRoute: React.FC<{ children: React.ReactElement }> = ({ children }) => {
  const { user, isAuthenticated } = useAuthStore();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  if (user?.role !== 'TENANT_ADMIN' && user?.role !== 'MANAGER') {
    return <Navigate to="/dashboard" replace />;
  }
  return children;
};

export const App: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public Routes */}
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/create-workspace" element={<CreateWorkspacePage />} />
        <Route path="/accept-invitation" element={<AcceptInvitationPage />} />

        {/* Authenticated Workspace App Layout */}
        <Route
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route
            path="/action-center"
            element={
              <AdminRoute>
                <ActionCenterPage />
              </AdminRoute>
            }
          />
          <Route path="/projects" element={<ProjectsPage />} />
          <Route path="/projects/:id" element={<ProjectWorkspacePage />} />
          <Route path="/tasks" element={<TasksPage />} />
          <Route path="/attendance" element={<AttendancePage />} />
          <Route path="/leave" element={<LeavePage />} />
          <Route path="/documents" element={<DocumentsPage />} />
          <Route path="/announcements" element={<AnnouncementsPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/reports" element={<ReportsPage />} />
          <Route path="/integrations" element={<IntegrationsPage />} />
          <Route path="/activity" element={<ActivityFeedPage />} />
          <Route
            path="/people"
            element={
              <AdminRoute>
                <PeoplePage />
              </AdminRoute>
            }
          />
          <Route
            path="/departments"
            element={
              <AdminRoute>
                <DepartmentsPage />
              </AdminRoute>
            }
          />
          <Route
            path="/teams"
            element={
              <AdminRoute>
                <TeamsPage />
              </AdminRoute>
            }
          />
          <Route
            path="/settings"
            element={
              <AdminRoute>
                <SettingsPage />
              </AdminRoute>
            }
          />
          <Route path="/profile" element={<ProfilePage />} />
        </Route>

        {/* Catch-all Fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
};
