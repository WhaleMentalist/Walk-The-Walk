package us.daniel.walkthewalk;

import android.content.BroadcastReceiver;

import android.content.Context;
import android.content.Intent;

public class PedometerRestartReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, PedometerService.class);
        context.startService(i); /** Start the pedometer */
    }
}
