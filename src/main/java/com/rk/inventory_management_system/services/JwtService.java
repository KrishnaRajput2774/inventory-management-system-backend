package com.rk.inventory_management_system.services;

import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {
    String generateJwtToken(String username);
    boolean validateToken(String token, UserDetails userDetails);
    boolean isTokenExpired(String token);
    String extractUsername(String token);
}
