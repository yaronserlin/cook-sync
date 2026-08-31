package com.cooksync_server.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.cooksync_server.services.AuthService;
import com.cooksync_server.services.PasswordService;
import com.cooksync_server.services.UserProfileService;
import com.dtos.request.auth.AvatarUpdateRequestDTO;
import com.dtos.request.auth.ChangePasswordRequestDTO;
import com.dtos.request.auth.DeleteAccountRequestDTO;
import com.dtos.request.auth.EmailUpdateRequestDTO;
import com.dtos.request.auth.LoginRequestDTO;
import com.dtos.request.auth.PrivacySettingsUpdateRequestDTO;
import com.dtos.request.auth.ProfileUpdateRequestDTO;
import com.dtos.request.auth.RegisterRequestDTO;
import com.dtos.request.auth.ResetPasswordRequestDTO;
import com.dtos.request.auth.TokenRefreshRequestDTO;
import com.dtos.request.auth.VerifyEmailChangeOtpRequestDTO;
import com.dtos.request.auth.VerifyRegistrationOtpRequestDTO;
import com.dtos.response.auth.AuthResponse;
import com.dtos.response.auth.PendingRegistrationResponse;
import com.dtos.response.user.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Web-layer test suite verifying {@link AuthController}'s request mapping,
 * payload validation, and status-code wiring against mocked
 * {@link AuthService}, {@link UserProfileService}, and {@link PasswordService}
 * instances. Confirms the controller routes each endpoint to the correct one of
 * the three services after {@code AuthServiceImp} was split by responsibility.
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
    private AuthService authService;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private PasswordService passwordService;

    /**
     * {@link com.cooksync_server.config.JwtAuthenticationFilter} is
     * auto-registered by {@code @WebMvcTest} as a servlet {@code Filter};
     * mocking its {@code JwtUtil} dependency just satisfies that bean's
     * constructor (no {@code Authorization} header is sent here).
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
        VerifyRegistrationOtpRequestDTO invalidRequest
                = new VerifyRegistrationOtpRequestDTO("john@example.com", "12");

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

    @Test
    @WithMockUser(username = "john@example.com")
    void updateEmail_ShouldRouteToUserProfileService() throws Exception {
        EmailUpdateRequestDTO request = new EmailUpdateRequestDTO("new@example.com", "Password123!");

        mockMvc.perform(put("/api/auth/email")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(userProfileService).requestEmailChange("john@example.com", request);
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void updateEmail_ShouldReturnBadRequest_WhenNewEmailInvalid() throws Exception {
        EmailUpdateRequestDTO invalidRequest = new EmailUpdateRequestDTO("not-an-email", "Password123!");

        mockMvc.perform(put("/api/auth/email")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void verifyEmailChangeOtp_ShouldReturnOk_WhenCodeValid() throws Exception {
        VerifyEmailChangeOtpRequestDTO request = new VerifyEmailChangeOtpRequestDTO("123456");
        when(userProfileService.confirmEmailChange("john@example.com", request))
                .thenReturn(new AuthResponse("jwt-token", "refresh-token", "user-1", "John", "Doe", false, null));

        mockMvc.perform(post("/api/auth/email/verify-otp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt-token"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void verifyEmailChangeOtp_ShouldReturnBadRequest_WhenCodeNotSixDigits() throws Exception {
        VerifyEmailChangeOtpRequestDTO invalidRequest = new VerifyEmailChangeOtpRequestDTO("12");

        mockMvc.perform(post("/api/auth/email/verify-otp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void changePassword_ShouldReturnOk_WhenPayloadValid() throws Exception {
        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO("OldPassword123!", "NewPassword123!");

        mockMvc.perform(put("/api/auth/password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(passwordService).changePassword("john@example.com", request);
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void changePassword_ShouldReturnUnauthorized_WhenCurrentPasswordWrong() throws Exception {
        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO("WrongPassword1!", "NewPassword123!");
        org.mockito.Mockito.doThrow(new InvalidCredentialsException("Current password is incorrect"))
                .when(passwordService).changePassword("john@example.com", request);

        mockMvc.perform(put("/api/auth/password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void refreshToken_ShouldReturnOk_WhenTokenValid() throws Exception {
        TokenRefreshRequestDTO request = new TokenRefreshRequestDTO("refresh-token");
        when(authService.refreshToken(any(TokenRefreshRequestDTO.class)))
                .thenReturn(new AuthResponse("new-jwt-token", "new-refresh-token", "user-1", "John", "Doe", false, null));

        mockMvc.perform(post("/api/auth/refresh-token")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("new-jwt-token"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void logout_ShouldReturnOk_AndRouteCallerEmail() throws Exception {
        mockMvc.perform(post("/api/auth/logout").with(csrf()))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(authService).logout("john@example.com");
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void deactivateAccount_ShouldReturnOk_AndRouteCallerEmail() throws Exception {
        mockMvc.perform(patch("/api/auth/deactivate").with(csrf()))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(userProfileService).deactivateAccount("john@example.com");
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void updatePrivacySettings_ShouldReturnOk_AndRouteCallerEmail() throws Exception {
        PrivacySettingsUpdateRequestDTO request = new PrivacySettingsUpdateRequestDTO(true, false);

        mockMvc.perform(put("/api/auth/privacy")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(userProfileService).updatePrivacySettings("john@example.com", request);
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void requestAccountDeletion_ShouldReturnOk_AndRouteCallerEmail() throws Exception {
        DeleteAccountRequestDTO request = new DeleteAccountRequestDTO("Password123!");

        mockMvc.perform(delete("/api/auth/account")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(userProfileService).requestAccountDeletion("john@example.com", request);
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void resetPassword_ShouldReturnOk_WhenPayloadValid() throws Exception {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO("john@example.com", "123456", "NewPassword123!");

        mockMvc.perform(post("/api/auth/reset-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(passwordService).resetPassword(request);
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void updateAvatar_ShouldReturnOk_AndRouteCallerEmail() throws Exception {
        AvatarUpdateRequestDTO request = new AvatarUpdateRequestDTO("https://example.com/avatar.png");

        mockMvc.perform(put("/api/auth/avatar")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(userProfileService).updateAvatar("john@example.com", request.avatarUrl());
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void updateProfile_ShouldReturnOk_AndRouteCallerEmail() throws Exception {
        ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO("John", "Doe", "Springfield", "Loves cooking");

        mockMvc.perform(put("/api/auth/profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(userProfileService).updateProfile("john@example.com", request);
    }

    @Test
    void getCurrentUser_ShouldRejectAnonymousCaller() throws Exception {
        mockMvc.perform(get("/api/auth/me").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }
}
