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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

import static java.lang.Thread.sleep;

/**
 * Allows tracking of foot steps based on accelerometer data.
 * This will consume a lot of battery in its current form. I
 * need to implement a mechanism that allows it to conserve
 * power when stationary and to check if we are moving periodically.
 */
public class PedometerService extends Service {

    /** Strictly for debugging logs */
    private static final String DEBUG_TAG = "PEDOMETER_SERVICE";

    /** Will be used to subtract off magnitude calculated from sensor data */
    private static final float GRAVITY = 9.807f;

    /** The period between sensor readings in microseconds */
    private static final int SAMPLE_PERIOD = 20000;

    /** Amount of sensor data samples to read*/
    private static final int SAMPLE_SIZE = 50;

    /** Threshold for when walking is started */
    private static final float THRESHOLD = 6.1f;

    /** Handler thread for sensor reading*/
    private HandlerThread accelerometerReader;

    /** Handler responds to sensor events */
    private Handler accelerometerHandler;

    /** Runnable that checks for steps that occur */
    private AccelerometerReader stepDetectionTask;

    /** Number of steps detected by accelerometer */
    private int steps = 0;

    /** This will queue data read from the sensor and also manage when it is read */
    private ArrayBlockingQueue<Float> acceleration;

    /** This will allow producer thread to notify the consumer thread for acceleration data */
    private CountDownLatch countDownLatch = new CountDownLatch(SAMPLE_SIZE);

    public PedometerService() {
        super();
        Log.i(DEBUG_TAG, "CONSTRUCTION");
    }

    @Override
    public void onCreate() {
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        acceleration = new ArrayBlockingQueue<>(2 * SAMPLE_SIZE);

        /** Create thread for sensor reading */
        accelerometerReader = new HandlerThread("ACCELEROMETER_W", Thread.MAX_PRIORITY);
        accelerometerReader.start();

        /** Handle sensor readings from other thread */
        accelerometerHandler = new Handler(accelerometerReader.getLooper());
        sensorManager.registerListener(new AccelerometerListener(), accelerometer, SAMPLE_PERIOD, accelerometerHandler);

        /** This will consume data that is produced by accelerometer*/
        stepDetectionTask = new AccelerometerReader();
        new Thread(stepDetectionTask, "STEP_DETECTION").start();
    }

    @Override
    public void onDestroy() {
        /** TODO: Unregister listener on sensor manager? */
        super.onDestroy();
        Log.i(DEBUG_TAG, "Application closed... Creating service in background");

        accelerometerReader.quit(); /** Stop reading data */
        stepDetectionTask.stop(); /** Stop detecting steps */

        Toast.makeText(this,
                "Running pedometer service in background", Toast.LENGTH_SHORT).show();

        Intent broadcast = new Intent("us.daniel.walkthewalk.PedometerRestart"); /** Allows service to persist when activity shutdown */
        sendBroadcast(broadcast); /** Will allow broadcaster to send out signal to restart pedometer service */
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        super.onStartCommand(intent, flags, startID);
        return START_STICKY; /** Sticky allows persistent service */
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Method allows pedometer service to send its step count to
     * the activities that are subscribed
     */
    private void sendMessageToActivity() {
        Intent intent = new Intent("PEDOMETER_STEP_UPDATE"); /** */
        intent.putExtra("step_count", steps);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent); /** Broadcast step count to interested components */
    }

    /**
     * Handles the accelerometer getting a new reading. This is considered the producer
     * in the producer-consumer problem
     */
    private class AccelerometerListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {

            float x, y, z, mag;
            x = sensorEvent.values[0];
            y = sensorEvent.values[1];
            z = sensorEvent.values[2];

            /** Want to subtract off the magnitude due to gravity*/
            mag = (float) Math.sqrt((double)(x * x + y * y + z * z)) - GRAVITY;

            try {
                acceleration.put(mag); /** Record into buffer for evaluation later */
                countDownLatch.countDown(); /** Bring down count down for other consumer thread*/
                Log.i(DEBUG_TAG, "Count down: " + countDownLatch.getCount());
            }
            catch(InterruptedException e) {
                Log.i(DEBUG_TAG, e.getMessage());
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

        /** Determines if thread should keep running. Important during teardown */
        private volatile boolean execute = true;

        /** A copy of the sensor readings */
        private List<Float> sensorDataCopy = new ArrayList<>();

        /** Track state of step detection algorithm */
        private boolean above = false;

        @Override
        public void run() {

            while(execute) { /** Allow thread to be stopped externally */

                try {
                    countDownLatch.await(); /** Wait until sample size is complete */
                    countDownLatch = new CountDownLatch(SAMPLE_SIZE); /** Reset count down*/
                }
                catch(InterruptedException e) {
                    Log.i(DEBUG_TAG, e.getMessage());
                }

                acceleration.drainTo(sensorDataCopy, SAMPLE_SIZE); /** Copy readings into data member */
                Log.i(DEBUG_TAG, "Copy Size: " + sensorDataCopy.size());
                Log.i(DEBUG_TAG, "Detecting steps");
                detectSteps(); /** This can be done as accelerometer is reading in data */
                Log.i(DEBUG_TAG, "Processing done");
                sensorDataCopy.clear();
            }
        }

        /**
         * Stop the execution of thread from the outside
         */
        public void stop() {
            execute = false;
        }

        /**
         * Algorithm checks for peaks and increments number steps based on
         * peaks found... NOTE: It will not count peaks that were always
         * above threshold, meaning the peak must go above and then below
         * the threshold to be counted as a step
         */
        private void detectSteps() {
            for(int i = 0; i < SAMPLE_SIZE; i++) {

                if(sensorDataCopy.get(i) > THRESHOLD) {
                    above = true;
                }
                else if(sensorDataCopy.get(i) < THRESHOLD) {
                    if(above) {
                        Log.i(DEBUG_TAG, "Detected a step");
                        steps += 1; /** Increment step count */
                        sendMessageToActivity(); /** Broadcast to update*/
                        above = false;
                    }
                }
            }

        }
    }
}

