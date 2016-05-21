package com.farproc.overlay;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class NapService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final int NAP_SERVICE_NOTIFICATION_ID = 20;
    private static final String ACTION_CANCEL_NAP = "com.farproc.overlay.action.CANCEL_NAP";

    private static final Handler sHandler = new Handler();

    private SharedPreferences mPrefs;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Intent settingsIntent = new Intent(this, SettingsActivity.class);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final Notification nt = new Notification.Builder(this)
                .setPriority(Notification.PRIORITY_MIN)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.napping))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(PendingIntent.getActivity(this, 0, settingsIntent, PendingIntent.FLAG_CANCEL_CURRENT))
                .addAction(0, getString(R.string.cancel), PendingIntent.getBroadcast(this, 0, new Intent(ACTION_CANCEL_NAP), 0))
                .build();
        this.startForeground(NAP_SERVICE_NOTIFICATION_ID, nt);
        registerReceiver(mCancelReceiver, new IntentFilter(ACTION_CANCEL_NAP));
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        sHandler.postDelayed(mEndNapRunnable, 10*1000);
    }

    private Runnable mEndNapRunnable = new Runnable() {
        @Override
        public void run() {
            mPrefs.edit()
                    .putBoolean(SettingsActivity.PREF_KEY_MASTER_SWITCH, true)
                    .commit();
            SettingsActivity.PrefsFragment.startOverlayServiceIfNeeded(NapService.this);
            stopSelf();
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mCancelReceiver);
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    private BroadcastReceiver mCancelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            sHandler.removeCallbacks(mEndNapRunnable);
            mEndNapRunnable.run();
        }
    };

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(SettingsActivity.PREF_KEY_MASTER_SWITCH.equals(key)) {
            sHandler.removeCallbacks(mEndNapRunnable);
            stopSelf();
        }
    }
}
