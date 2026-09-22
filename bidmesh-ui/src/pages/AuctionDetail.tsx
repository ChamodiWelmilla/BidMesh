import { useState, useEffect, useContext } from 'react';
import { useParams } from 'react-router-dom';
import { WebSocketService } from '../services/WebSocketService';
import { AuthContext } from '../App';
import toast from 'react-hot-toast';

interface Auction {
  id: number;
  item: {
    id: number;
    name: string;
    description: string;
    imageUrl?: string;
  };
  currentPrice: number;
  bids?: any[];
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
      setAuction(prev => prev ? { 
        ...prev, 
        currentPrice: notification.currentPrice,
        bids: [...(prev.bids || []), { dummy: true }]
      } : null);
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
          toast.error('Access Denied: Only regular users can place bids.');
        } else {
          const err = await response.json().catch(() => ({ message: 'Bid failed' }));
          toast.error(err.message || 'Bid failed');
        }
      } else {
        setBidAmount('');
      }
    } catch (err) {
      toast.error('Network error while placing bid');
    }
  };

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !auction?.item?.id) return;

    const formData = new FormData();
    formData.append('file', file);

    const toastId = toast.loading('Uploading image...');
    try {
      const response = await fetch(`http://localhost:9000/api/items/${auction.item.id}/image`, {
        method: 'POST',
        headers: auth?.getAuthHeader(),
        body: formData
      });

      if (response.ok) {
        toast.success('Image uploaded successfully', { id: toastId });
        // Refresh auction to get new image URL
        fetch(`http://localhost:9000/api/auctions/${id}`)
          .then(res => res.json())
          .then(data => setAuction(data));
      } else {
        toast.error('Failed to upload image', { id: toastId });
      }
    } catch (err) {
      toast.error('Network error uploading image', { id: toastId });
    }
  };

  if (loading) return <div className="container">Loading auction details...</div>;
  if (error || !auction) return <div className="container" style={{color: 'var(--danger)'}}>Error: {error || 'Auction not found'}</div>;

  return (
    <div className="card" style={{maxWidth: '600px', margin: '2rem auto'}}>
      {auction.item?.imageUrl && (
        <img 
          src={auction.item.imageUrl.startsWith('http') ? auction.item.imageUrl : `http://localhost:9000${auction.item.imageUrl}`} 
          alt={auction.item.name} 
          style={{width: '100%', height: '300px', objectFit: 'cover', borderRadius: '0.5rem', marginBottom: '1rem'}} 
        />
      )}
      <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start'}}>
        <div>
          <h2>{auction.item?.name || (auction as any).itemName || 'Unnamed Item'}</h2>
          <p>{auction.item?.description || (auction as any).description || 'No description available'}</p>
        </div>
        {auth?.role === 'ROLE_ADMIN' && auction.status !== 'COMPLETED' && (
          <div style={{display: 'flex', gap: '0.5rem', flexWrap: 'wrap', justifyContent: 'flex-end'}}>
            <label className="btn" style={{border: '1px solid var(--primary)', color: 'var(--primary)', cursor: 'pointer', fontSize: '0.8rem', padding: '0.25rem 0.5rem'}}>
              {auction.item?.imageUrl ? 'Edit Image' : 'Upload Image'}
              <input type="file" accept="image/*" style={{display: 'none'}} onChange={handleImageUpload} />
            </label>
            {(auction.bids && auction.bids.length > 0) && (
              <button 
                onClick={async () => {
                  try {
                    const res = await fetch(`http://localhost:9000/api/auctions/${id}/end`, {
                      method: 'PUT',
                      headers: auth?.getAuthHeader()
                    });
                    if (res.ok) {
                      toast.success('Auction ended successfully');
                      setAuction(await res.json());
                    } else {
                      toast.error('Failed to end auction');
                    }
                  } catch (err) { toast.error('Network error'); }
                }}
                className="btn" 
                style={{border: '1px solid #f59e0b', color: '#f59e0b', cursor: 'pointer', fontSize: '0.8rem', padding: '0.25rem 0.5rem', background: 'transparent'}}
              >
                End Auction
              </button>
            )}
            <button 
              onClick={async () => {
                if (auction.bids && auction.bids.length > 0) return;
                if (!window.confirm("Are you sure you want to delete this auction?")) return;
                try {
                  const res = await fetch(`http://localhost:9000/api/auctions/${id}`, {
                    method: 'DELETE',
                    headers: auth?.getAuthHeader()
                  });
                  if (res.ok) {
                    toast.success('Auction deleted successfully');
                    window.location.href = '/'; 
                  } else {
                    const err = await res.json().catch(()=>({message: 'Failed to delete'}));
                    toast.error(err.message || 'Failed to delete auction');
                  }
                } catch (err) { toast.error('Network error'); }
              }}
              disabled={auction.bids && auction.bids.length > 0}
              title={(auction.bids && auction.bids.length > 0) ? "Cannot delete an auction that has bids" : "Delete Auction"}
              className="btn" 
              style={{
                border: `1px solid ${(auction.bids && auction.bids.length > 0) ? '#ccc' : 'var(--danger)'}`, 
                color: (auction.bids && auction.bids.length > 0) ? '#999' : 'var(--danger)', 
                cursor: (auction.bids && auction.bids.length > 0) ? 'not-allowed' : 'pointer', 
                fontSize: '0.8rem', padding: '0.25rem 0.5rem', 
                background: (auction.bids && auction.bids.length > 0) ? '#f3f4f6' : 'transparent'
              }}
            >
              Delete Auction
            </button>
          </div>
        )}
      </div>
      
      <div style={{textAlign: 'center', margin: '2rem 0'}}>
        <span style={{fontSize: '1rem', color: '#666'}}>Current Price</span>
        <div className="price-tag" style={{fontSize: '3rem'}}>${auction.currentPrice}</div>
        {message && <div style={{color: 'var(--primary)', fontWeight: 'bold'}}>{message}</div>}
      </div>

      {auction.status === 'COMPLETED' ? (
        <div style={{textAlign: 'center', padding: '1rem', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '0.5rem', color: '#ef4444', fontWeight: 'bold'}}>
          This auction has officially ended.
        </div>
      ) : auth?.role === 'ROLE_USER' ? (
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
          {auth?.role === 'ROLE_ADMIN' ? 'Admins cannot place bids.' : 'Please log in as a User to place bids.'}
        </div>
      )}
    </div>
  );
};

export default AuctionDetail;
