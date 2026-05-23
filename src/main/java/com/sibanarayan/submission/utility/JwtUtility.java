package com.sibanarayan.submission.utility;


import com.sibanarayan.submission.enums.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;

@Component
public class JwtUtility {

    private final Key key;
    private final long expiration;

    public JwtUtility() {
        String secret = System.getenv("JWT_SECRET");
        String exp = System.getenv("JWT_EXPIRATION");

        if (secret == null || exp == null) {
            throw new IllegalStateException("JWT environment variables not set");
        }

        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiration = Long.parseLong(exp);
    }

    // Generate token
    public String generateToken(String email, UUID userId, UserRole role) {
        Instant now = Instant.now();

        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("ROLE",role.name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(expiration)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Extract email
    public String getEmail(String token) {
        return getClaims(token).getSubject();
    }

    public  String getRole(String token){
        return getClaims(token).get("ROLE",String.class);
    }

    // Extract userId
    public UUID getUserId(String token) {
        String id= getClaims(token).get("userId", String.class);
        return UUID.fromString(id);
    }

    // Validate token
    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    public UserPrincipal getCurrentUser(){
        return (UserPrincipal) SecurityContextHolder.
                getContext().
                getAuthentication().
                getPrincipal();
    }

    // Internal helpers
    private Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }



    private Claims getClaims(String token) {
        return parseToken(token).getBody();
    }
}


