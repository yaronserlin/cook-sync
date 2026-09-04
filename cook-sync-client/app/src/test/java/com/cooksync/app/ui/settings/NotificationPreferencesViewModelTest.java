package com.cooksync.app.ui.settings;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.cooksync.app.data.repository.NotificationPreferencesRepository;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.testutil.ApiResultAnswers;
import com.dtos.response.notification.NotificationPreferencesResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Unit tests for {@link NotificationPreferencesViewModel}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public class NotificationPreferencesViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private NotificationPreferencesRepository notificationPreferencesRepository;
    private NotificationPreferencesViewModel viewModel;

    @Before
    public void setUp() {
        notificationPreferencesRepository = mock(NotificationPreferencesRepository.class);
        viewModel = new NotificationPreferencesViewModel(notificationPreferencesRepository);
    }

    @Test
    public void loadPreferences_publishesSuccess() {
        NotificationPreferencesResponse preferences = new NotificationPreferencesResponse(true, false);
        doAnswer(ApiResultAnswers.success(preferences)).when(notificationPreferencesRepository).getPreferences(any());

        viewModel.loadPreferences();

        ApiResult<NotificationPreferencesResponse> result = viewModel.getPreferencesResult().getValue();
        assertTrue(result instanceof ApiResult.Success<NotificationPreferencesResponse>);
        NotificationPreferencesResponse data = ((ApiResult.Success<NotificationPreferencesResponse>) result).getData();
        assertTrue(data.systemAnnouncements());
        assertTrue(!data.pushEnabled());
    }

    @Test
    public void setSystemAnnouncements_forwardsBothCurrentValuesToRepository() {
        // The server endpoint replaces the whole preferences object, so toggling one switch must
        // still send the other switch's current (unchanged) value, not just the one that changed.
        viewModel.setSystemAnnouncements(false, true);

        verify(notificationPreferencesRepository).updatePreferences(eq(false), eq(true), any());
    }

    @Test
    public void setPushEnabled_forwardsBothCurrentValuesToRepository() {
        viewModel.setPushEnabled(true, false);

        verify(notificationPreferencesRepository).updatePreferences(eq(true), eq(false), any());
    }
}
