package com.cooksync.app.ui.settings;
import com.cooksync.app.ui.base.BaseActivity;

import android.os.Bundle;

import com.cooksync.app.R;
import com.cooksync.app.data.datasource.local.CookingPreferencesStore;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * Screen for editing device-level cooking preferences: whether cooking mode keeps the screen
 * awake, and whether a finished step timer plays a sound and vibrates. Reads and writes
 * {@link CookingPreferencesStore} directly rather than through a ViewModel or repository, since
 * these settings are local to the device and carry no network or account dependency.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 07/08/2026
 */
public class CookingPreferencesActivity extends BaseActivity {

    /**
     * Binds the screen-awake and timer-sound switches to their stored {@link CookingPreferencesStore}
     * values.
     *
     * @param savedInstanceState previously saved instance state, unused
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cooking_preferences);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        SwitchMaterial switchScreenAwake = findViewById(R.id.switch_screen_awake);
        switchScreenAwake.setChecked(CookingPreferencesStore.isScreenAwakeEnabled());
        switchScreenAwake.setOnCheckedChangeListener((button, checked) ->
                CookingPreferencesStore.setScreenAwakeEnabled(checked));

        SwitchMaterial switchTimerSound = findViewById(R.id.switch_timer_sound);
        switchTimerSound.setChecked(CookingPreferencesStore.isTimerSoundEnabled());
        switchTimerSound.setOnCheckedChangeListener((button, checked) ->
                CookingPreferencesStore.setTimerSoundEnabled(checked));
    }
}
