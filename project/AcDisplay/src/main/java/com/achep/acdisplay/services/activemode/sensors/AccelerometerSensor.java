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
package com.achep.acdisplay.services.activemode.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.achep.acdisplay.Build;
import com.achep.acdisplay.services.activemode.ActiveModeSensor;

import java.lang.ref.WeakReference;

/**
 * Basing on results of accelerometer sensor it notifies when
 * {@link com.achep.acdisplay.acdisplay.AcDisplayActivity AcDisplay}
 * should be shown.
 *
 * @author Artem Chepurnoy
 */
public final class AccelerometerSensor extends ActiveModeSensor implements
        SensorEventListener {

    private static final String TAG = "AccelerometerSensor";

    private static WeakReference<AccelerometerSensor> sAccelerometerSensorWeak;

    private AccelerometerSensor() {
        super();
    }

    public static AccelerometerSensor getInstance() {
        AccelerometerSensor sensor = sAccelerometerSensorWeak != null
                ? sAccelerometerSensorWeak.get() : null;
        if (sensor == null) {
            sensor = new AccelerometerSensor();
            sAccelerometerSensorWeak = new WeakReference<>(sensor);
        }
        return sensor;
    }

    @Override
    public int getType() {
        return Sensor.TYPE_ACCELEROMETER;
    }

    @Override
    protected boolean isSupported(SensorManager sensorManager, Context context) {
        return false;
    }

    @Override
    public void onStart() {
        if (Build.DEBUG) Log.d(TAG, "Starting accelerometer sensor...");

        SensorManager sensorManager = getSensorManager();
        Sensor accelerationSensor = sensorManager.getDefaultSensor(getType());
        sensorManager.registerListener(this, accelerationSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onStop() {
        if (Build.DEBUG) Log.d(TAG, "Stopping accelerometer sensor...");

        SensorManager sensorManager = getSensorManager();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) { /* placeholder */ }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /* unused */ }

}
