import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import { useState, useEffect, createContext, useContext } from 'react';
import Dashboard from './pages/Dashboard';
import AuctionList from './pages/AuctionList';
import AuctionDetail from './pages/AuctionDetail';

// Simple Auth Context for Role Simulation
interface AuthContextType {
  role: 'GUEST' | 'ADMIN' | 'USER';
  setRole: (role: 'GUEST' | 'ADMIN' | 'USER') => void;
  getAuthHeader: () => { Authorization?: string };
}

export const AuthContext = createContext<AuthContextType | null>(null);

function App() {
  const [role, setRole] = useState<'GUEST' | 'ADMIN' | 'USER'>('GUEST');

  const getAuthHeader = () => {
    if (role === 'ADMIN') return { Authorization: 'Basic ' + btoa('admin:password') };
    if (role === 'USER') return { Authorization: 'Basic ' + btoa('user1:password') };
    return {};
  };

  return (
    <AuthContext.Provider value={{ role, setRole, getAuthHeader }}>
      <BrowserRouter>
        <header className="header">
          <div style={{display: 'flex', alignItems: 'center', gap: '2rem'}}>
            <Link to="/" className="logo">BidMesh</Link>
            <nav style={{display: 'flex', gap: '1.5rem'}}>
              <Link to="/" style={{textDecoration: 'none', color: 'var(--secondary)', fontWeight: 500}}>Dashboard</Link>
              <Link to="/auctions" style={{textDecoration: 'none', color: 'var(--secondary)', fontWeight: 500}}>Live Auctions</Link>
            </nav>
          </div>
          
          <div style={{display: 'flex', alignItems: 'center', gap: '1.5rem'}}>
            <div className="role-simulator" style={{background: '#f1f5f9', padding: '0.25rem 0.5rem', borderRadius: '0.5rem', display: 'flex', gap: '0.5rem', alignItems: 'center'}}>
              <span style={{fontSize: '0.75rem', fontWeight: 'bold', color: '#64748b'}}>ROLE SIMULATOR:</span>
              <select 
                value={role} 
                onChange={(e) => setRole(e.target.value as any)}
                style={{border: 'none', background: 'transparent', fontWeight: 'bold', color: 'var(--primary)', cursor: 'pointer'}}
              >
                <option value="GUEST">Guest (View Only)</option>
                <option value="ADMIN">Admin (admin)</option>
                <option value="USER">User (user1)</option>
              </select>
            </div>
            <div className="user-info">Welcome, {role.toLowerCase()}</div>
          </div>
        </header>
        <main className="container">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/auctions" element={<AuctionList />} />
            <Route path="/auction/:id" element={<AuctionDetail />} />
          </Routes>
        </main>
      </BrowserRouter>
    </AuthContext.Provider>
  );
}

export default App;
