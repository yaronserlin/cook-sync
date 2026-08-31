package com.cooksync_server.config;

import java.security.Key;
import java.util.Collection;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite verifying JWT token generation, parsing, claim extraction, and validation logic.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/08/2026
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String BASE64_SECRET = "c3VwZXItc2VjcmV0LWp3dC1rZXktZm9yLWNvb2tzeW5jLXNlcnZlci1hcHBsaWNhdGlvbi0yMDI2";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", BASE64_SECRET);
    }

    @Test
    void generateToken_ShouldReturnValidJwtToken() {
        String token = jwtUtil.generateToken("user@example.com", "user-uuid-123", false);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals("user@example.com", jwtUtil.extractEmail(token));
    }

    @Test
    void isTokenValid_ShouldReturnTrueForMatchingEmail() {
        String email = "chef@cooksync.com";
        String token = jwtUtil.generateToken(email, "user-456", false);

        assertTrue(jwtUtil.isTokenValid(token, email));
        assertFalse(jwtUtil.isTokenValid(token, "other@cooksync.com"));
    }

    @Test
    void isTokenValid_ShouldThrowExpiredJwtException_WhenTokenHasExpired() {
        String email = "expired@cooksync.com";
        Key signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(BASE64_SECRET));
        String expiredToken = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 30))
                .setExpiration(new Date(System.currentTimeMillis() - 1000 * 60 * 15))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        assertThrows(ExpiredJwtException.class, () -> jwtUtil.isTokenValid(expiredToken, email));
    }

    @Test
    void extractAuthorities_ShouldReturnAdminRoleForAdminUser() {
        String token = jwtUtil.generateToken("admin@cooksync.com", "admin-id", true);
        Collection<? extends GrantedAuthority> authorities = jwtUtil.extractAuthorities(token);

        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void extractAuthorities_ShouldReturnUserRoleForNormalUser() {
        String token = jwtUtil.generateToken("user@cooksync.com", "user-id", false);
        Collection<? extends GrantedAuthority> authorities = jwtUtil.extractAuthorities(token);

        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }
}
