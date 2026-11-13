import { create } from 'zustand';
import type { DocumentMetadataDto } from '../types';
import { documentApi } from '../services/api';
import toast from 'react-hot-toast';

interface DocumentState {
  documents: DocumentMetadataDto[];
  selectedDocumentId: string | null;
  isLoadingDocuments: boolean;
  activeTab: 'chat' | 'search' | 'chunks';
  isSidebarOpen: boolean;
  isBackendAwake: boolean; // tracks whether backend has responded

  setSelectedDocumentId: (id: string | null) => void;
  setActiveTab: (tab: 'chat' | 'search' | 'chunks') => void;
  setIsSidebarOpen: (v: boolean) => void;
  setIsBackendAwake: (v: boolean) => void;
  fetchDocuments: () => Promise<void>;
  pollDocumentsUntilIndexed: () => void;
  deleteDocument: (id: string) => Promise<void>;
  reset: () => void;
}

const initialState = {
  documents: [] as DocumentMetadataDto[],
  selectedDocumentId: null as string | null,
  isLoadingDocuments: false,
  activeTab: 'chat' as const,
  isSidebarOpen: true,
  isBackendAwake: false,
};

export const useDocumentStore = create<DocumentState>((set, get) => ({
  ...initialState,

  setSelectedDocumentId: (id: string | null) => set({ selectedDocumentId: id }),
  setActiveTab: (tab: 'chat' | 'search' | 'chunks') => set({ activeTab: tab }),
  setIsSidebarOpen: (v: boolean) => set({ isSidebarOpen: v }),
  setIsBackendAwake: (v: boolean) => set({ isBackendAwake: v }),

  fetchDocuments: async () => {
    set({ isLoadingDocuments: true });
    try {
      const resp = await documentApi.getMine();
      if (resp.success) {
        set({ documents: resp.data, isBackendAwake: true });
      }
    } catch (err) {
      console.error('Failed to fetch user documents', err);
    } finally {
      set({ isLoadingDocuments: false });
    }
  },

  // Poll every 5 seconds while any document is in PROCESSING state
  pollDocumentsUntilIndexed: () => {
    const intervalId = setInterval(async () => {
      const { documents } = get();
      const hasProcessing = documents.some(
        (d) => d.status === 'PROCESSING' || d.status === 'processing'
      );
      if (!hasProcessing) {
        clearInterval(intervalId);
        return;
      }
      try {
        const resp = await documentApi.getMine();
        if (resp.success) {
          set({ documents: resp.data });
          const stillProcessing = resp.data.some(
            (d) => d.status === 'PROCESSING' || d.status === 'processing'
          );
          if (!stillProcessing) {
            clearInterval(intervalId);
            toast.success('All documents indexed and ready!');
          }
        }
      } catch {
        clearInterval(intervalId);
      }
    }, 5000);

    // Safety: stop polling after 3 minutes
    setTimeout(() => clearInterval(intervalId), 180_000);
  },

  deleteDocument: async (id: string) => {
    try {
      await documentApi.delete(id);
      set((state) => ({
        documents: state.documents.filter((d) => d.id !== id),
        selectedDocumentId: state.selectedDocumentId === id ? null : state.selectedDocumentId,
      }));
      toast.success('Document deleted successfully');
    } catch {
      toast.error('Failed to delete document');
    }
  },

  reset: () => {
    set({
      ...initialState,
    });
  },
}));
