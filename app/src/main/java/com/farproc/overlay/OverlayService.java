package com.farproc.overlay;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;


public class OverlayService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String ACTION_NAP_FOR_10_SEC = "com.farproc.overlay.action.NAP_FOR_10_SEC";

    private WindowManager mWindowManager;
    private View mOverlayView;
    private SharedPreferences mPrefs;

    public class LocalBinder extends Binder {
        public OverlayService getService() {
            return OverlayService.this;
        }
    };

    private LocalBinder mLocalBinder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mLocalBinder;
    }

    private static final int SERVICE_NOTIFICATION_ID = 10;

    @Override
    public void onCreate() {
        super.onCreate();
        mWindowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        // NOTE: Adding ACTION_SCREEN_ON and ACTION_USER_PRESENT in a single IntentFilter does not work.
        registerReceiver(mScreenOnReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
        registerReceiver(mUserPresentReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
        final IntentFilter turnOffIntent = new IntentFilter(SettingsActivity.ACTION_TURN_OFF);
        turnOffIntent.addAction(ACTION_NAP_FOR_10_SEC);
        registerReceiver(mTurnOffReceiver, turnOffIntent);
        setupOverlay();

        final Intent settingsIntent = new Intent(this, SettingsActivity.class);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final Notification nt = new Notification.Builder(this)
                .setPriority(Notification.PRIORITY_MIN)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.running))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(PendingIntent.getActivity(this, 0, settingsIntent, PendingIntent.FLAG_CANCEL_CURRENT))
                .addAction(0, getString(R.string.turn_off), PendingIntent.getBroadcast(this, 0, new Intent(SettingsActivity.ACTION_TURN_OFF), 0))
                .addAction(0, getString(R.string.nap_for_10_sec), PendingIntent.getBroadcast(this, 0, new Intent(ACTION_NAP_FOR_10_SEC), 0))
                .build();
        this.startForeground(SERVICE_NOTIFICATION_ID, nt);
    }

    public void setBlueFilterLevel(int level) {
        if(mPrefs.getBoolean(SettingsActivity.PREF_KEY_BLUE_FILTER, false) && mOverlayView != null) {
            mOverlayView.setBackgroundColor(getOverlayViewBackground(level));
        }
    }

    private int getOverlayViewBackground(int level) {
        return Color.argb(128*level/100, 0xFF, 0xFF, 0x0);
    }

    private void setupOverlay() {
        final boolean overlayViewAdded = mOverlayView != null;
        if(!overlayViewAdded) {
            mOverlayView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.overlay, null);
        }
        int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        int orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        int uiVisibility = 0;
        int backgroundColor = Color.TRANSPARENT;
        final boolean forceImmersive = mPrefs.getBoolean(SettingsActivity.PREF_KEY_FORCE_IMMERSIVE, false);
        final boolean forceRotation = mPrefs.getBoolean(SettingsActivity.PREF_KEY_FORCE_ROTATION, false);
        final boolean blueFilterOn = mPrefs.getBoolean(SettingsActivity.PREF_KEY_BLUE_FILTER, false);
        final boolean keepScreenOn = mPrefs.getBoolean(SettingsActivity.PREF_KEY_KEEP_SCREEN_ON, false);
        if(forceImmersive) {
            uiVisibility = //View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | // hide nav bar
                    View.SYSTEM_UI_FLAG_FULLSCREEN |  // hide status bar
                    View.SYSTEM_UI_FLAG_IMMERSIVE|
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        } else {
            // Make back key to work.
            // Immersive mode needs focusable.
            flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }

        if(forceRotation) {
            orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
        }

        if(blueFilterOn) {
            final int level = mPrefs.getInt(SettingsActivity.PREF_KEY_BLUE_FILTER_LEVEL, 50);
            backgroundColor = getOverlayViewBackground(level);
            if(!forceImmersive) {
                uiVisibility =
                        (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_FULLSCREEN);
            } else {
                // All flags needed is already set.
            }
        }

        if(keepScreenOn) {
            flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        }

        mOverlayView.setSystemUiVisibility(uiVisibility);
        mOverlayView.setBackgroundColor(backgroundColor);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_TOAST,
                flags,
                PixelFormat.TRANSLUCENT);
        params.screenOrientation = orientation;

        if(overlayViewAdded) {
            mWindowManager.updateViewLayout(mOverlayView, params);
        } else {
            mWindowManager.addView(mOverlayView, params);
        }
    }

    private void stopOverlay() {
        // Refresh
        mOverlayView.setSystemUiVisibility(0);
        mWindowManager.removeView(mOverlayView);
        mOverlayView = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        unregisterReceiver(mScreenOnReceiver);
        unregisterReceiver(mUserPresentReceiver);
        unregisterReceiver(mTurnOffReceiver);
        stopOverlay();
    }

    private BroadcastReceiver mUserPresentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setupOverlay();
        }
    };

    private BroadcastReceiver mScreenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setupOverlay();
        }
    };

    private BroadcastReceiver mTurnOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean(SettingsActivity.PREF_KEY_MASTER_SWITCH, false)
                    .commit();
            if(ACTION_NAP_FOR_10_SEC.equals(intent.getAction())) {
                context.startService(new Intent(context, NapService.class));
            }
        }
    };

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch(key) {
            case SettingsActivity.PREF_KEY_MASTER_SWITCH:
            case SettingsActivity.PREF_KEY_FORCE_IMMERSIVE:
            case SettingsActivity.PREF_KEY_FORCE_ROTATION:
            case SettingsActivity.PREF_KEY_BLUE_FILTER:
            case SettingsActivity.PREF_KEY_KEEP_SCREEN_ON:
                if(sharedPreferences.getBoolean(SettingsActivity.PREF_KEY_MASTER_SWITCH, false) == false ||
                        (sharedPreferences.getBoolean(SettingsActivity.PREF_KEY_FORCE_IMMERSIVE, false) == false &&
                        sharedPreferences.getBoolean(SettingsActivity.PREF_KEY_FORCE_ROTATION, false) == false &&
                        sharedPreferences.getBoolean(SettingsActivity.PREF_KEY_BLUE_FILTER, false) == false &&
                        sharedPreferences.getBoolean(SettingsActivity.PREF_KEY_KEEP_SCREEN_ON, false) == false)) {
                    stopSelf();
                } else {
                    setupOverlay();
                }
                break;
            case SettingsActivity.PREF_KEY_BLUE_FILTER_LEVEL:
                setupOverlay();
        }
    }
}
