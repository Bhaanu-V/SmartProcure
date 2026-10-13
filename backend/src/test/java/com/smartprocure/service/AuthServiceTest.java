package com.smartprocure.service;

import com.smartprocure.dto.auth.AuthResponse;
import com.smartprocure.dto.auth.LoginRequest;
import com.smartprocure.dto.auth.RegisterRequest;
import com.smartprocure.dto.auth.UserProfileResponse;
import com.smartprocure.entity.Role;
import com.smartprocure.entity.User;
import com.smartprocure.exception.BadRequestException;
import com.smartprocure.repository.DepartmentRepository;
import com.smartprocure.repository.UserRepository;
import com.smartprocure.security.jwt.JwtProperties;
import com.smartprocure.security.jwt.JwtTokenProvider;
import com.smartprocure.security.user.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                authenticationManager,
                userRepository,
                departmentRepository,
                passwordEncoder,
                tokenProvider,
                jwtProperties
        );
    }

    @Test
    @DisplayName("Should successfully authenticate user and return AuthResponse")
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("testuser", "password123");
        User user = new User("testuser", "test@smartprocure.com", "encodedPass", "Test User", Role.REQUESTER);
        user.setId(10L);
        CustomUserDetails userDetails = CustomUserDetails.build(user);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(tokenProvider.generateToken("testuser", 10L, "REQUESTER")).thenReturn("mock-jwt-token");
        when(jwtProperties.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("testuser", response.getUsername());
        assertEquals(Role.REQUESTER, response.getRole());
    }

    @Test
    @DisplayName("Should throw BadCredentialsException on invalid login")
    void testLoginInvalidCredentials() {
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest("newuser", "new@smartprocure.com", "secret123", "New User", null, Role.REQUESTER);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@smartprocure.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encodedSecret");

        User savedUser = new User("newuser", "new@smartprocure.com", "encodedSecret", "New User", Role.REQUESTER);
        savedUser.setId(20L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserProfileResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(20L, response.getId());
        assertEquals("newuser", response.getUsername());
        assertEquals("new@smartprocure.com", response.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject registration if username already exists")
    void testRegisterDuplicateUsername() {
        RegisterRequest request = new RegisterRequest("existinguser", "new@smartprocure.com", "secret123", "User", null, Role.REQUESTER);
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject registration if email already exists")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("newuser", "existing@smartprocure.com", "secret123", "User", null, Role.REQUESTER);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@smartprocure.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }
}
