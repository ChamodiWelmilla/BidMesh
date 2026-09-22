import { useState, useEffect, useContext } from 'react';
import { Link } from 'react-router-dom';
import { AuthContext } from '../App';
import toast from 'react-hot-toast';

interface DashboardStats {
  activeAuctions: number;
  completedAuctions: number;
  totalBids: number;
  totalUsers: number;
}

interface Auction {
  id: number;
  item: {
    name: string;
    imageUrl?: string;
  };
  currentPrice: number;
  status: string;
  endTime: string;
}

const Dashboard = () => {
  const auth = useContext(AuthContext);
  const [stats, setStats] = useState<DashboardStats>({ activeAuctions: 0, completedAuctions: 0, totalBids: 0, totalUsers: 0 });
  const [recentAuctions, setRecentAuctions] = useState<Auction[]>([]);
  const [allAuctions, setAllAuctions] = useState<Auction[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [showAuctionsModal, setShowAuctionsModal] = useState<{show: boolean, type: 'ACTIVE' | 'COMPLETED'}>({show: false, type: 'ACTIVE'});

  // Form State
  const [startPrice, setStartPrice] = useState('100');
  const [itemName, setItemName] = useState('');
  const [itemDescription, setItemDescription] = useState('');
  const [imageFile, setImageFile] = useState<File | null>(null);

  // Bid History State
  const [showBidHistory, setShowBidHistory] = useState(false);
  const [bidHistory, setBidHistory] = useState<any[]>([]);

  const fetchBidHistory = async () => {
    try {
      const res = await fetch('http://localhost:9000/api/users/me/bids', {
        headers: auth?.getAuthHeader()
      });
      if (res.ok) {
        const data = await res.json();
        setBidHistory(data);
        setShowBidHistory(true);
      } else {
        toast.error('Failed to load bid history');
      }
    } catch (err) {
      toast.error('Network error loading bid history');
    }
  };

  const fetchDashboard = () => {
    fetch('http://localhost:9000/api/auctions/all')
      .then(res => res.json())
      .then(data => {
        const auctions = Array.isArray(data) ? data : [data];
        setAllAuctions(auctions);
        
        const oneDayAgo = new Date(Date.now() - 24 * 60 * 60 * 1000);
        const featured = auctions.filter((a: any) => {
          if (a.status === 'ACTIVE') return true;
          if (a.status === 'COMPLETED') return new Date(a.endTime) >= oneDayAgo;
          return false;
        }).slice(0, 3);
        setRecentAuctions(featured);

        setStats({
          activeAuctions: auctions.filter((a: any) => a.status === 'ACTIVE').length,
          completedAuctions: auctions.filter((a: any) => a.status === 'COMPLETED').length,
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
    if (!itemName) {
      toast.error("Item Name is required!");
      return;
    }

    try {
      // 1. Create the Item First
      const itemResponse = await fetch(`http://localhost:9000/api/items`, {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          ...auth?.getAuthHeader()
        },
        body: JSON.stringify({ name: itemName, description: itemDescription })
      });

      if (!itemResponse.ok) throw new Error("Failed to create item");
      const newItem = await itemResponse.json();
      const createdItemId = newItem.id;

      // 2. Create the Auction with the new Item ID
      const response = await fetch(`http://localhost:9000/api/auctions?creatorId=1&itemId=${createdItemId}`, {
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
        // 3. Upload Image if provided
        if (imageFile) {
          const formData = new FormData();
          formData.append('file', imageFile);
          await fetch(`http://localhost:9000/api/items/${createdItemId}/image`, {
            method: 'POST',
            headers: auth?.getAuthHeader(),
            body: formData
          }).catch(err => console.error("Image upload failed", err));
        }

        toast.success('Auction created successfully!');
        setShowCreateForm(false);
        setImageFile(null);
        setItemName('');
        setItemDescription('');
        fetchDashboard();
      } else {
        toast.error('Failed to create auction. Status: ' + response.status);
      }
    } catch (err) {
      toast.error('Error creating auction');
    }
  };

  if (loading) return <div className="container">Loading dashboard...</div>;

  return (
    <div>
      <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem'}}>
        <h1>Platform Dashboard</h1>
        {auth?.role === 'ROLE_ADMIN' && (
          <button className="btn btn-primary" onClick={() => setShowCreateForm(!showCreateForm)}>
            {showCreateForm ? 'Cancel' : '+ New Auction'}
          </button>
        )}
      </div>

      {showCreateForm && (
        <div style={{
          position: 'fixed', top: 0, left: 0, width: '100%', height: '100%',
          backgroundColor: 'rgba(0, 0, 0, 0.6)', backdropFilter: 'blur(4px)',
          zIndex: 9999, display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '1rem'
        }}>
          <div className="card" style={{ width: '100%', maxWidth: '700px', background: 'white', borderRadius: '12px', padding: '2.5rem', boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1)' }}>
            <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem'}}>
              <h2 style={{margin: 0, color: 'var(--primary)'}}>Create New Auction</h2>
              <button onClick={() => setShowCreateForm(false)} style={{background: 'none', border: 'none', fontSize: '2rem', cursor: 'pointer', color: 'var(--secondary)', lineHeight: '1rem'}}>&times;</button>
            </div>
            
            <form onSubmit={handleCreateAuction} style={{display: 'flex', flexDirection: 'column', gap: '1.25rem'}}>
              <div>
                <label style={{display: 'block', fontSize: '0.875rem', fontWeight: 'bold', marginBottom: '0.5rem'}}>Item Name</label>
                <input type="text" value={itemName} onChange={e => setItemName(e.target.value)} style={{width: '100%', padding: '0.75rem', borderRadius: '0.375rem', border: '1px solid #ccc'}} required />
              </div>
              
              <div>
                <label style={{display: 'block', fontSize: '0.875rem', fontWeight: 'bold', marginBottom: '0.5rem'}}>Description</label>
                <textarea value={itemDescription} onChange={e => setItemDescription(e.target.value)} rows={3} style={{width: '100%', padding: '0.75rem', borderRadius: '0.375rem', border: '1px solid #ccc', resize: 'vertical'}} />
              </div>
              
              <div>
                <label style={{display: 'block', fontSize: '0.875rem', fontWeight: 'bold', marginBottom: '0.5rem'}}>Start Price ($)</label>
                <input type="number" value={startPrice} onChange={e => setStartPrice(e.target.value)} style={{width: '100%', padding: '0.75rem', borderRadius: '0.375rem', border: '1px solid #ccc', boxSizing: 'border-box'}} required />
              </div>
              
              <div>
                <label style={{display: 'block', fontSize: '0.875rem', fontWeight: 'bold', marginBottom: '0.5rem'}}>Item Image</label>
                <input type="file" accept="image/*" onChange={e => setImageFile(e.target.files?.[0] || null)} style={{width: '100%', padding: '0.6rem', fontSize: '0.85rem', border: '1px solid #ccc', borderRadius: '0.375rem', boxSizing: 'border-box'}} />
              </div>
              
              <div style={{marginTop: '1.5rem'}}>
                <button type="submit" className="btn btn-primary" style={{width: '100%', padding: '0.875rem', fontSize: '1.05rem', fontWeight: 'bold'}}>Publish Auction</button>
              </div>
            </form>
          </div>
        </div>
      )}
      
      <div style={{display: 'flex', justifyContent: 'center', flexWrap: 'wrap', gap: '1.5rem', marginBottom: '3rem'}}>
        <div 
          className={auth?.role === 'ROLE_ADMIN' ? "card hover-card" : "card"} 
          style={{flex: '1 1 300px', maxWidth: '400px', textAlign: 'center', cursor: auth?.role === 'ROLE_ADMIN' ? 'pointer' : 'default'}}
          onClick={() => auth?.role === 'ROLE_ADMIN' && setShowAuctionsModal({show: true, type: 'ACTIVE'})}
          title={auth?.role === 'ROLE_ADMIN' ? "Click to view active auctions" : ""}
        >
          <div style={{fontSize: '2.5rem', fontWeight: 'bold', color: 'var(--primary)'}}>{stats.activeAuctions}</div>
          <div style={{color: 'var(--secondary)'}}>Active Auctions</div>
        </div>
        
        {auth?.role === 'ROLE_USER' && (
          <div 
            className="card hover-card" 
            style={{flex: '1 1 300px', maxWidth: '400px', textAlign: 'center', cursor: 'pointer'}}
            onClick={fetchBidHistory}
            title="Click to view your bid history"
          >
            <div style={{fontSize: '2.5rem', fontWeight: 'bold', color: 'var(--success)'}}>{stats.totalBids}</div>
            <div style={{color: 'var(--secondary)'}}>Total Bids Placed (Click to view)</div>
          </div>
        )}

        {auth?.role === 'ROLE_ADMIN' && (
          <>
            <div 
              className="card hover-card" 
              style={{flex: '1 1 300px', maxWidth: '400px', textAlign: 'center', cursor: 'pointer'}}
              onClick={() => setShowAuctionsModal({show: true, type: 'COMPLETED'})}
              title="Click to view completed auctions"
            >
              <div style={{fontSize: '2.5rem', fontWeight: 'bold', color: 'var(--success)'}}>{stats.completedAuctions}</div>
              <div style={{color: 'var(--secondary)'}}>Completed Auctions</div>
            </div>
            <div className="card" style={{flex: '1 1 300px', maxWidth: '400px', textAlign: 'center'}}>
              <div style={{fontSize: '2.5rem', fontWeight: 'bold', color: 'var(--text)'}}>{stats.totalUsers}</div>
              <div style={{color: 'var(--secondary)'}}>Registered Users</div>
            </div>
          </>
        )}
      </div>

      <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem'}}>
        <h2>Featured Auctions</h2>
        <Link to="/auctions" className="btn btn-primary">View All</Link>
      </div>

      <div className="auction-grid">
        {recentAuctions.map(auction => (
          <div key={auction.id} className="card" style={{position: 'relative'}}>
            {auction.status === 'COMPLETED' && (
              <div style={{ position: 'absolute', top: '12px', right: '12px', background: 'var(--danger)', color: 'white', padding: '0.25rem 0.6rem', borderRadius: '4px', fontSize: '0.75rem', fontWeight: 'bold', zIndex: 10, boxShadow: '0 2px 4px rgba(0,0,0,0.1)' }}>
                Ended
              </div>
            )}
            {auction.item?.imageUrl && (
              <img 
                src={auction.item.imageUrl.startsWith('http') ? auction.item.imageUrl : `http://localhost:9000${auction.item.imageUrl}`} 
                alt="Auction Item" 
                style={{width: '100%', height: '150px', objectFit: 'cover', borderRadius: '0.375rem', marginBottom: '1rem'}} 
              />
            )}
            <h3>{auction.item?.name || (auction as any).itemName || 'Unnamed Item'}</h3>
            <p className="price-tag">${auction.currentPrice}</p>
            <Link to={`/auction/${auction.id}`} className="btn" style={{border: '1px solid var(--primary)', color: 'var(--primary)', marginTop: '1rem', display: 'inline-block', textDecoration: 'none'}}>
              Monitor
            </Link>
          </div>
        ))}
      </div>

      {showBidHistory && (
        <div style={{
          position: 'fixed', top: 0, left: 0, width: '100%', height: '100%',
          backgroundColor: 'rgba(0, 0, 0, 0.6)', backdropFilter: 'blur(4px)',
          zIndex: 9999, display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '1rem'
        }}>
          <div className="card" style={{ width: '100%', maxWidth: '600px', maxHeight: '80vh', overflowY: 'auto', background: 'white', borderRadius: '12px', padding: '2rem', boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1)' }}>
            <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem'}}>
              <h2 style={{margin: 0, color: 'var(--primary)'}}>Your Bid History</h2>
              <button onClick={() => setShowBidHistory(false)} style={{background: 'none', border: 'none', fontSize: '2rem', cursor: 'pointer', color: 'var(--secondary)', lineHeight: '1rem'}}>&times;</button>
            </div>
            
            {bidHistory.length === 0 ? (
              <p style={{textAlign: 'center', color: 'var(--secondary)', margin: '2rem 0'}}>You haven't placed any bids yet.</p>
            ) : (
              <div style={{display: 'flex', flexDirection: 'column', gap: '1rem'}}>
                {bidHistory.map((bid, i) => (
                  <div key={i} style={{padding: '1rem', border: '1px solid #e2e8f0', borderRadius: '8px', display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                    <div>
                      <div style={{fontWeight: 'bold', fontSize: '1.1rem'}}>{bid.itemName}</div>
                      <div style={{fontSize: '0.85rem', color: 'var(--secondary)', marginTop: '0.25rem'}}>
                        {new Date(bid.bidTime).toLocaleString()}
                      </div>
                    </div>
                    <div style={{textAlign: 'right'}}>
                      <div style={{fontWeight: 'bold', color: 'var(--success)', fontSize: '1.25rem'}}>${bid.amount}</div>
                      <Link to={`/auction/${bid.auctionId}`} style={{fontSize: '0.85rem', color: 'var(--primary)', textDecoration: 'none'}}>View Auction &rarr;</Link>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
      {showAuctionsModal.show && (
        <div style={{
          position: 'fixed', top: 0, left: 0, width: '100%', height: '100%',
          backgroundColor: 'rgba(0, 0, 0, 0.6)', backdropFilter: 'blur(4px)',
          zIndex: 9999, display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '1rem'
        }}>
          <div className="card" style={{ width: '100%', maxWidth: '600px', maxHeight: '80vh', overflowY: 'auto', background: 'white', borderRadius: '12px', padding: '2rem', boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1)' }}>
            <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem'}}>
              <h2 style={{margin: 0, color: 'var(--primary)'}}>
                {showAuctionsModal.type === 'ACTIVE' ? 'Active Auctions' : 'Completed Auctions'}
              </h2>
              <button onClick={() => setShowAuctionsModal({show: false, type: 'ACTIVE'})} style={{background: 'none', border: 'none', fontSize: '2rem', cursor: 'pointer', color: 'var(--secondary)', lineHeight: '1rem'}}>&times;</button>
            </div>
            
            <div style={{display: 'flex', flexDirection: 'column', gap: '1rem'}}>
              {allAuctions.filter(a => a.status === showAuctionsModal.type).map(auction => (
                <div key={auction.id} style={{padding: '1rem', border: '1px solid #e2e8f0', borderRadius: '8px', display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                  <div>
                    <div style={{fontWeight: 'bold', fontSize: '1.1rem'}}>{auction.item?.name || 'Unnamed Item'}</div>
                    <div style={{fontSize: '0.85rem', color: 'var(--secondary)', marginTop: '0.25rem'}}>${auction.currentPrice}</div>
                  </div>
                  <div>
                    <Link to={`/auction/${auction.id}`} className="btn" style={{border: '1px solid var(--primary)', color: 'var(--primary)', textDecoration: 'none', padding: '0.4rem 0.8rem', fontSize: '0.85rem'}}>View &rarr;</Link>
                  </div>
                </div>
              ))}
              {allAuctions.filter(a => a.status === showAuctionsModal.type).length === 0 && (
                <p style={{textAlign: 'center', color: 'var(--secondary)', margin: '2rem 0'}}>No {showAuctionsModal.type.toLowerCase()} auctions found.</p>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
