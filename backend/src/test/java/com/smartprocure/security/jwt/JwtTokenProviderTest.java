package com.smartprocure.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private JwtProperties jwtProperties;

    // 256-bit secure test secret
    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties(TEST_SECRET, EXPIRATION_MS);
        tokenProvider = new JwtTokenProvider(jwtProperties);
    }

    @Test
    @DisplayName("Should generate and validate valid JWT token")
    void testGenerateAndValidateToken() {
        String token = tokenProvider.generateToken("john_doe", 1L, "REQUESTER");

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals("john_doe", tokenProvider.getUsernameFromToken(token));
        assertEquals(1L, tokenProvider.getUserIdFromToken(token));
        assertEquals("REQUESTER", tokenProvider.getRoleFromToken(token));
        assertNotNull(tokenProvider.getExpirationDateFromToken(token));
    }

    @Test
    @DisplayName("Should reject malformed JWT token")
    void testValidateMalformedToken() {
        assertFalse(tokenProvider.validateToken("not-a-valid-token"));
    }

    @Test
    @DisplayName("Should reject expired JWT token")
    void testValidateExpiredToken() {
        JwtProperties expiredProps = new JwtProperties(TEST_SECRET, -1000); // already expired
        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProps);

        String expiredToken = expiredProvider.generateToken("expired_user", 2L, "APPROVER");
        assertFalse(tokenProvider.validateToken(expiredToken));
    }

    @Test
    @DisplayName("Should reject token signed with different key")
    void testValidateTamperedKey() {
        String differentSecret = "7336763979244226452948404D635166546A576E5A7234753778214125442A47";
        JwtProperties otherProps = new JwtProperties(differentSecret, EXPIRATION_MS);
        JwtTokenProvider otherProvider = new JwtTokenProvider(otherProps);

        String token = otherProvider.generateToken("john_doe", 1L, "REQUESTER");
        assertFalse(tokenProvider.validateToken(token));
    }
}
