package com.andd.DoDangAn.DoDangAn.Util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private Key SECRET_KEY;

    @PostConstruct
    public void init() {
        // Khởi tạo SECRET_KEY từ jwtSecret
        if (jwtSecret == null || jwtSecret.length() < 32) {
            logger.error("JWT secret key is too short or not provided. It must be at least 32 characters for HS512.");
            throw new IllegalArgumentException("JWT secret key must be at least 32 characters for HS512");
        }
        SECRET_KEY = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        logger.info("JWT expiration value: {} milliseconds ({} seconds)", jwtExpiration, jwtExpiration / 1000);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = parseClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String generateToken(String username, String fullName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("fullName", fullName);
        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS512)
                .compact();
        logger.info("Generated JWT for user {}: {}", username, token);
        return token;
    }

    public long getExpiration() {
        return jwtExpiration;
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        logger.info("Validating JWT for user {}. Is valid: {}", username, isValid);
        return isValid;
    }

    public boolean isTokenExpired(String token) {
        try {
            boolean expired = extractExpiration(token).before(new Date());
            logger.info("Token expiration check: expired={}", expired);
            return expired;
        } catch (Exception e) {
            logger.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }
}