package com.cooksync_server.config;

import java.security.Key;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.cooksync_server.constants.SecurityConstants;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Utility component for generating, signing, and parsing JSON Web Tokens (JWT).
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Component
public class JwtUtil {

    /** Access token lifetime: 15 minutes, after which the client must refresh. */
    private static final long ACCESS_TOKEN_VALIDITY_MS = 1000L * 60 * 15;

    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * Extracts subject email address from the given JWT token string.
     *
     * @param token encoded JWT token
     * @return extracted subject email string
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts a custom claim using the supplied claim resolver function.
     *
     * @param <T> claim return type
     * @param token encoded JWT token
     * @param claimsResolver function extracting target claim
     * @return resolved claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates a signed JWT access token containing user identity and role claims.
     *
     * @param email target user email
     * @param userId target user ID
     * @param isAdmin administrative privilege status flag
     * @return signed compact JWT string
     */
    public String generateToken(String email, String userId, boolean isAdmin) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("isAdmin", isAdmin);

        List<String> roles = isAdmin ? List.of(SecurityConstants.ROLE_ADMIN) : List.of(SecurityConstants.ROLE_USER);
        claims.put("roles", roles);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY_MS))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validates whether the token subject matches target user email and is unexpired.
     *
     * @param token JWT string
     * @param email target user email
     * @return true if token is valid, false otherwise
     */
    public boolean isTokenValid(String token, String email) {
        final String extractedEmail = extractEmail(token);
        return (extractedEmail.equals(email)) && !isTokenExpired(token);
    }

    /**
     * Extracts user granted authorities from roles claim within JWT payload.
     *
     * @param token JWT string
     * @return collection of granted authority objects
     */
    @SuppressWarnings("unchecked")
    public Collection<? extends GrantedAuthority> extractAuthorities(String token) {
        Claims claims = extractAllClaims(token);
        List<String> roles = claims.get("roles", List.class);
        return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
