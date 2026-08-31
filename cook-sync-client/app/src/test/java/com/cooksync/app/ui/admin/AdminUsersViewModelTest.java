package com.cooksync.app.ui.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.cooksync.app.data.repository.AdminRepository;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.domain.Event;
import com.cooksync.app.testutil.ApiResultAnswers;
import com.dtos.response.PagedResponse;
import com.dtos.response.user.UserResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

/**
 * Unit tests for {@link AdminUsersViewModel}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 12/08/2026
 */
public class AdminUsersViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AdminRepository adminRepository;
    private AdminUsersViewModel viewModel;

    private final UserResponse activeUser = new UserResponse("u1", "Ada", "Lovelace",
            "ada@example.com", false, null, "2026-01-01", "2026-01-01", true, "ACTIVE",
            null, null, true, true);
    private final UserResponse suspendedUser = new UserResponse("u2", "Grace", "Hopper",
            "grace@example.com", false, null, "2026-01-01", "2026-01-01", false, "SUSPENDED",
            null, null, true, true);

    @Before
    public void setUp() {
        adminRepository = mock(AdminRepository.class);
        viewModel = new AdminUsersViewModel(adminRepository);
    }

    @Test
    public void refreshUsers_publishesFirstPage() {
        loadOnePageContainingActiveUser();

        ApiResult<List<UserResponse>> result = viewModel.getUsersResult().getValue();
        assertTrue(result instanceof ApiResult.Success<List<UserResponse>>);
        assertEquals(List.of(activeUser), ((ApiResult.Success<List<UserResponse>>) result).getData());
        assertEquals(1, viewModel.getUsersTotalElements());
    }

    @Test
    public void refreshUsers_error_postsErrorResult() {
        doAnswer(ApiResultAnswers.<PagedResponse<UserResponse>>error("network error"))
                .when(adminRepository).getUsers(eq(0), eq(20), any(), any(), any(), any(), any());

        viewModel.refreshUsers(null, null);

        ApiResult<List<UserResponse>> result = viewModel.getUsersResult().getValue();
        assertTrue(result instanceof ApiResult.Error<List<UserResponse>>);
        assertEquals("network error", ((ApiResult.Error<List<UserResponse>>) result).getMessage());
    }

    @Test
    public void loadNextUsersPage_noOp_whenLastPageAlreadyLoaded() {
        loadOnePageContainingActiveUser();

        viewModel.loadNextUsersPage();

        verify(adminRepository, never()).getUsers(eq(1), eq(20), any(), any(), any(), any(), any());
    }

    @Test
    public void loadNextUsersPage_fetchesAndAppendsNextPage() {
        PagedResponse<UserResponse> firstPage = new PagedResponse<>(List.of(activeUser), 0, 20, 2, 2, false);
        doAnswer(ApiResultAnswers.success(firstPage))
                .when(adminRepository).getUsers(eq(0), eq(20), any(), any(), any(), any(), any());
        viewModel.refreshUsers(null, null);

        PagedResponse<UserResponse> secondPage = new PagedResponse<>(List.of(suspendedUser), 1, 20, 2, 2, true);
        doAnswer(ApiResultAnswers.success(secondPage))
                .when(adminRepository).getUsers(eq(1), eq(20), any(), any(), any(), any(), any());

        viewModel.loadNextUsersPage();

        ApiResult<List<UserResponse>> result = viewModel.getUsersResult().getValue();
        assertEquals(List.of(activeUser, suspendedUser), ((ApiResult.Success<List<UserResponse>>) result).getData());
        assertEquals(2, viewModel.getUsersTotalElements());
    }

    @Test
    public void toggleUsersSortDirection_flipsDirection_andReloadsFromFirstPage() {
        loadOnePageContainingActiveUser();

        viewModel.toggleUsersSortDirection();

        verify(adminRepository).getUsers(eq(0), eq(20), any(), any(), eq("createdAt"), eq("asc"), any());
    }

    @Test
    public void setUserEnabled_enable_routesToEnableUser_andDoesNotFireDisabledEvent() {
        loadOnePageContainingSuspendedUser();
        doAnswer(ApiResultAnswers.<Void>success(null))
                .when(adminRepository).enableUser(eq("u2"), any());

        viewModel.setUserEnabled(suspendedUser, true);

        UserResponse patched = firstUser();
        assertTrue(patched.enabled());
        assertEquals("ACTIVE", patched.status());
        assertNull(viewModel.getUserDisabledEvent().getValue());

        viewModel.onCleared();

        verify(adminRepository).enableUser(eq("u2"), any());
        verify(adminRepository, never()).suspendUser(eq("u2"), any());
    }

    @Test
    public void setUserEnabled_disable_appliesOptimisticPatch_andFiresUserDisabledEvent() {
        loadOnePageContainingActiveUser();

        viewModel.setUserEnabled(activeUser, false);

        UserResponse patched = firstUser();
        assertFalse(patched.enabled());
        assertEquals("SUSPENDED", patched.status());

        Event<String> event = viewModel.getUserDisabledEvent().getValue();
        assertEquals("u1", event.getContentIfNotHandled());
    }

    @Test
    public void setUserEnabled_disable_undoneBeforeFlush_restoresRow_andNeverCallsRepository() {
        loadOnePageContainingActiveUser();

        viewModel.setUserEnabled(activeUser, false);
        viewModel.undoSetUserEnabled(activeUser);

        UserResponse restored = firstUser();
        assertTrue(restored.enabled());
        assertEquals("ACTIVE", restored.status());
        verify(adminRepository, never()).suspendUser(any(), any());
    }

    @Test
    public void setUserEnabled_disable_serverErrorAfterFlush_rollsBackAndSignalsResync() {
        loadOnePageContainingActiveUser();
        doAnswer(ApiResultAnswers.<Void>error("network error"))
                .when(adminRepository).suspendUser(eq("u1"), any());

        viewModel.setUserEnabled(activeUser, false);
        viewModel.onCleared();

        UserResponse rolledBack = firstUser();
        assertTrue(rolledBack.enabled());
        assertEquals("ACTIVE", rolledBack.status());
        assertTrue(viewModel.getReportsResyncNeeded().getValue().getContentIfNotHandled());
    }

    @Test
    public void deleteUser_success_removesRow_decrementsCount_andFiresDeletedEvent() {
        loadOnePageContainingActiveUser();
        doAnswer(ApiResultAnswers.<Void>success(null))
                .when(adminRepository).deleteUser(eq("u1"), any());

        viewModel.deleteUser(activeUser);

        ApiResult<List<UserResponse>> result = viewModel.getUsersResult().getValue();
        assertTrue(((ApiResult.Success<List<UserResponse>>) result).getData().isEmpty());
        assertEquals(0, viewModel.getUsersTotalElements());

        ApiResult<String> deleteResult = viewModel.getUserDeletedResult().getValue().getContentIfNotHandled();
        assertTrue(deleteResult instanceof ApiResult.Success<String>);
        assertEquals("Ada Lovelace", ((ApiResult.Success<String>) deleteResult).getData());
    }

    @Test
    public void deleteUser_serverError_firesErrorEvent_andKeepsRow() {
        loadOnePageContainingActiveUser();
        doAnswer(ApiResultAnswers.<Void>error("network error"))
                .when(adminRepository).deleteUser(eq("u1"), any());

        viewModel.deleteUser(activeUser);

        assertEquals(activeUser, firstUser());
        assertEquals(1, viewModel.getUsersTotalElements());

        ApiResult<String> deleteResult = viewModel.getUserDeletedResult().getValue().getContentIfNotHandled();
        assertTrue(deleteResult instanceof ApiResult.Error<String>);
        assertEquals("network error", ((ApiResult.Error<String>) deleteResult).getMessage());
    }

    private UserResponse firstUser() {
        ApiResult<List<UserResponse>> result = viewModel.getUsersResult().getValue();
        return ((ApiResult.Success<List<UserResponse>>) result).getData().get(0);
    }

    private void loadOnePageContainingActiveUser() {
        PagedResponse<UserResponse> page = new PagedResponse<>(List.of(activeUser), 0, 20, 1, 1, true);
        doAnswer(ApiResultAnswers.success(page))
                .when(adminRepository).getUsers(eq(0), eq(20), any(), any(), any(), any(), any());
        viewModel.refreshUsers(null, null);
    }

    private void loadOnePageContainingSuspendedUser() {
        PagedResponse<UserResponse> page = new PagedResponse<>(List.of(suspendedUser), 0, 20, 1, 1, true);
        doAnswer(ApiResultAnswers.success(page))
                .when(adminRepository).getUsers(eq(0), eq(20), any(), any(), any(), any(), any());
        viewModel.refreshUsers(null, null);
    }
}
