package com.smartprocure.service;

import com.smartprocure.dto.auth.AuthResponse;
import com.smartprocure.dto.auth.LoginRequest;
import com.smartprocure.dto.auth.RegisterRequest;
import com.smartprocure.dto.auth.UserProfileResponse;
import com.smartprocure.entity.Department;
import com.smartprocure.entity.Role;
import com.smartprocure.entity.User;
import com.smartprocure.exception.BadRequestException;
import com.smartprocure.exception.ResourceNotFoundException;
import com.smartprocure.repository.DepartmentRepository;
import com.smartprocure.repository.UserRepository;
import com.smartprocure.security.jwt.JwtProperties;
import com.smartprocure.security.jwt.JwtTokenProvider;
import com.smartprocure.security.user.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing user authentication, registration, and profile retrieval.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       DepartmentRepository departmentRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       JwtProperties jwtProperties) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Authenticates credentials and returns a JWT access token.
     */
    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail().trim(),
                        loginRequest.getPassword()
                )
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String jwt = tokenProvider.generateToken(
                userDetails.getUsername(),
                userDetails.getId(),
                userDetails.getRole().name()
        );

        log.info("User '{}' successfully authenticated", userDetails.getUsername());

        return new AuthResponse(
                jwt,
                jwtProperties.getExpirationMs(),
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getFullName(),
                userDetails.getRole(),
                userDetails.getDepartment()
        );
    }

    /**
     * Registers a new user account with securely hashed credentials.
     */
    @Transactional
    public UserProfileResponse register(RegisterRequest registerRequest) {
        String username = registerRequest.getUsername().trim().toLowerCase();
        String email = registerRequest.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username '" + username + "' is already taken");
        }

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email '" + email + "' is already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFullName(registerRequest.getFullName().trim());
        user.setRole(registerRequest.getRole() != null ? registerRequest.getRole() : Role.REQUESTER);
        user.setActive(true);

        if (registerRequest.getDepartmentId() != null) {
            Department department = departmentRepository.findById(registerRequest.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", registerRequest.getDepartmentId()));
            user.setDepartmentEntity(department);
            user.setDepartment(department.getName());
        }

        User savedUser = userRepository.save(user);
        log.info("Registered new user '{}' with role {}", savedUser.getUsername(), savedUser.getRole());

        return UserProfileResponse.fromUser(savedUser);
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return UserProfileResponse.fromUser(user);
    }
}
