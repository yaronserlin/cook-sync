package com.cooksync_server.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cooksync_server.config.JwtUtil;
import com.cooksync_server.exceptions.auth.InvalidCredentialsException;
import com.cooksync_server.services.IAuthService;
import com.cooksync_server.services.IPasswordService;
import com.cooksync_server.services.IUserProfileService;
import com.dtos.request.auth.LoginRequestDTO;
import com.dtos.request.auth.RegisterRequestDTO;
import com.dtos.request.auth.VerifyRegistrationOtpRequestDTO;
import com.dtos.response.auth.AuthResponse;
import com.dtos.response.auth.PendingRegistrationResponse;
import com.dtos.response.user.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Web-layer test suite verifying {@link AuthController}'s request mapping, payload validation,
 * and status-code wiring against mocked {@link IAuthService}, {@link IUserProfileService}, and
 * {@link IPasswordService} instances. Confirms the controller routes each endpoint to the correct
 * one of the three services after {@code AuthService} was split by responsibility.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 13/08/2026
 */
@WebMvcTest(controllers = AuthController.class)
@WithMockUser(username = "john@example.com")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IAuthService authService;

    @MockitoBean
    private IUserProfileService userProfileService;

    @MockitoBean
    private IPasswordService passwordService;

    /**
     * {@link com.cooksync_server.config.JwtAuthenticationFilter} is auto-registered by
     * {@code @WebMvcTest} as a servlet {@code Filter}; mocking its {@code JwtUtil} dependency
     * just satisfies that bean's constructor (no {@code Authorization} header is sent here).
     */
    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void register_ShouldReturnOk_WhenPayloadValid() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "John", "Doe", "john@example.com", "Password123!", true, false);
        when(authService.register(any(RegisterRequestDTO.class)))
                .thenReturn(new PendingRegistrationResponse("john@example.com", 600));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("john@example.com"));
    }

    @Test
    void register_ShouldReturnBadRequest_WhenEmailInvalid() throws Exception {
        RegisterRequestDTO invalidRequest = new RegisterRequestDTO(
                "John", "Doe", "not-an-email", "Password123!", true, false);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShouldReturnBadRequest_WhenTermsNotAccepted() throws Exception {
        RegisterRequestDTO invalidRequest = new RegisterRequestDTO(
                "John", "Doe", "john@example.com", "Password123!", false, false);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyRegistrationOtp_ShouldReturnBadRequest_WhenCodeNotSixDigits() throws Exception {
        VerifyRegistrationOtpRequestDTO invalidRequest =
                new VerifyRegistrationOtpRequestDTO("john@example.com", "12");

        mockMvc.perform(post("/api/auth/verify-registration-otp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_ShouldReturnOk_WhenCredentialsValid() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("john@example.com", "Password123!");
        when(authService.login(any(LoginRequestDTO.class)))
                .thenReturn(new AuthResponse("jwt-token", "refresh-token", "user-1", "John", "Doe", false, null));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt-token"));
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenCredentialsInvalid() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("john@example.com", "WrongPassword");
        when(authService.login(any(LoginRequestDTO.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_ShouldReturnBadRequest_WhenPasswordTooShort() throws Exception {
        LoginRequestDTO invalidRequest = new LoginRequestDTO("john@example.com", "123");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void getCurrentUser_ShouldRouteToUserProfileService() throws Exception {
        UserResponse response = new UserResponse("user-1", "John", "Doe", "john@example.com",
                false, null, "2026-08-01T00:00:00Z", "2026-08-01T00:00:00Z", true, "ACTIVE",
                null, null, true, true);
        when(userProfileService.getCurrentUserProfile("john@example.com")).thenReturn(response);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("john@example.com"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void forgotPassword_ShouldRouteToPasswordService() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"john@example.com\"}"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(passwordService).forgotPassword(any());
    }
}
