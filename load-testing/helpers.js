let currentBid = 0; // <-- CHANGE THIS to your auction's current price

function generateBid(context, events, done) {
  currentBid += 5;
  
  context.vars.amount = currentBid;
  
  return done();
}

module.exports = { generateBid };
