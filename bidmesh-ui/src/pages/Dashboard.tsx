import { useState, useEffect, useContext } from 'react';
import { Link } from 'react-router-dom';
import { AuthContext } from '../App';

interface DashboardStats {
  activeAuctions: number;
  totalBids: number;
  totalUsers: number;
}

interface Auction {
  id: number;
  item: {
    name: string;
  };
  currentPrice: number;
}

const Dashboard = () => {
  const auth = useContext(AuthContext);
  const [stats, setStats] = useState<DashboardStats>({ activeAuctions: 0, totalBids: 0, totalUsers: 0 });
  const [recentAuctions, setRecentAuctions] = useState<Auction[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateForm, setShowCreateForm] = useState(false);

  // Form State
  const [startPrice, setStartPrice] = useState('100');
  const [itemId, setItemId] = useState('1');

  const fetchDashboard = () => {
    fetch('http://localhost:9000/api/auctions/all')
      .then(res => res.json())
      .then(data => {
        const auctions = Array.isArray(data) ? data : [data];
        setRecentAuctions(auctions.slice(0, 3));
        setStats({
          activeAuctions: auctions.length,
          totalBids: auctions.reduce((acc, a) => acc + (a.bids?.length || 0), 0),
          totalUsers: 2
        });
        setLoading(false);
      })
      .catch(err => {
        console.error('Dashboard fetch error:', err);
        setLoading(false);
      });
  };

  useEffect(() => {
    fetchDashboard();
  }, []);

  const handleCreateAuction = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const response = await fetch(`http://localhost:9000/api/auctions?creatorId=1&itemId=${itemId}`, {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          ...auth?.getAuthHeader()
        },
        body: JSON.stringify({ 
          startPrice: Number(startPrice),
          startTime: new Date().toISOString(),
          endTime: new Date(Date.now() + 86400000).toISOString()
        })
      });

      if (response.ok) {
        alert('Auction created successfully!');
        setShowCreateForm(false);
        fetchDashboard();
      } else {
        alert('Failed to create auction. Status: ' + response.status);
      }
    } catch (err) {
      alert('Error creating auction');
    }
  };

  if (loading) return <div className="container">Loading dashboard...</div>;

  return (
    <div>
      <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem'}}>
        <h1>Platform Dashboard</h1>
        {auth?.role === 'ADMIN' && (
          <button className="btn btn-primary" onClick={() => setShowCreateForm(!showCreateForm)}>
            {showCreateForm ? 'Cancel' : '+ New Auction'}
          </button>
        )}
      </div>

      {showCreateForm && (
        <div className="card" style={{marginBottom: '2rem', background: '#f8fafc', border: '2px dashed var(--primary)'}}>
          <h3>Create New Auction (Admin)</h3>
          <form onSubmit={handleCreateAuction} style={{display: 'flex', gap: '1rem', alignItems: 'flex-end'}}>
            <div>
              <label style={{display: 'block', fontSize: '0.875rem', marginBottom: '0.25rem'}}>Item ID</label>
              <input type="number" value={itemId} onChange={e => setItemId(e.target.value)} style={{padding: '0.5rem', borderRadius: '0.25rem', border: '1px solid #ddd'}} />
            </div>
            <div>
              <label style={{display: 'block', fontSize: '0.875rem', marginBottom: '0.25rem'}}>Start Price ($)</label>
              <input type="number" value={startPrice} onChange={e => setStartPrice(e.target.value)} style={{padding: '0.5rem', borderRadius: '0.25rem', border: '1px solid #ddd'}} />
            </div>
            <button type="submit" className="btn btn-primary">Publish Auction</button>
          </form>
        </div>
      )}
      
      <div className="auction-grid" style={{marginBottom: '3rem'}}>
        <div className="card" style={{textAlign: 'center'}}>
          <div style={{fontSize: '2.5rem', fontWeight: 'bold', color: 'var(--primary)'}}>{stats.activeAuctions}</div>
          <div style={{color: 'var(--secondary)'}}>Active Auctions</div>
        </div>
        <div className="card" style={{textAlign: 'center'}}>
          <div style={{fontSize: '2.5rem', fontWeight: 'bold', color: 'var(--success)'}}>{stats.totalBids}</div>
          <div style={{color: 'var(--secondary)'}}>Total Bids Placed</div>
        </div>
        <div className="card" style={{textAlign: 'center'}}>
          <div style={{fontSize: '2.5rem', fontWeight: 'bold', color: 'var(--text)'}}>{stats.totalUsers}</div>
          <div style={{color: 'var(--secondary)'}}>Registered Users</div>
        </div>
      </div>

      <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem'}}>
        <h2>Featured Auctions</h2>
        <Link to="/auctions" className="btn btn-primary">View All</Link>
      </div>

      <div className="auction-grid">
        {recentAuctions.map(auction => (
          <div key={auction.id} className="card">
            <h3>{auction.item?.name || (auction as any).itemName || 'Unnamed Item'}</h3>
            <p className="price-tag">${auction.currentPrice}</p>
            <Link to={`/auction/${auction.id}`} className="btn" style={{border: '1px solid var(--primary)', color: 'var(--primary)', marginTop: '1rem', display: 'inline-block', textDecoration: 'none'}}>
              Monitor
            </Link>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Dashboard;
