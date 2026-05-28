import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';

interface Auction {
  id: number;
  item: {
    name: string;
    description: string;
  };
  currentPrice: number;
  endTime: string;
}

const AuctionList = () => {
  const [auctions, setAuctions] = useState<Auction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch('http://localhost:9000/api/auctions/all')
      .then(res => {
        if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
        return res.json();
      })
      .then(data => {
        setAuctions(Array.isArray(data) ? data : [data]);
        setLoading(false);
      })
      .catch(err => {
        console.error('Fetch error:', err);
        setError('Could not connect to the backend. Please ensure the Gateway and Backend are running.');
        setLoading(false);
      });
  }, []);

  if (loading) return <div className="container">Loading auctions...</div>;
  if (error) return <div className="container" style={{color: 'var(--danger)'}}>{error}</div>;

  return (
    <div>
      <h1>Active Auctions</h1>
      {auctions.length === 0 ? (
        <p>No active auctions found. Seed some data to get started!</p>
      ) : (
        <div className="auction-grid">
          {auctions.map(auction => (
            <div key={auction.id} className="card">
              <h3>{auction.item?.name || (auction as any).itemName || 'Unnamed Item'}</h3>
              <p className="price-tag">${auction.currentPrice}</p>
              <p>Ends: {new Date(auction.endTime).toLocaleString()}</p>
              <Link to={`/auction/${auction.id}`} className="btn btn-primary" style={{textDecoration: 'none', display: 'inline-block', marginTop: '1rem'}}>
                View Live Auction
              </Link>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default AuctionList;
