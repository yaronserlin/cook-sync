package com.cooksync.app.data.repository.impl;

import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.data.datasource.local.TokenStore;
import com.cooksync.app.data.datasource.remote.ApiService;
import com.cooksync.app.data.datasource.remote.RetrofitClient;
import com.cooksync.app.data.repository.AuthRepository;
import com.cooksync.app.data.repository.BaseRepository;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.util.SessionManager;
import com.dtos.request.auth.AvatarUpdateRequestDTO;
import com.dtos.request.auth.ChangePasswordRequestDTO;
import com.dtos.request.auth.DeleteAccountRequestDTO;
import com.dtos.request.auth.EmailUpdateRequestDTO;
import com.dtos.request.auth.ForgotPasswordRequestDTO;
import com.dtos.request.auth.LoginRequestDTO;
import com.dtos.request.auth.PrivacySettingsUpdateRequestDTO;
import com.dtos.request.auth.ProfileUpdateRequestDTO;
import com.dtos.request.auth.RegisterRequestDTO;
import com.dtos.request.auth.ResendRegistrationOtpRequestDTO;
import com.dtos.request.auth.ResetPasswordRequestDTO;
import com.dtos.request.auth.TokenRefreshRequestDTO;
import com.dtos.request.auth.VerifyEmailChangeOtpRequestDTO;
import com.dtos.request.auth.VerifyRegistrationOtpRequestDTO;
import com.dtos.response.auth.AuthResponse;
import com.dtos.response.auth.PendingRegistrationResponse;
import com.dtos.response.user.PublicUserProfileResponse;
import com.dtos.response.user.UserResponse;

