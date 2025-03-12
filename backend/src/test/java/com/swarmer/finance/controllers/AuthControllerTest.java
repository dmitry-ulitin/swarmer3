package com.swarmer.finance.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.swarmer.finance.dto.UserDto;
import com.swarmer.finance.models.User;
import com.swarmer.finance.repositories.UserRepository;
import com.swarmer.finance.security.JwtTokenProvider;
import com.swarmer.finance.security.UserPrincipal;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authenticationManager, userRepository, passwordEncoder, tokenProvider);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void authenticateUser_Success() {
        // Arrange
        AuthController.LoginRequest loginRequest = new AuthController.LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("jwt-token");

        // Act
        ResponseEntity<?> response = authController.authenticateUser(loginRequest);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        @SuppressWarnings({ "unchecked", "null" })
        String token = ((java.util.Map<String, String>) response.getBody()).get("token");
        assertThat(token).isEqualTo("jwt-token");
    }

    @Test
    void registerUser_Success() {
        // Arrange
        AuthController.RegisterRequest registerRequest = new AuthController.RegisterRequest();
        registerRequest.setName("Test User");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password");

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
        when(tokenProvider.generateToken(any(UserDto.class))).thenReturn("jwt-token");

        // Act
        ResponseEntity<?> response = authController.registerUser(registerRequest);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        @SuppressWarnings({ "unchecked", "null" })
        String token = ((java.util.Map<String, String>) response.getBody()).get("token");
        assertThat(token).isEqualTo("jwt-token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_EmailTaken() {
        // Arrange
        AuthController.RegisterRequest registerRequest = new AuthController.RegisterRequest();
        registerRequest.setEmail("existing@example.com");

        when(userRepository.findByEmailIgnoreCase("existing@example.com"))
                .thenReturn(Optional.of(new User()));

        // Act
        ResponseEntity<?> response = authController.registerUser(registerRequest);

        // Assert
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Email is already taken!");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_Success() {
        // Arrange
        AuthController.ChangePasswordRequest request = new AuthController.ChangePasswordRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("newPassword");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword("encoded-old-password");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserPrincipal(user));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "encoded-old-password")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encoded-new-password");

        // Act
        ResponseEntity<?> response = authController.changePassword(request);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("Password changed successfully");
        verify(userRepository).save(user);
        assertThat(user.getPassword()).isEqualTo("encoded-new-password");
    }

    @Test
    void changePassword_IncorrectOldPassword() {
        // Arrange
        AuthController.ChangePasswordRequest request = new AuthController.ChangePasswordRequest();
        request.setOldPassword("wrongPassword");
        request.setNewPassword("newPassword");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword("encoded-old-password");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserPrincipal(user));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encoded-old-password")).thenReturn(false);

        // Act
        ResponseEntity<?> response = authController.changePassword(request);

        // Assert
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Current password is incorrect");
        verify(userRepository, never()).save(any(User.class));
    }
} 