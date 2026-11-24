package com.bidmesh.catalog.security;

import java.security.Principal;

public record CatalogPrincipal(Long userId, String email, String username) implements Principal {
    @Override
    public String getName() {
        return email;
    }
}
