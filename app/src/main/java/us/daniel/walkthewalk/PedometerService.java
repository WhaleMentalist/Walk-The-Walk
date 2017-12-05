package us.daniel.walkthewalk;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import android.util.Log;

import android.support.annotation.Nullable;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Allows tracking of foot steps based on accelerometer data.
 * This will consume a lot of battery in its current form. I
 * need to implement a mechanism that allows it to conserve
 * power when stationary and to check if we are moving periodically.
 */
public class PedometerService extends Service {

    private static final String DEBUG_TAG = "PEDOMETER_SERVICE";

    private static final int SAMPLE_PERIOD = 20000;

    private static final int SAMPLE_SIZE = 50;

    private HandlerThread sensorThread;

    private HandlerThread readerThread;

    private Handler sensorHandler;

    private Handler readerHandler;

    private SensorManager sensorManager;

    private Sensor accelerometer;

    private int steps;

    private ArrayBlockingQueue<Double> acceleration;

    public PedometerService() {
        super(); /** Help debug the service running */
        Log.i(DEBUG_TAG, "CONSTRUCTION");
    }

    @Override
    public void onCreate() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        acceleration = new ArrayBlockingQueue<Double>(2 * SAMPLE_SIZE);

        sensorThread = new HandlerThread("ACCELEROMETER_THREAD", Thread.MAX_PRIORITY);
        sensorThread.start();

        sensorHandler = new Handler(sensorThread.getLooper());
        sensorManager.registerListener(new AccelerometerListener(), accelerometer, SAMPLE_PERIOD, sensorHandler);

        readerThread = new HandlerThread("READER_THREAD", Thread.MAX_PRIORITY);
        readerThread.start();

        readerHandler = new Handler(readerThread.getLooper());

        readerHandler.post(new Runnable() {

            @Override
            public void run() {

            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(DEBUG_TAG, "Application closed... Creating service in background");
        sensorThread.quitSafely(); /** Investigate quit safely */
        Intent broadcast = new Intent("us.daniel.walkthewalk.PedometerRestart");
        sendBroadcast(broadcast);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        super.onStartCommand(intent, flags, startID);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Handles the accelerometer getting a new reading. Since this is in another thread
     * this can be considered the producer in the consumer-producer problem
     */
    private class AccelerometerListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float x, y, z, mag;
            x = sensorEvent.values[0];
            y = sensorEvent.values[1];
            z = sensorEvent.values[2];

            mag = (float) Math.sqrt((double)(x * x + y * y + z * z)); /** Not a fan of conversions */

            try {
                acceleration.put(Double.valueOf(mag)); /** Record into buffer for evaluation later */
            }
            catch(InterruptedException e) {
                Log.i(DEBUG_TAG, e.getMessage());
            }

            Log.i(DEBUG_TAG, "Produce");
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            /** Empty for now */
        }
    }

    /**
     * Handles the reading the accelerometer data. Since this is in another thread
     * this can be considered the consumer in the consumer-producer problem
     */
    private class AccelerometerReader implements Runnable {

        @Override
        public void run() {

        }
    }
}
