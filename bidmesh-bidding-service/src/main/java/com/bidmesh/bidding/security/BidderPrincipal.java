package com.bidmesh.bidding.security;

import java.security.Principal;

public record BidderPrincipal(Long userId, String email, String username) implements Principal {
    @Override
    public String getName() {
        return email;
    }
}
