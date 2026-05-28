import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import AuctionList from './pages/AuctionList';
import AuctionDetail from './pages/AuctionDetail';

function App() {
  return (
    <BrowserRouter>
      <header className="header">
        <Link to="/" className="logo">BidMesh</Link>
        <div className="user-info">Welcome, User</div>
      </header>
      <main className="container">
        <Routes>
          <Route path="/" element={<AuctionList />} />
          <Route path="/auction/:id" element={<AuctionDetail />} />
        </Routes>
      </main>
    </BrowserRouter>
  );
}

export default App;
