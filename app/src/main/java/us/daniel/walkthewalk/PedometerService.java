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

import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Allows tracking of foot steps based on accelerometer data.
 * This will consume a lot of battery in its current form. I
 * need to implement a mechanism that allows it to conserve
 * power when stationary and to check if we are moving periodically.
 */
public class PedometerService extends Service {

    /** Strictly for debugging logs */
    private static final String DEBUG_TAG = "PEDOMETER_SERVICE";

    private static final float GRAVITY = 9.807f;

    private static final int SAMPLE_PERIOD = 20000;

    private static final int SAMPLE_SIZE = 50;

    private static final double THRESHOLD = 5.2;

    private HandlerThread sensorThread;

    private Handler sensorHandler;

    private SensorManager sensorManager;

    private Sensor accelerometer;

    /** Sample data read from the sensor*/
    private List<Double> data;

    /** Number of steps detected by accelerometer */
    private int steps = 0;

    /** This will queue data read from the sensor and also manage when it is read */
    private ArrayBlockingQueue<Double> acceleration;

    public PedometerService() {
        super(); /** Help debug the service running */
        Log.i(DEBUG_TAG, "CONSTRUCTION");
    }

    @Override
    public void onCreate() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        acceleration = new ArrayBlockingQueue<>(2 * SAMPLE_SIZE);
        data =  new ArrayList<>();

        sensorThread = new HandlerThread("ACCELEROMETER_THREAD", Thread.MAX_PRIORITY);
        sensorThread.start();

        /** Handle sensor readings from other thread */
        sensorHandler = new Handler(sensorThread.getLooper());
        sensorManager.registerListener(new AccelerometerListener(), accelerometer, SAMPLE_PERIOD, sensorHandler);

        /** Consumes the read data*/
        Thread threadOne = new Thread(new AccelerometerReader());
        threadOne.start();
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

    private void sendMessageToActivity() {
        Intent intent = new Intent("Pedometer");
        intent.putExtra("steps", steps);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent); /** Broadcast step count to interested components */
    }

    /**
     * Handles the accelerometer getting a new reading. Since this is in another thread
     * this can be considered the producer in the consumer-producer problem
     */
    private class AccelerometerListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {

            synchronized (acceleration) {

                if(acceleration.size() >= SAMPLE_SIZE) { /** Allow other thread access if sample is ready*/
                    Log.i(DEBUG_TAG, "Produced a sample. Wait...");

                    acceleration.notifyAll();

                    try {
                        acceleration.wait();
                    }
                    catch(InterruptedException e) {
                        Log.i(DEBUG_TAG, e.getMessage());
                    }
                }

                float x, y, z, mag;
                x = sensorEvent.values[0];
                y = sensorEvent.values[1];
                z = sensorEvent.values[2];

                mag = (float) Math.sqrt((double)(x * x + y * y + z * z)) - GRAVITY; /** Not a fan of conversions */

                try {
                    acceleration.put(Double.valueOf(mag)); /** Record into buffer for evaluation later */
                }
                catch(InterruptedException e) {
                    Log.i(DEBUG_TAG, e.getMessage());
                }

                Log.i(DEBUG_TAG, "Size: " + acceleration.size());
            }
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
            while(true) { /** Icky, icky... Need to figure out better way... */
                synchronized (acceleration) {
                    while (acceleration.size() < SAMPLE_SIZE) { /** Not enough data ready */
                        Log.i(DEBUG_TAG, "Waiting on sample...");

                        try {
                            acceleration.wait();
                        } catch (InterruptedException e) {
                            Log.i(DEBUG_TAG, e.getMessage());
                        }
                    }

                    Log.i(DEBUG_TAG, "Consuming sample...");
                    acceleration.drainTo(data, SAMPLE_SIZE); /** Copy readings into data */
                    detectSteps();
                    data.clear();
                    acceleration.notifyAll(); /** Finished reading data. Prepare producer. */
                }
            }
        }

        private void detectSteps() {

            boolean above = false;

            for(int i = 0; i < SAMPLE_SIZE; i++) {

                if(data.get(i) > THRESHOLD) {
                    above = true;
                }
                else if(data.get(i) < THRESHOLD) {
                    if(above) {
                        Log.i(DEBUG_TAG, "Detected a step");
                        steps += 1;
                        above = false; /** Reset */
                    }
                }
            }

            sendMessageToActivity(); /** Broadcast to update*/
        }
    }
}

