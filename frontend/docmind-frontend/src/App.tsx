import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { useDocumentStore } from './store/documentStore';
import { useConversationStore } from './store/conversationStore';
import { useAuthStore } from './store/authStore';
import Navbar from './components/Navbar';
import Sidebar from './components/Sidebar';
import ChatView from './components/ChatView';
import SearchView from './components/SearchView';
import ChunksView from './components/ChunksView';
import UploadDialog from './components/UploadDialog';
import ProtectedRoute from './components/ProtectedRoute';
import { ErrorBoundary } from './components/ErrorBoundary';
import Login from './pages/Login';
import Register from './pages/Register';

import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { useDocumentStore } from './store/documentStore';
import { useConversationStore } from './store/conversationStore';
import { useAuthStore } from './store/authStore';
import Navbar from './components/Navbar';
import Sidebar from './components/Sidebar';
import ChatView from './components/ChatView';
import SearchView from './components/SearchView';
import ChunksView from './components/ChunksView';
import UploadDialog from './components/UploadDialog';
import ProtectedRoute from './components/ProtectedRoute';
import { ErrorBoundary } from './components/ErrorBoundary';
import Login from './pages/Login';
import Register from './pages/Register';

// Cold-start banner: shown while backend is waking up (Render free tier)
const ColdStartBanner: React.FC<{ visible: boolean }> = ({ visible }) => {
  if (!visible) return null;
  return (
    <div className="flex items-center justify-center gap-2.5 px-4 py-2 bg-amber-500/10 border-b border-amber-500/20 text-amber-300 text-xs">
      <svg className="w-3.5 h-3.5 animate-spin flex-shrink-0" fill="none" viewBox="0 0 24 24">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
      </svg>
      <span>
        ☕ <strong>Server is waking up</strong> from free-tier sleep — this may take 30–60 seconds on first load. Thank you for your patience!
      </span>
    </div>
  );
};

const Dashboard: React.FC = () => {
  const { activeTab, fetchDocuments, isBackendAwake } = useDocumentStore();
  const { loadChats } = useConversationStore();
  const token = useAuthStore((state) => state.token);
  const user = useAuthStore((state) => state.user);
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [showColdStartBanner, setShowColdStartBanner] = useState(false);

  useEffect(() => {
    if (token && user) {
      // Show cold-start banner after 3 seconds if backend hasn't responded yet
      const bannerTimer = setTimeout(() => {
        if (!isBackendAwake) setShowColdStartBanner(true);
      }, 3000);
      fetchDocuments();
      loadChats();
      return () => clearTimeout(bannerTimer);
    }
  }, [token, user, fetchDocuments, loadChats]);

  // Hide banner once backend responds
  useEffect(() => {
    if (isBackendAwake) setShowColdStartBanner(false);
  }, [isBackendAwake]);

  return (
    <div className="flex flex-col h-screen overflow-hidden bg-[#0f172a]">
      <Navbar onUploadClick={() => setIsUploadOpen(true)} />
      <ColdStartBanner visible={showColdStartBanner} />

      <div className="flex flex-1 overflow-hidden">
        <Sidebar onUploadClick={() => setIsUploadOpen(true)} />

        {/* Main content */}
        <main className="flex-1 overflow-hidden">
          {activeTab === 'chat' && <ChatView />}
          {activeTab === 'search' && <SearchView />}
          {activeTab === 'chunks' && <ChunksView />}
        </main>
      </div>

      <UploadDialog isOpen={isUploadOpen} onClose={() => setIsUploadOpen(false)} />
    </div>
  );
};

const App: React.FC = () => {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <Routes>
        {/* Public Routes */}
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        {/* Protected Application Routes */}
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<Dashboard />} />
          <Route path="/*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>

      <Toaster
        position="bottom-right"
        toastOptions={{
          style: {
            background: '#1e293b',
            color: '#f1f5f9',
            border: '1px solid #334155',
            borderRadius: '12px',
            fontSize: '14px',
          },
          success: {
            iconTheme: { primary: '#22c55e', secondary: '#1e293b' },
          },
          error: {
            iconTheme: { primary: '#ef4444', secondary: '#1e293b' },
          },
        }}
      />
    </BrowserRouter>
  </ErrorBoundary>
  );
};

export default App;
