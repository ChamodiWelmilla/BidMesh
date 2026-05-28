import { useState, useEffect, useContext } from 'react';
import { useParams } from 'react-router-dom';
import { WebSocketService } from '../services/WebSocketService';
import { AuthContext } from '../App';

interface Auction {
  id: number;
  item: {
    name: string;
    description: string;
  };
  currentPrice: number;
}

interface BidNotification {
  auctionId: number;
  currentPrice: number;
  lastBidder: string;
}

const AuctionDetail = () => {
  const auth = useContext(AuthContext);
  const { id } = useParams<{ id: string }>();
  const [auction, setAuction] = useState<Auction | null>(null);
  const [bidAmount, setBidAmount] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Fetch initial data
    setLoading(true);
    fetch(`http://localhost:9000/api/auctions/${id}`)
      .then(res => {
        if (!res.ok) throw new Error('Auction not found');
        return res.json();
      })
      .then(data => {
        setAuction(data);
        setLoading(false);
      })
      .catch(err => {
        setError(err.message);
        setLoading(false);
      });

    // Connect WebSocket
    const ws = new WebSocketService((notification: BidNotification) => {
      setAuction(prev => prev ? { ...prev, currentPrice: notification.currentPrice } : null);
      setMessage(`New bid by ${notification.lastBidder}!`);
      setTimeout(() => setMessage(''), 5000);
    }, Number(id));

    ws.activate();
    return () => ws.deactivate();
  }, [id]);

  const handleBid = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const response = await fetch(`http://localhost:9000/api/auctions/${id}/bids`, {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          ...auth?.getAuthHeader()
        },
        body: JSON.stringify({ bidderId: 2, amount: Number(bidAmount) })
      });

      if (!response.ok) {
        if (response.status === 403) {
          alert('Access Denied: Only regular users can place bids.');
        } else {
          const err = await response.json().catch(() => ({ message: 'Bid failed' }));
          alert(err.message || 'Bid failed');
        }
      } else {
        setBidAmount('');
      }
    } catch (err) {
      alert('Network error while placing bid');
    }
  };

  if (loading) return <div className="container">Loading auction details...</div>;
  if (error || !auction) return <div className="container" style={{color: 'var(--danger)'}}>Error: {error || 'Auction not found'}</div>;

  return (
    <div className="card" style={{maxWidth: '600px', margin: '2rem auto'}}>
      <h2>{auction.item?.name || (auction as any).itemName || 'Unnamed Item'}</h2>
      <p>{auction.item?.description || (auction as any).description || 'No description available'}</p>
      
      <div style={{textAlign: 'center', margin: '2rem 0'}}>
        <span style={{fontSize: '1rem', color: '#666'}}>Current Price</span>
        <div className="price-tag" style={{fontSize: '3rem'}}>${auction.currentPrice}</div>
        {message && <div style={{color: 'var(--primary)', fontWeight: 'bold'}}>{message}</div>}
      </div>

      {auth?.role === 'USER' ? (
        <form onSubmit={handleBid} style={{display: 'flex', gap: '1rem'}}>
          <input 
            type="number" 
            value={bidAmount}
            onChange={(e) => setBidAmount(e.target.value)}
            placeholder="Enter your bid"
            style={{flex: 1, padding: '0.5rem', borderRadius: '0.375rem', border: '1px solid #ddd'}}
            required
          />
          <button type="submit" className="btn btn-primary">Place Bid</button>
        </form>
      ) : (
        <div style={{textAlign: 'center', padding: '1rem', background: '#f1f5f9', borderRadius: '0.5rem', color: '#64748b', fontSize: '0.875rem'}}>
          {auth?.role === 'ADMIN' ? 'Admins cannot place bids.' : 'Please log in as a User to place bids.'}
        </div>
      )}
    </div>
  );
};

export default AuctionDetail;
