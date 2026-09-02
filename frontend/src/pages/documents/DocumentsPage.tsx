import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { documentApi } from '../../api/services';
import api from '../../api/client';
import { Modal } from '../../components/common/Modal';
import {
  FileText,
  Upload,
  Download,
  Trash2,
  File,
  Sparkles,
  Lock,
  Eye,
  CheckCircle,
  AlertCircle,
  FileCode,
  Image as ImageIcon,
} from 'lucide-react';

export const DocumentsPage: React.FC = () => {
  const [uploadModalOpen, setUploadModalOpen] = useState(false);
  const [previewModalOpen, setPreviewModalOpen] = useState(false);
  const [previewDoc, setPreviewDoc] = useState<any | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [entityType, setEntityType] = useState('ORGANIZATION');
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  const queryClient = useQueryClient();

  const { data: docsData, isLoading } = useQuery({
    queryKey: ['documents-list'],
    queryFn: () => documentApi.getAll(),
  });

  const uploadMutation = useMutation({
    mutationFn: (formData: FormData) => documentApi.upload(formData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents-list'] });
      setUploadModalOpen(false);
      setFile(null);
      setName('');
      setDescription('');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => documentApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents-list'] });
    },
  });

  const handleUpload = (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);
    if (name) formData.append('name', name);
    if (description) formData.append('description', description);
    formData.append('entityType', entityType);

    uploadMutation.mutate(formData);
  };

  const handleDownload = async (docId: string, originalName: string) => {
    setDownloadingId(docId);
    try {
      const response = await api.get(`/documents/${docId}/download`, {
        responseType: 'blob',
      });
      const blobType = (response.headers['content-type'] as string) || 'application/octet-stream';
      const blob = new Blob([response.data], { type: blobType });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = originalName || 'document';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Failed to download document', err);
      alert('Error downloading document. Please verify your connection.');
    } finally {
      setDownloadingId(null);
    }
  };

  const docs = docsData?.data?.content || [];

  const getFileIcon = (contentType?: string) => {
    if (contentType?.includes('image')) return ImageIcon;
    if (contentType?.includes('pdf') || contentType?.includes('word') || contentType?.includes('text')) return FileText;
    return File;
  };

  return (
    <div className="space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider border border-indigo-500/20 mb-2">
            <FileText className="w-3.5 h-3.5" />
            Storage & Repository
          </div>
          <h1 className="text-3xl font-black font-display text-white">Documents</h1>
          <p className="text-slate-400 text-sm">
            Encrypted object storage with tenant isolation, streaming downloads, and entity attachments.
          </p>
        </div>

        <button
          onClick={() => setUploadModalOpen(true)}
          className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs shadow-lg shadow-indigo-600/25 flex items-center gap-2 transition-all self-start sm:self-auto"
        >
          <Upload className="w-4 h-4" />
          <span>Upload File</span>
        </button>
      </div>

      {/* Document Grid */}
      {isLoading ? (
        <div className="text-center py-20 text-slate-500 text-sm">Loading documents...</div>
      ) : docs.length === 0 ? (
        <div className="glass-panel p-12 text-center rounded-3xl border border-slate-800">
          <FileText className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <h3 className="text-lg font-bold text-white font-display">No Documents Stored</h3>
          <p className="text-sm text-slate-400 mt-1">Upload organization or project files to get started.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {docs.map((doc: any) => {
            const Icon = getFileIcon(doc.contentType);
            return (
              <div
                key={doc.id}
                className="glass-panel glass-card-hover p-6 rounded-3xl border border-slate-800 flex flex-col justify-between"
              >
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="p-2.5 rounded-xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                      <Icon className="w-5 h-5" />
                    </div>
                    <span className="text-xs px-2 py-0.5 rounded bg-slate-800 text-slate-400 font-mono">
                      v{doc.version || 1} • {doc.entityType || 'ORG'}
                    </span>
                  </div>

                  <h3 className="text-base font-bold text-white font-display truncate">{doc.name}</h3>
                  <p className="text-xs text-slate-400 line-clamp-2">
                    {doc.description || 'No description provided.'}
                  </p>
                  <div className="text-[10px] text-slate-500 font-mono">
                    {doc.fileSize ? `${Math.round(doc.fileSize / 1024)} KB` : ''} • {doc.contentType || 'binary'}
                  </div>
                </div>

                <div className="mt-6 pt-4 border-t border-slate-800/80 flex items-center justify-between">
                  <button
                    onClick={() => handleDownload(doc.id, doc.originalName || doc.name)}
                    disabled={downloadingId === doc.id}
                    className="px-3 py-1.5 rounded-lg bg-indigo-600/20 hover:bg-indigo-600/40 text-indigo-300 text-xs font-bold flex items-center gap-1.5 transition-colors disabled:opacity-50"
                  >
                    <Download className="w-3.5 h-3.5" />
                    <span>{downloadingId === doc.id ? 'Downloading...' : 'Download'}</span>
                  </button>

                  <button
                    onClick={() => {
                      if (window.confirm('Delete this document from storage?')) {
                        deleteMutation.mutate(doc.id);
                      }
                    }}
                    className="p-1.5 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
                    title="Delete document"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Upload Document Modal */}
      <Modal isOpen={uploadModalOpen} onClose={() => setUploadModalOpen(false)} title="Upload Document">
        <form onSubmit={handleUpload} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Select File *
            </label>
            <input
              type="file"
              required
              onChange={(e) => {
                if (e.target.files && e.target.files[0]) {
                  setFile(e.target.files[0]);
                  if (!name) setName(e.target.files[0].name);
                }
              }}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-xs file:font-semibold file:bg-indigo-600 file:text-white hover:file:bg-indigo-500"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Document Display Name
            </label>
            <input
              type="text"
              placeholder="e.g. Employee Handbook 2026"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Scope / Entity Type
            </label>
            <select
              value={entityType}
              onChange={(e) => setEntityType(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            >
              <option value="ORGANIZATION">Organization Wide</option>
              <option value="DEPARTMENT">Department</option>
              <option value="PROJECT">Project</option>
              <option value="TASK">Task Attachment</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Description
            </label>
            <textarea
              rows={3}
              placeholder="Summary of contents..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-700 text-white text-sm"
            />
          </div>

          <div className="flex justify-end gap-3 pt-3">
            <button
              type="button"
              onClick={() => setUploadModalOpen(false)}
              className="px-4 py-2.5 rounded-xl bg-slate-800 text-slate-300 text-xs font-bold"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={!file || uploadMutation.isPending}
              className="px-5 py-2.5 rounded-xl bg-indigo-600 text-white text-xs font-bold shadow-lg shadow-indigo-600/25 disabled:opacity-50"
            >
              {uploadMutation.isPending ? 'Uploading...' : 'Upload Document'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
