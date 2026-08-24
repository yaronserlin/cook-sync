package com.cooksync.app.ui.base;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.util.ResendCooldownTimer;

import java.util.function.Consumer;

/**
 * Shared base for all ViewModels in the application. Provides common utilities
 * like one-shot LiveData observation to reduce boilerplate in feature ViewModels.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 07/08/2026
 */
public abstract class BaseViewModel extends ViewModel {

    /**
     * Attaches a self-removing observer to a one-shot repository call: skips the initial
     * {@link ApiResult.Loading} emission, invokes {@code onSettled} for the terminal
     * Success/Error value, then detaches itself.
     *
     * @param <T> the payload type carried by the result
     * @param liveData the one-shot result stream to observe
     * @param onSettled callback invoked with the first non-Loading value
     */
    protected <T> void observeOnce(MutableLiveData<ApiResult<T>> liveData, Consumer<ApiResult<T>> onSettled) {
        liveData.observeForever(new Observer<>() {
            @Override
            public void onChanged(ApiResult<T> value) {
                if (value instanceof ApiResult.Loading) {
                    return;
                }
                liveData.removeObserver(this);
                onSettled.accept(value);
            }
        });
    }

    /**
     * Fires a "resend code" action gated by a cooldown timer: a no-op while the cooldown from a
     * previous send is still running, otherwise triggers {@code networkCall} and restarts
     * {@code cooldownTimer} once it resolves successfully. Shared by every screen with a
     * resend-with-cooldown OTP/reset code action (registration OTP verification, forgot-password
     * reset, email-change verification), so this "check cooldown, call, restart on success"
     * sequence lives in exactly one place.
     *
     * @param <T> the payload type carried by the result the resend call posts to
     * @param cooldownSeconds the LiveData stream {@code cooldownTimer} ticks are posted to
     * @param cooldownTimer the countdown timer to restart once the resend call succeeds
     * @param resultTarget the one-shot result stream the resend call posts to
     * @param networkCall invoked to actually fire the resend request through the repository
     */
    protected <T> void resendWithCooldown(MutableLiveData<Integer> cooldownSeconds, ResendCooldownTimer cooldownTimer,
                                           MutableLiveData<ApiResult<T>> resultTarget, Runnable networkCall) {
        Integer cooldown = cooldownSeconds.getValue();
        if (cooldown != null && cooldown > 0) {
            return;
        }
        observeOnce(resultTarget, result -> {
            if (result instanceof ApiResult.Success) {
                cooldownTimer.start();
            }
        });
        networkCall.run();
    }
}
