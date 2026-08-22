package com.cooksync.app.util;

import android.os.CountDownTimer;

import androidx.lifecycle.MutableLiveData;

/**
 * Reusable countdown timer that drives a "resend code" button's disabled-cooldown period,
 * posting the remaining whole seconds to a caller-owned {@link MutableLiveData} once per
 * second until it reaches zero. Shared by every screen with a resend-with-cooldown OTP/reset
 * code action (registration OTP verification, forgot-password reset), so the countdown
 * arithmetic and cancellation handling live in exactly one place.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 22/08/2026
 */
public final class ResendCooldownTimer {

    private final int seconds;
    private final MutableLiveData<Integer> secondsRemaining;

    private CountDownTimer countDownTimer;

    /**
     * Constructs a cooldown timer bound to the given LiveData target.
     *
     * @param seconds duration of the cooldown, in whole seconds
     * @param secondsRemaining the LiveData stream ticks are posted to
     */
    public ResendCooldownTimer(int seconds, MutableLiveData<Integer> secondsRemaining) {
        this.seconds = seconds;
        this.secondsRemaining = secondsRemaining;
    }

    /**
     * (Re)starts the countdown from the configured duration, cancelling any timer already
     * running.
     */
    public void start() {
        cancel();
        secondsRemaining.setValue(seconds);
        countDownTimer = new CountDownTimer(seconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                secondsRemaining.setValue((int) Math.ceil(millisUntilFinished / 1000.0));
            }

            @Override
            public void onFinish() {
                secondsRemaining.setValue(0);
            }
        }.start();
    }

    /**
     * Cancels the running countdown, if any. Safe to call when no countdown is active.
     */
    public void cancel() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }
}
