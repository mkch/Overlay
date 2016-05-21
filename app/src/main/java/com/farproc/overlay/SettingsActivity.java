package com.farproc.overlay;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;

public class SettingsActivity extends Activity {

    public static final String ACTION_TURN_OFF = "com.farproc.overlay.action.TURN_OFF";

    public static final String PREF_KEY_FORCE_IMMERSIVE = "pref_key_force_immersive";
    public static final String PREF_KEY_FORCE_ROTATION = "pref_key_force_rotation";
    public static final String PREF_KEY_BLUE_FILTER = "pref_key_blue_filter";
    public static final String PREF_KEY_BLUE_FILTER_LEVEL = "pref_key_blue_filter_level";
    public static final String PREF_KEY_KEEP_SCREEN_ON = "pref_key_keep_screen_on";
    public static final String PREF_KEY_MASTER_SWITCH = "pref_key_master_switch";

    public static class PrefsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        private SharedPreferences mPrefs;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.settings);
            mPrefs = getPreferenceManager().getSharedPreferences();
            setupPreferences();
        }

        @Override
        public void onResume() {
            super.onResume();
            mPrefs.registerOnSharedPreferenceChangeListener(this);
            getActivity().invalidateOptionsMenu();
            setPreferencesEnabledState(mPrefs.getBoolean(PREF_KEY_MASTER_SWITCH, false));
        }

        @Override
        public void onPause() {
            super.onPause();
            mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.settings_opt, menu);
            final Switch masterSwitch = (Switch) menu.findItem(R.id.master_switch).getActionView();
            // Disables saving instance state of switch to avoid inconsistent state of the switch and
            // the SharedPreferences caused by saved instance state.
            masterSwitch.setSaveEnabled(false);
            masterSwitch.setChecked(false);
            masterSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    ((Switch) buttonView).setText(isChecked ? R.string.action_on : R.string.action_off);
                    if(mPrefs.getBoolean(PREF_KEY_MASTER_SWITCH, false) != isChecked) {
                        mPrefs.edit().putBoolean(PREF_KEY_MASTER_SWITCH, isChecked).commit();
                    }
                }
            });
        }

        @Override
        public void onPrepareOptionsMenu (Menu menu) {
            final Switch masterSwitch = (Switch) menu.findItem(R.id.master_switch).getActionView();
            final boolean masterSwitchOn = mPrefs.getBoolean(PREF_KEY_MASTER_SWITCH, false);
            masterSwitch.setChecked(masterSwitchOn);
            masterSwitch.setText(masterSwitchOn ? R.string.action_on : R.string.action_off);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            switch(key) {
                case PREF_KEY_MASTER_SWITCH: {
                    getActivity().invalidateOptionsMenu();
                    final boolean masterSwitchOn = prefs.getBoolean(key, false);
                    setPreferencesEnabledState(masterSwitchOn);
                    // No break here.
                }
                case PREF_KEY_FORCE_IMMERSIVE:
                case PREF_KEY_FORCE_ROTATION:
                case PREF_KEY_BLUE_FILTER:
                case PREF_KEY_KEEP_SCREEN_ON:
                    if(prefs.getBoolean(PREF_KEY_MASTER_SWITCH, false) &&
                            (prefs.getBoolean(PREF_KEY_FORCE_IMMERSIVE, false) ||
                                prefs.getBoolean(PREF_KEY_FORCE_ROTATION, false) ||
                                prefs.getBoolean(PREF_KEY_BLUE_FILTER, false) ||
                                prefs.getBoolean(PREF_KEY_KEEP_SCREEN_ON, false))) {
                        startOverlayService(getActivity());
                    }
                    break;
            }
        }

        private void setPreferencesEnabledState(boolean masterSwitchOn) {
            findPreference(PREF_KEY_FORCE_IMMERSIVE).setEnabled(masterSwitchOn);
            findPreference(PREF_KEY_FORCE_ROTATION).setEnabled(masterSwitchOn);
            findPreference(PREF_KEY_KEEP_SCREEN_ON).setEnabled(masterSwitchOn);
            findPreference(PREF_KEY_BLUE_FILTER).setEnabled(masterSwitchOn);
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
                if(mService != null) {
                    mService.setBlueFilterLevel(mPrefs.getInt(PREF_KEY_BLUE_FILTER_LEVEL, 50));
                }
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
            if(!mPrefs.contains(PREF_KEY_BLUE_FILTER_LEVEL)) {
                mPrefs.edit().putInt(PREF_KEY_BLUE_FILTER_LEVEL, 50).commit();
            }
            final SeekBarPreference blueFilterLevel = (SeekBarPreference)findPreference(PREF_KEY_BLUE_FILTER_LEVEL);
            blueFilterLevel.setOnSeekbarChangedListener(mBlueFilterLevelListener);
            blueFilterLevel.setOnShowDialogListener(mBlueFilterLevelListener);
            blueFilterLevel.setOnDialogClosedListener(mBlueFilterLevelListener);
            //blueFilterLevel.setEnabled(prefs.getBoolean(PREF_KEY_BLUE_FILTER, false));

            startOverlayServiceIfNeeded(getActivity());
        }

        public static void startOverlayServiceIfNeeded(Context context) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if(prefs.getBoolean(PREF_KEY_MASTER_SWITCH, false) &&
                    (prefs.getBoolean(PREF_KEY_FORCE_IMMERSIVE, false) ||
                    prefs.getBoolean(PREF_KEY_FORCE_ROTATION, false) ||
                    prefs.getBoolean(PREF_KEY_BLUE_FILTER, false) ||
                    prefs.getBoolean(PREF_KEY_KEEP_SCREEN_ON, false))) {
                startOverlayService(context);
            }
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

