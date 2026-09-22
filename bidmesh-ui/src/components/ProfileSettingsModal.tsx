import { useState, useContext } from 'react';
import { AuthContext } from '../App';
import toast from 'react-hot-toast';

export default function ProfileSettingsModal({ onClose }: { onClose: () => void }) {
  const auth = useContext(AuthContext);
  const [email, setEmail] = useState(auth?.email || '');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await fetch('http://localhost:9000/api/users/me', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          ...auth?.getAuthHeader()
        },
        body: JSON.stringify({ email, username, password })
      });
      
      if (!res.ok) {
        const err = await res.text();
        throw new Error(err || 'Failed to update profile');
      }
      
      const data = await res.json();
      
      if (data.token) {
        auth?.login(data.token);
      }
      
      toast.success('Profile updated successfully!');
      onClose();
    } catch (error: any) {
      toast.error(error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ position: 'fixed', top: 0, left: 0, width: '100%', height: '100%', backgroundColor: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(4px)', zIndex: 9999, display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '1rem' }}>
      <div className="card" style={{ width: '100%', maxWidth: '500px', background: 'white', borderRadius: '12px', padding: '2.5rem', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1)' }}>
         <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem'}}>
           <h2 style={{margin: 0, color: 'var(--primary)'}}>Profile Settings</h2>
           <button onClick={onClose} style={{background: 'none', border: 'none', fontSize: '2rem', cursor: 'pointer', color: 'var(--secondary)', lineHeight: '1rem'}}>&times;</button>
         </div>
         <form onSubmit={handleUpdate} autoComplete="off" style={{display: 'flex', flexDirection: 'column', gap: '1.25rem'}}>
           <div>
             <label style={{display: 'block', fontSize: '0.875rem', fontWeight: 'bold', marginBottom: '0.5rem'}}>Email Address</label>
             <input type="email" value={email} onChange={e => setEmail(e.target.value)} required style={{width: '100%', padding: '0.75rem', borderRadius: '0.375rem', border: '1px solid #ccc', boxSizing: 'border-box'}} autoComplete="off" />
           </div>
           <div>
             <label style={{display: 'block', fontSize: '0.875rem', fontWeight: 'bold', marginBottom: '0.5rem'}}>New Display Name</label>
             <input type="text" value={username} onChange={e => setUsername(e.target.value)} placeholder="Leave blank to keep current" style={{width: '100%', padding: '0.75rem', borderRadius: '0.375rem', border: '1px solid #ccc', boxSizing: 'border-box'}} autoComplete="off" />
           </div>
           <div>
             <label style={{display: 'block', fontSize: '0.875rem', fontWeight: 'bold', marginBottom: '0.5rem'}}>New Password</label>
             <input type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="Leave blank to keep current" style={{width: '100%', padding: '0.75rem', borderRadius: '0.375rem', border: '1px solid #ccc', boxSizing: 'border-box'}} autoComplete="new-password" />
           </div>
           <div style={{marginTop: '1.5rem'}}>
             <button type="submit" disabled={loading} className="btn btn-primary" style={{width: '100%', padding: '0.875rem', fontSize: '1.05rem', fontWeight: 'bold'}}>
               {loading ? 'Saving...' : 'Save Changes'}
             </button>
           </div>
         </form>
      </div>
    </div>
  );
}
