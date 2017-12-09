package us.daniel.walkthewalk;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    /** Allow log debugging */
    private static final String DEBUG_TAG = "MAIN_ACTIVITY";

    /** Reference to spawned service */
    private Intent pedometerService;

    /** Receives broadcasts from pedometer service */
    private BroadcastReceiver pedometerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int steps = intent.getIntExtra("step_count", 0);
            TextView text = findViewById(R.id.step_counter);
            text.setText(Integer.toString(steps)); /** Update text with received step-count */
            ProgressBar bar = findViewById(R.id.progress_bar);
            bar.setProgress(steps);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pedometerService = new Intent(getApplicationContext(), PedometerService.class);
        startService(pedometerService);

        LocalBroadcastManager.getInstance(this).
                registerReceiver(pedometerReceiver, new IntentFilter("PEDOMETER_STEP_UPDATE"));
    }

    @Override
    protected void onDestroy() {
        stopService(pedometerService);
        Log.i(DEBUG_TAG, "Destroying Pedometer Service");
        super.onDestroy();
    }
}
