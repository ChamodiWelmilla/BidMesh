import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import { useState, useEffect, createContext } from 'react';
import Dashboard from './pages/Dashboard';
import AuctionList from './pages/AuctionList';
import AuctionDetail from './pages/AuctionDetail';
import Login from './pages/Login';
import Signup from './pages/Signup';
import ProfileSettingsModal from './components/ProfileSettingsModal';
import { Toaster } from 'react-hot-toast';

// JWT Auth Context
interface AuthContextType {
  token: string | null;
  role: string | null;
  email: string | null;
  displayName: string | null;
  isAuthenticated: boolean;
  login: (token: string) => void;
  logout: () => void;
  getAuthHeader: () => { Authorization?: string };
}

export const AuthContext = createContext<AuthContextType | null>(null);

function App() {
  const [token, setToken] = useState<string | null>(localStorage.getItem('jwtToken'));
  const [role, setRole] = useState<string | null>(null);
  const [email, setEmail] = useState<string | null>(null);
  const [displayName, setDisplayName] = useState<string | null>(null);
  const [showSettings, setShowSettings] = useState(false);

  useEffect(() => {
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        setRole(payload.role);
        setEmail(payload.sub); 
        setDisplayName(payload.displayName || payload.sub); 
        localStorage.setItem('jwtToken', token);
      } catch (e) {
        console.error("Invalid token format", e);
        logout();
      }
    } else {
      setRole(null);
      setEmail(null);
      setDisplayName(null);
      localStorage.removeItem('jwtToken');
    }
  }, [token]);

  const login = (newToken: string) => setToken(newToken);
  const logout = () => {
    setToken(null);
    window.location.href = '/login';
  };

  const getAuthHeader = () => {
    return token ? { Authorization: `Bearer ${token}` } : {};
  };

  const isAuthenticated = !!token;

  return (
    <AuthContext.Provider value={{ token, role, email, displayName, isAuthenticated, login, logout, getAuthHeader }}>
      <BrowserRouter>
        <Toaster position="top-right" />
        <header className="header">
          <div style={{display: 'flex', alignItems: 'center', gap: '2rem'}}>
            <Link to="/" className="logo">BidMesh</Link>
            {isAuthenticated && (
              <nav style={{display: 'flex', gap: '1.5rem'}}>
                <Link to="/auctions" style={{textDecoration: 'none', color: 'var(--secondary)', fontWeight: 500}}>Live Auctions</Link>
              </nav>
            )}
          </div>
          
          <div style={{display: 'flex', alignItems: 'center', gap: '1.5rem'}}>
            {isAuthenticated && (
              <>
                <div className="user-info">
                  <span style={{fontWeight: 'bold'}}>Hi {displayName}!</span>
                </div>
                <button 
                  onClick={() => setShowSettings(true)}
                  style={{ background: '#f1f5f9', border: 'none', color: 'var(--primary)', padding: '0.5rem', borderRadius: '50%', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                  title="Profile Settings"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                  </svg>
                </button>
                <button 
                  onClick={logout}
                  style={{ background: 'transparent', border: '1px solid var(--primary)', color: 'var(--primary)', padding: '0.25rem 0.75rem', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold' }}
                >
                  Logout
                </button>
              </>
            )}
          </div>
        </header>
        {showSettings && <ProfileSettingsModal onClose={() => setShowSettings(false)} />}
        <main className="container">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/auctions" element={<AuctionList />} />
            <Route path="/auction/:id" element={<AuctionDetail />} />
            <Route path="/login" element={<Login />} />
            <Route path="/signup" element={<Signup />} />
          </Routes>
        </main>
      </BrowserRouter>
    </AuthContext.Provider>
  );
}

export default App;
