import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { userApi } from '../../api/services';
import { Lock, User, Phone, ArrowRight, AlertCircle, Building, Users2, Shield, CheckCircle2 } from 'lucide-react';

export const AcceptInvitationPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';

  const [invitationDetails, setInvitationDetails] = useState<any | null>(null);
  const [checkingToken, setCheckingToken] = useState(true);
  const [fullName, setFullName] = useState('');
  const [password, setPassword] = useState('');
  const [phone, setPhone] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { setAuth } = useAuthStore();
  const navigate = useNavigate();

  useEffect(() => {
    if (!token) {
      setError('Invalid or missing invitation token. Please check your invitation link.');
      setCheckingToken(false);
      return;
    }

    userApi.getInvitationDetails(token)
      .then((res) => {
        setInvitationDetails(res.data);
        if (res.data.name) {
          setFullName(res.data.name);
        }
        if (!res.data.valid) {
          setError(res.data.message || 'Invitation is invalid, expired, or already used.');
        }
      })
      .catch(() => {
        setError('Could not verify invitation token. It may be invalid or expired.');
      })
      .finally(() => {
        setCheckingToken(false);
      });
  }, [token]);

  const handleAccept = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token) {
      setError('Invalid or missing invitation token');
      return;
    }
    setError(null);
    setLoading(true);

    try {
      const res = await userApi.acceptInvitation({
        token,
        fullName,
        password,
        phone,
      });

      setAuth(res.data);
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to accept invitation. Token may be expired or already used.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <Link to="/" className="inline-flex items-center gap-3 mb-4">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 flex items-center justify-center text-white font-black text-xl shadow-lg shadow-indigo-500/25">
            W
          </div>
          <span className="font-display font-black text-2xl tracking-tight text-white">
            WorkHive
          </span>
        </Link>
        <h2 className="text-2xl font-bold font-display text-white">
          {invitationDetails?.tenantName ? `Join ${invitationDetails.tenantName}` : 'Accept Workspace Invitation'}
        </h2>
        <p className="mt-2 text-sm text-slate-400">
          Set up your employee profile and password to enter your workspace.
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        {checkingToken ? (
          <div className="glass-panel p-8 rounded-3xl text-center text-slate-400 text-sm">
            Verifying your invitation...
          </div>
        ) : (
          <div className="glass-panel p-8 rounded-3xl shadow-2xl border border-slate-800 space-y-6">
            {/* Org Placement preview if valid */}
            {invitationDetails?.valid && (
              <div className="p-4 rounded-2xl bg-slate-900/90 border border-slate-800 space-y-2">
                <div className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1">
                  Invitation Details
                </div>
                <div className="flex flex-wrap gap-2 pt-1">
                  <span className="text-xs px-2.5 py-1 rounded-lg font-bold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 flex items-center gap-1">
                    <Shield className="w-3 h-3" />
                    {invitationDetails.role}
                  </span>
                  {invitationDetails.departmentName && (
                    <span className="text-xs px-2.5 py-1 rounded-lg font-bold bg-violet-500/10 text-violet-400 border border-violet-500/20 flex items-center gap-1">
                      <Building className="w-3 h-3" />
                      {invitationDetails.departmentName}
                    </span>
                  )}
                  {invitationDetails.teamName && (
                    <span className="text-xs px-2.5 py-1 rounded-lg font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center gap-1">
                      <Users2 className="w-3 h-3" />
                      {invitationDetails.teamName}
                    </span>
                  )}
                </div>
                <div className="text-xs text-slate-400 pt-1">
                  Recipient Email: <span className="text-white font-mono">{invitationDetails.email}</span>
                </div>
              </div>
            )}

            {error && (
              <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm flex items-center gap-3">
                <AlertCircle className="w-5 h-5 flex-shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {(!invitationDetails || invitationDetails.valid) && (
              <form onSubmit={handleAccept} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                    Your Full Name *
                  </label>
                  <div className="relative">
                    <User className="w-5 h-5 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      required
                      placeholder="John Doe"
                      value={fullName}
                      onChange={(e) => setFullName(e.target.value)}
                      className="w-full pl-11 pr-4 py-3 rounded-xl bg-slate-900/80 border border-slate-700/80 text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 text-sm"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                    Set Password * (8+ chars)
                  </label>
                  <div className="relative">
                    <Lock className="w-5 h-5 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type="password"
                      required
                      minLength={8}
                      placeholder="••••••••"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      className="w-full pl-11 pr-4 py-3 rounded-xl bg-slate-900/80 border border-slate-700/80 text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 text-sm"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                    Phone (Optional)
                  </label>
                  <div className="relative">
                    <Phone className="w-5 h-5 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type="tel"
                      placeholder="+1 555-0199"
                      value={phone}
                      onChange={(e) => setPhone(e.target.value)}
                      className="w-full pl-11 pr-4 py-3 rounded-xl bg-slate-900/80 border border-slate-700/80 text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 text-sm"
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full mt-4 py-3.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white font-bold text-sm shadow-lg shadow-indigo-600/30 transition-all flex items-center justify-center gap-2"
                >
                  {loading ? 'Activating Account...' : 'Join Workspace & Login'}
                  <ArrowRight className="w-4 h-4" />
                </button>
              </form>
            )}

            {invitationDetails && !invitationDetails.valid && (
              <div className="text-center pt-2">
                <Link
                  to="/login"
                  className="inline-block px-5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 font-bold text-xs border border-slate-700 transition-all"
                >
                  Go to Login Page &rarr;
                </Link>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
