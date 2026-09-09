package com.cooksync.app.ui.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.data.repository.AuthRepository;
import com.cooksync.app.data.repository.MediaRepository;
import com.cooksync.app.data.repository.RecipeRepository;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.testutil.ApiResultAnswers;
import com.dtos.request.auth.EmailUpdateRequestDTO;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Unit tests for {@link SettingsViewModel}'s combined-outcome batching in
 * {@link SettingsViewModel#saveAccountChanges} and the resend/cooldown gating of
 * {@link SettingsViewModel#resendEmailChangeOtp()}. Every field supplied is deliberately valid
 * so no {@link com.cooksync.app.util.InputValidator} failure branch runs — those branches call
 * {@link com.cooksync.app.CookSyncApplication#getAppContext()}, which is only populated by a real
 * {@code Application.onCreate()} and stays {@code null} in this plain-JVM test environment.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/09/2026
 */
public class SettingsViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AuthRepository authRepository;
    private SettingsViewModel viewModel;

    @Before
    public void setUp() {
        authRepository = mock(AuthRepository.class);
        MediaRepository mediaRepository = mock(MediaRepository.class);
        RecipeRepository recipeRepository = mock(RecipeRepository.class);
        viewModel = new SettingsViewModel(authRepository, mediaRepository, recipeRepository);
    }

    @Test
    public void saveAccountChanges_reportsSuccess_whenProfileAndPrivacyBothSucceed_andNoPasswordChangeRequested() {
        doAnswer(ApiResultAnswers.<Void>success(null)).when(authRepository).updateProfile(any(), any());
        doAnswer(ApiResultAnswers.<Void>success(null)).when(authRepository).updatePrivacySettings(any(), any());

        viewModel.saveAccountChanges("Jane", "Doe", "Tel Aviv", "Home cook.",
                true, false, "", "", "");

        ApiResult<Void> result = viewModel.getSaveChangesResult().getValue().getContentIfNotHandled();
        assertTrue(result instanceof ApiResult.Success);
        verify(authRepository, never()).changePassword(any(), any());
    }

    @Test
    public void saveAccountChanges_reportsError_whenPrivacyUpdateFails_andNoPasswordChangeRequested() {
        doAnswer(ApiResultAnswers.<Void>success(null)).when(authRepository).updateProfile(any(), any());
        doAnswer(ApiResultAnswers.<Void>error("Privacy update failed")).when(authRepository).updatePrivacySettings(any(), any());

        viewModel.saveAccountChanges("Jane", "Doe", "Tel Aviv", "Home cook.",
                true, false, "", "", "");

        ApiResult<Void> result = viewModel.getSaveChangesResult().getValue().getContentIfNotHandled();
        assertTrue(result instanceof ApiResult.Error);
        assertEquals("Privacy update failed", ((ApiResult.Error<Void>) result).getMessage());
    }

    @Test
    public void saveAccountChanges_firesAllThreeCalls_andReportsSuccess_whenChangingPassword() {
        doAnswer(ApiResultAnswers.<Void>success(null)).when(authRepository).updateProfile(any(), any());
        doAnswer(ApiResultAnswers.<Void>success(null)).when(authRepository).updatePrivacySettings(any(), any());
        doAnswer(ApiResultAnswers.<Void>success(null)).when(authRepository).changePassword(any(), any());

        viewModel.saveAccountChanges("Jane", "Doe", "Tel Aviv", "Home cook.",
                true, false, "OldPass1!", "NewPass1!", "NewPass1!");

        ApiResult<Void> result = viewModel.getSaveChangesResult().getValue().getContentIfNotHandled();
        assertTrue(result instanceof ApiResult.Success);
        verify(authRepository, times(1)).changePassword(any(), any());
    }

    @Test
    public void saveAccountChanges_reportsError_whenPasswordChangeFails_amongThreeCalls() {
        doAnswer(ApiResultAnswers.<Void>success(null)).when(authRepository).updateProfile(any(), any());
        doAnswer(ApiResultAnswers.<Void>success(null)).when(authRepository).updatePrivacySettings(any(), any());
        doAnswer(ApiResultAnswers.<Void>error("Current password is incorrect")).when(authRepository).changePassword(any(), any());

        viewModel.saveAccountChanges("Jane", "Doe", "Tel Aviv", "Home cook.",
                true, false, "OldPass1!", "NewPass1!", "NewPass1!");

        ApiResult<Void> result = viewModel.getSaveChangesResult().getValue().getContentIfNotHandled();
        assertTrue(result instanceof ApiResult.Error);
        assertEquals("Current password is incorrect", ((ApiResult.Error<Void>) result).getMessage());
    }

    @Test
    public void saveAccountChanges_skipsPasswordChange_whenNewPasswordFieldIsBlank() {
        doAnswer(ApiResultAnswers.<Void>success(null)).when(authRepository).updateProfile(any(), any());
        doAnswer(ApiResultAnswers.<Void>success(null)).when(authRepository).updatePrivacySettings(any(), any());

        viewModel.saveAccountChanges("Jane", "Doe", "", "",
                false, false, "", "", "");

        verify(authRepository, never()).changePassword(any(), any());
    }

    @Test
    public void resendEmailChangeOtp_isNoOp_whileCooldownStillRunning() {
        @SuppressWarnings("unchecked")
        MutableLiveData<Integer> cooldown = (MutableLiveData<Integer>) viewModel.getEmailOtpResendCooldownSeconds();
        cooldown.setValue(20);

        viewModel.resendEmailChangeOtp();

        verify(authRepository, never()).requestEmailChange(any(), any());
    }

    @Test
    public void resendEmailChangeOtp_firesRequest_whenCooldownHasElapsed() {
        doAnswer(ApiResultAnswers.<Void>error("network error")).when(authRepository).requestEmailChange(any(), any());

        viewModel.resendEmailChangeOtp();

        verify(authRepository, times(1)).requestEmailChange(any(), any());
    }

    @Test
    public void resendEmailChangeOtp_reusesTheStashedPendingRequest() {
        doAnswer(ApiResultAnswers.<Void>error("network error")).when(authRepository).requestEmailChange(any(), any());

        viewModel.resendEmailChangeOtp();

        verify(authRepository, times(1)).requestEmailChange(
                eq(new EmailUpdateRequestDTO(null, null)), any());
    }
}
