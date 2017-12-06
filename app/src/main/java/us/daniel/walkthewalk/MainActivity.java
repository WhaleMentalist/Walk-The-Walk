package us.daniel.walkthewalk;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    private static final String DEBUG_TAG = "MAIN_ACTIVITY";

    private Intent pedometerService;

    private BroadcastReceiver pedometerReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            TextView text = findViewById(R.id.step_counter);
            text.setText(Integer.toString(intent.getIntExtra("steps", 0)));
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pedometerService = new Intent(getApplicationContext(), PedometerService.class);
        startService(pedometerService);

        LocalBroadcastManager.getInstance(this).
                registerReceiver(pedometerReceiver, new IntentFilter("Pedometer"));
    }

    @Override
    protected void onDestroy() {
        stopService(pedometerService);
        Log.i(DEBUG_TAG, "Destroying Pedometer Service");
        super.onDestroy();
    }
}