/**
 * Concrete implementation of {@link AuthRepository} that delegates every call to the remote
 * REST API via Retrofit, executes the network work on a dedicated background thread pool
 * (inherited from {@link BaseRepository}), and posts typed {@link ApiResult} values back to the
 * caller-supplied {@link MutableLiveData} targets on the main thread.
 *
 * <p>Session side effects — persisting tokens and broadcasting login/logout state — are handled
 * here through {@link SessionManager}, so neither the ViewModel nor the UI layer ever touches raw
 * token strings.</p>
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
public class AuthRepositoryImp extends BaseRepository implements AuthRepository {

    private final ApiService apiService;

    /**
     * Constructs the repository against the shared authenticated Retrofit service.
     */
    public AuthRepositoryImp() {
        this.apiService = RetrofitClient.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Posts {@link ApiResult.Loading} immediately, then executes the login call
     * asynchronously. On HTTP 200 with {@code success=true}, the session is started and
     * {@link ApiResult.Success} is posted; on any other outcome, {@link ApiResult.Error} is
     * posted with a user-facing message.</p>
     */
    @Override
    public void login(LoginRequestDTO request, MutableLiveData<ApiResult<AuthResponse>> resultTarget) {
        resultTarget.postValue(new ApiResult.Loading<>());
        EXECUTOR.execute(() -> {
            ApiResult<AuthResponse> result = executeCall(apiService.login(request));
            if (result instanceof ApiResult.Success) {
                SessionManager.getInstance().startSession(((ApiResult.Success<AuthResponse>) result).getData());
                SessionManager.getInstance().cacheEmail(request.email());
            }
            resultTarget.postValue(result);
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Posts {@link ApiResult.Loading} immediately, then executes the registration call
     * asynchronously. No session is started here: the server only emails an OTP code at this
     * stage, and the session begins once {@link #verifyRegistrationOtp} succeeds.</p>
     */
    @Override
    public void register(RegisterRequestDTO request, MutableLiveData<ApiResult<PendingRegistrationResponse>> resultTarget) {
        executeAsync(apiService.register(request), resultTarget);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Posts {@link ApiResult.Loading} immediately, then executes the OTP verification call
     * asynchronously. On success the session is started immediately, exactly as registration
     * itself used to before the OTP step was introduced.</p>
     */
    @Override
    public void verifyRegistrationOtp(VerifyRegistrationOtpRequestDTO request, MutableLiveData<ApiResult<AuthResponse>> resultTarget) {
        resultTarget.postValue(new ApiResult.Loading<>());
        EXECUTOR.execute(() -> {
            ApiResult<AuthResponse> result = executeCall(apiService.verifyRegistrationOtp(request));
            if (result instanceof ApiResult.Success) {
                SessionManager.getInstance().startSession(((ApiResult.Success<AuthResponse>) result).getData());
                SessionManager.getInstance().cacheEmail(request.email());
            }
            resultTarget.postValue(result);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resendRegistrationOtp(ResendRegistrationOtpRequestDTO request, MutableLiveData<ApiResult<PendingRegistrationResponse>> resultTarget) {
        executeAsync(apiService.resendRegistrationOtp(request), resultTarget);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Calls the server logout endpoint to invalidate the refresh token server-side, then
     * clears the local session regardless of the server response, so a network failure never
     * leaves the user stuck in an apparently logged-in state.</p>
     */
    @Override
    public void logout(MutableLiveData<ApiResult<Void>> resultTarget) {
        resultTarget.postValue(new ApiResult.Loading<>());
        EXECUTOR.execute(() -> {
            ApiResult<Void> serverResult = executeCall(apiService.logout());
            if (serverResult instanceof ApiResult.Error<Void> error) {
                android.util.Log.w("AuthRepositoryImp", "Server logout request failed: " + error.getMessage());
            }
            SessionManager.getInstance().logout();
            resultTarget.postValue(new ApiResult.Success<>(null));
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>On success, the new first/last name are also cached locally, since the server response
     * carries no body to read them back from.</p>
     */
    @Override
    public void updateProfile(ProfileUpdateRequestDTO request, MutableLiveData<ApiResult<Void>> resultTarget) {
        resultTarget.postValue(new ApiResult.Loading<>());
        EXECUTOR.execute(() -> {
            ApiResult<Void> result = executeCall(apiService.updateProfile(request));
            if (result instanceof ApiResult.Success) {
                SessionManager.getInstance().updateCachedProfile(request.firstName(), request.lastName());
            }
            resultTarget.postValue(result);
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>On success, the new avatar URL is also cached locally, since the server response
     * carries no body to read it back from.</p>
     */
    @Override
    public void updateAvatar(AvatarUpdateRequestDTO request, MutableLiveData<ApiResult<Void>> resultTarget) {
        resultTarget.postValue(new ApiResult.Loading<>());
        EXECUTOR.execute(() -> {
            ApiResult<Void> result = executeCall(apiService.updateAvatar(request));
            if (result instanceof ApiResult.Success) {
                SessionManager.getInstance().updateCachedAvatar(request.avatarUrl());
            }
            resultTarget.postValue(result);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changePassword(ChangePasswordRequestDTO request, MutableLiveData<ApiResult<Void>> resultTarget) {
        executeAsync(apiService.changePassword(request), resultTarget);
    }

    /**
     * {@inheritDoc}
     *
     * <p>No session change happens here — the server only emails a verification code at this
     * stage, and the session is renewed once {@link #verifyEmailChangeOtp} succeeds.</p>
     */
    @Override
    public void requestEmailChange(EmailUpdateRequestDTO request, MutableLiveData<ApiResult<Void>> resultTarget) {
        executeAsync(apiService.updateEmail(request), resultTarget);
    }

    /**
     * {@inheritDoc}
     *
     * <p>On success, the new session — carrying renewed tokens issued for the new email
     * identity — is persisted through {@link SessionManager}.</p>
     */
    @Override
    public void verifyEmailChangeOtp(VerifyEmailChangeOtpRequestDTO request, String newEmail,
                                      MutableLiveData<ApiResult<AuthResponse>> resultTarget) {
        resultTarget.postValue(new ApiResult.Loading<>());
        EXECUTOR.execute(() -> {
            ApiResult<AuthResponse> result = executeCall(apiService.verifyEmailChangeOtp(request));
            if (result instanceof ApiResult.Success) {
                SessionManager.getInstance().startSession(((ApiResult.Success<AuthResponse>) result).getData());
                SessionManager.getInstance().cacheEmail(newEmail);
            }
            resultTarget.postValue(result);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getCurrentUserProfile(MutableLiveData<ApiResult<UserResponse>> resultTarget) {
        executeAsync(apiService.getCurrentUser(), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getUserProfile(String userId, MutableLiveData<ApiResult<PublicUserProfileResponse>> resultTarget) {
        executeAsync(apiService.getUserProfile(userId), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updatePrivacySettings(PrivacySettingsUpdateRequestDTO request, MutableLiveData<ApiResult<Void>> resultTarget) {
        executeAsync(apiService.updatePrivacySettings(request), resultTarget);
    }

    /**
     * {@inheritDoc}
     *
     * <p>On success, the local session is also cleared: the account is disabled server-side for
     * the duration of the 30-day grace period, so the user is signed out immediately and must
     * log back in to cancel the deletion.</p>
     */
    @Override
    public void requestAccountDeletion(DeleteAccountRequestDTO request, MutableLiveData<ApiResult<Void>> resultTarget) {
        resultTarget.postValue(new ApiResult.Loading<>());
        EXECUTOR.execute(() -> {
            ApiResult<Void> result = executeCall(apiService.requestAccountDeletion(request));
            if (result instanceof ApiResult.Success) {
                SessionManager.getInstance().logout();
            }
            resultTarget.postValue(result);
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Executes a silent validation/refresh flow intended for app startup:</p>
     * <ol>
     *   <li>Calls {@code GET /api/auth/validate-token} with the current access token.</li>
     *   <li>If the access token is expired (HTTP 401), {@link com.cooksync.app.data.datasource.remote.TokenAuthenticator}
     *       transparently attempts a refresh. If that succeeds, the call is retried and a
     *       {@link ApiResult.Success} is received here.</li>
     *   <li>If validation is still failing for any other reason (e.g. network failure or
     *       authenticator failure), this method manually attempts a refresh using the stored
     *       refresh token via the bare (unauthenticated) API service, to guarantee a clean
     *       terminal state.</li>
     *   <li>On any ultimate success, the cached profile is updated and the user identity is
     *       posted. On ultimate failure, the local session is cleared.</li>
     * </ol>
     */
    @Override
    public void validateToken(MutableLiveData<ApiResult<AuthResponse>> resultTarget) {
        resultTarget.postValue(new ApiResult.Loading<>());
        EXECUTOR.execute(() -> {
            ApiResult<AuthResponse> validateResult = executeCall(apiService.validateToken());

            if (validateResult instanceof ApiResult.Success) {
                SessionManager.getInstance().refreshCachedProfile(((ApiResult.Success<AuthResponse>) validateResult).getData());
                resultTarget.postValue(validateResult);
                return;
            }

            ApiResult<AuthResponse> terminalResult;

            String refreshToken = TokenStore.getRefreshToken();
            if (refreshToken != null && !refreshToken.isEmpty()) {
                // The bare (unauthenticated) service is used here to avoid a recursive authenticator loop.
                ApiResult<AuthResponse> refreshResult = executeCall(
                        RetrofitClient.getBareService().refreshToken(new TokenRefreshRequestDTO(refreshToken))
                );

                if (refreshResult instanceof ApiResult.Success) {
                    AuthResponse renewed = ((ApiResult.Success<AuthResponse>) refreshResult).getData();
                    SessionManager.getInstance().startSession(renewed);
                    // The refresh response itself carries the full user profile, so this counts as a valid terminal state.
                    resultTarget.postValue(refreshResult);
                    return;
                }
                terminalResult = refreshResult;
            } else {
                terminalResult = validateResult;
            }

            SessionManager.getInstance().forceLogout();
            resultTarget.postValue(terminalResult);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void forgotPassword(ForgotPasswordRequestDTO request, MutableLiveData<ApiResult<Void>> resultTarget) {
        executeAsync(apiService.forgotPassword(request), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetPassword(ResetPasswordRequestDTO request, MutableLiveData<ApiResult<Void>> resultTarget) {
        executeAsync(apiService.resetPassword(request), resultTarget);
    }

}
