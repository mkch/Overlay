package com.farproc.overlay;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.SeekBar;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREF_KEY_FORCE_IMMERSIVE = "pref_key_force_immersive";
    public static final String PREF_KEY_FORCE_ROTATION = "pref_key_force_rotation";
    public static final String PREF_KEY_BLUE_FILTER = "pref_key_blue_filter";
    public static final String PREF_KEY_BLUE_FILTER_LEVEL = "pref_key_blue_filter_level";
    public static final String PREF_KEY_KEEP_SCREEN_ON = "pref_key_keep_screen_on";

    public static class PrefsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.settings);
            setupPreferences();
        }

        private class BlueFilterLevelListener implements SeekBar.OnSeekBarChangeListener, SeekBarPreference.OnShowDialogListener, SeekBarPreference.OnDialogClosedListener {

            private OverlayService mService;
            private ServiceConnection mConn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    mService = ((OverlayService.LocalBinder)service).getService();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    mService = null;
                }
            };

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mService != null) {
                    mService.setBlueFilterLevel(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onDialogClosed(boolean positiveResult) {
                final Activity activity = getActivity();
                activity.unbindService(mConn);
            }

            @Override
            public void onShowDialog() {
                final Activity activity = getActivity();
                activity.bindService(new Intent(activity, OverlayService.class), mConn, 0);
            }
        }

        private BlueFilterLevelListener mBlueFilterLevelListener = new BlueFilterLevelListener();

        private void setupPreferences() {
            findPreference(PREF_KEY_FORCE_IMMERSIVE).setOnPreferenceChangeListener(this);
            findPreference(PREF_KEY_FORCE_ROTATION).setOnPreferenceChangeListener(this);
            findPreference(PREF_KEY_BLUE_FILTER).setOnPreferenceChangeListener(this);

            final SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            if(!prefs.contains(PREF_KEY_BLUE_FILTER_LEVEL)) {
                prefs.edit().putInt(PREF_KEY_BLUE_FILTER_LEVEL, 50).commit();
            }
            final SeekBarPreference blueFilterLevel = (SeekBarPreference)findPreference(PREF_KEY_BLUE_FILTER_LEVEL);
            blueFilterLevel.setOnPreferenceChangeListener(this);
            blueFilterLevel.setOnSeekbarChangedListener(mBlueFilterLevelListener);
            blueFilterLevel.setOnShowDialogListener(mBlueFilterLevelListener);
            blueFilterLevel.setOnDialogClosedListener(mBlueFilterLevelListener);
            blueFilterLevel.setEnabled(prefs.getBoolean(PREF_KEY_BLUE_FILTER, false));

            findPreference(PREF_KEY_KEEP_SCREEN_ON).setOnPreferenceChangeListener(this);

            startOverlayServiceIfNeeded(getActivity());
        }

        public static void startOverlayServiceIfNeeded(Context context) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if(prefs.getBoolean(PREF_KEY_FORCE_IMMERSIVE, false) ||
                    prefs.getBoolean(PREF_KEY_FORCE_ROTATION, false) ||
                    prefs.getBoolean(PREF_KEY_BLUE_FILTER, false)) {
                startOverlayService(context);
            }
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final Activity activity = getActivity();
            switch(preference.getKey()) {
                case PREF_KEY_FORCE_IMMERSIVE:
                case PREF_KEY_FORCE_ROTATION:
                case PREF_KEY_BLUE_FILTER:
                case PREF_KEY_KEEP_SCREEN_ON:
                    if((boolean)newValue) {
                        startOverlayService(activity);
                    }
                    if(preference.getKey().equals(PREF_KEY_BLUE_FILTER)) {
                        findPreference(PREF_KEY_BLUE_FILTER_LEVEL).setEnabled((boolean)newValue);
                    }
                    break;
                case PREF_KEY_BLUE_FILTER_LEVEL:
                    startOverlayService(activity);
            }
            return true;
        }

        private static void startOverlayService(Context context) {
            // The service is responsible to stop itself.
            context.startService(new Intent(context, OverlayService.class));
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .add(android.R.id.content, new PrefsFragment())
                .commit();
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
    }
}

