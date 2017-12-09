package us.daniel.walkthewalk;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import android.support.annotation.Nullable;

import android.util.Log;


/**
 * Service will allows the tracking of foot steps by
 * spawning a accelerometer processing object that
 * manages the reading and processing of sensor data.
 * Ultimately, the service will have the ability to be ran
 * in the background.
 */
public class PedometerService extends Service {

    /** Strictly for debugging logs */
    private static final String DEBUG_TAG = "PEDOMETER_SERVICE";

    /** The data processor that reads and dtects footsteps from data */
    private AccelerometerProcessing dataProcessor;

    /**
     *
     */
    public PedometerService() {
        super();
        Log.d(DEBUG_TAG, "CONSTRUCTION");
        dataProcessor = AccelerometerProcessing.getInstance();
    }

    @Override
    public void onCreate() {
        Log.d(DEBUG_TAG, "Creating accelerometer data processor");
        dataProcessor.setup(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dataProcessor.destroy(); /** Cleanup threads and listeners */

        /** Allows service to persist when activity shutdown */
        Intent broadcast = new Intent("us.daniel.walkthewalk.PedometerRestart");
        sendBroadcast(broadcast);
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
}

