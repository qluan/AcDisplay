/*
 * Copyright (C) 2014 AChep@xda <artemchep@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package com.achep.acdisplay.activities;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Build;
import com.achep.acdisplay.R;


/**
 * Activity that contains some methods to emulate system keyguard.
 */
public abstract class KeyguardActivity extends Activity {

    private static final String TAG = "KeyguardActivity";

    public static final String EXTRA_TURN_SCREEN_ON = "turn_screen_on";
    public static final String EXTRA_FINISH_ON_SCREEN_OFF = "finish_on_screen_off";

    private static final int UNLOCKING_MAX_TIME = 200;

    private BroadcastReceiver mScreenOffReceiver;

    private long mPendingFinishStartTime;
    private int mPendingFinishMax;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!hasFocus) {
            long now = SystemClock.elapsedRealtime();
            long elapsedTime = now - mPendingFinishStartTime;
            if (elapsedTime < mPendingFinishMax) {
                finish();
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mPendingFinishStartTime -= 500;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(0
                // Show activity above the system keyguard.
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                // Allow ignoring random presses.
                | WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);

        if (getIntent() != null) {
            Intent intent = getIntent();

            // Registers a receiver to finish activity when screen goes off. 
            if (intent.getBooleanExtra(EXTRA_FINISH_ON_SCREEN_OFF, false))
                registerScreenOffReceiver();

            // Turns screen on.
            if (intent.getBooleanExtra(EXTRA_TURN_SCREEN_ON, false))
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        mPendingFinishMax = getResources().getInteger(R.integer.config_maxPendingFinishDelayMillis);
    }

    @Override
    protected void onDestroy() {
        unregisterScreenOffReceiver();
        super.onDestroy();
    }

    /**
     * Registers a receiver to finish activity when screen goes off. 
     * You will need to {@link #unregisterScreenOffReceiver() unregister} it
     * later.
     *
     * @see #unregisterScreenOffReceiver()
     */
    private void registerScreenOffReceiver() {
        mScreenOffReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                // Avoid of blocking this event.
                // TODO: Double-check that #finish() is always run.
                new Handler().post(new Runnable() {

                    @Override
                    public void run() {
                        KeyguardActivity.this.finish();
                    }
                });
            }
        };

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1); // max allowed priority
        registerReceiver(mScreenOffReceiver, intentFilter);
    }

    /**
     * Unregisters screen off receiver if it was registered previuosly.
     *
     * @see #registerScreenOffReceiver()
     */
    private void unregisterScreenOffReceiver() {
        if (mScreenOffReceiver != null) {
            unregisterReceiver(mScreenOffReceiver);
            mScreenOffReceiver = null;
        }        
    }

    @Override
    protected void onResume() {
        super.onResume();
        overrideHomePress(true);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    }

    @Override
    protected void onPause() {
        super.onPause();
        overrideHomePress(false);
    }

    /**
     * Notifies Xposed module to start ignoring home button press.
     * Please, notice that it will ignore home button click everywhere
     * until you call {@code overrideHomePress(false)}
     *
     * @param override {@code true} to start ignoring, {@code false} to stop.
     * @see com.achep.acdisplay.xposed.OverrideHomeButton
     * @see #sendBroadcast(android.content.Intent)
     */
    private void overrideHomePress(boolean override) {
        Intent intent = new Intent(override
                ? App.ACTION_EAT_HOME_PRESS_START
                : App.ACTION_EAT_HOME_PRESS_STOP);
        sendBroadcast(intent);
    }

    /**
     * Locks the device (and turns screen off).
     *
     * @return {@code true} if successful, {@code false} otherwise.
     * @see DevicePolicyManager#lockNow()
     */
    public boolean lock() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        try {
            // TODO: Respect secure lockscreen timeout settings.
            dpm.lockNow();
            return true;
        } catch (SecurityException e) {
            return false; // User didn't make us an admin.
        }
    }

    /**
     * Unlocks keyguard and runs {@link Runnable runnable} when unlocked.
     */
    public void unlock(Runnable runnable) {
        unlock(runnable, true);
    }

    public void unlockWithPendingFinish(Runnable runnable) {
        mPendingFinishStartTime = SystemClock.elapsedRealtime();
        unlock(runnable, false);
    }

    /**
     * Unlocks keyguard and runs {@link Runnable runnable} when unlocked.
     *
     * @param finish {@code true} to finish activity, {@code false} to keep it
     * @see #unlock(Runnable)
     * @see #unlockWithPendingFinish(Runnable)
     * @see #isUnlocking()
     */
    public void unlock(final Runnable runnable, final boolean finish) {
        if (Build.DEBUG) Log.d(TAG, "Unlocking with params: finish=" + finish);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        // If keyguard is disabled no need to make
        // a delay between calling this method and
        // unlocking.
        // Otherwise we need this delay to get new
        // flags applied.
        final KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        final long now = SystemClock.elapsedRealtime();

        final Handler handler = new Handler();
        handler.post(new Runnable() {

            @Override
            public void run() {
                // Loop until flag gets applied.
                // TODO: Use somewhat trigger for detecting unlocking.
                int delta = (int) (SystemClock.elapsedRealtime() - now);
                if (km.isKeyguardLocked() && !km.isKeyguardSecure()
                        && delta < UNLOCKING_MAX_TIME) {
                    handler.postDelayed(this, 16);
                    return;
                }

                if (runnable != null) runnable.run();
                if (finish) {
                    finish();
                    overridePendingTransition(0, R.anim.activity_unlock);
                }
            }
        });
    }

    @Override
    public void onBackPressed() { /* override back button */ }

}
