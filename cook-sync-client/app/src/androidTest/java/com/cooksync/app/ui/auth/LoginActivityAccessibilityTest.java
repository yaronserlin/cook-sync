package com.cooksync.app.ui.auth;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.accessibility.AccessibilityChecks;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.cooksync.app.R;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Runs Google's Accessibility Test Framework checks (missing labels, touch targets
 * smaller than 48dp, low text contrast, ...) over the whole sign-in screen.
 * Once enabled, the checks run on every Espresso view action in this test process,
 * so later UI tests are checked too. A failure lists each problem and the view it
 * was found on.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 24/09/2026
 */
@RunWith(AndroidJUnit4.class)
public class LoginActivityAccessibilityTest {

    @BeforeClass
    public static void enableAccessibilityChecks() {
        AccessibilityChecks.enable().setRunChecksFromRootView(true);
    }

    @Test
    public void loginScreen_passesAccessibilityChecks() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            // Any view action triggers the checks on the full view hierarchy.
            onView(withId(R.id.et_email)).perform(click());
        }
    }
}
