package us.daniel.walkthewalk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.graphics.Color;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.gelitenight.waveview.library.WaveView;

public class MainActivity extends AppCompatActivity {

    /** Allow log debugging */
    private static final String DEBUG_TAG = "MAIN_ACTIVITY";

    /** Default number of steps to the target goal*/
    private static final int GOAL = 100;

    /** Color for the progress wave view */
    private static final int BORDER_COLOR = Color.parseColor("#44f16d7a");

    /** Border for the progress wave view */
    private static final int BORDER_WIDTH = 10;

    /** Reference to spawned service */
    private Intent pedometerService;

    /** Reference to wave view displayed */
    private WaveView waveView;

    /** The object will load proper animations as user progresses in step count */
    private WaveHelper waveHelper;

    private int progression;

    private int lastMilestone;

    /** Receives broadcasts from pedometer service */
    private BroadcastReceiver pedometerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int steps = intent.getIntExtra("step_count", 0);
            TextView progressText = findViewById(R.id.step_progress_text_view);

            /** Update text view */
            if(steps > GOAL) {
                progressText.setText("Goal Completed");
            }
            else {
                progressText.setText(steps + " / " + GOAL);
            }

            progression = (int) ((float) (steps) / GOAL * 100.0f);

            if(progression >= lastMilestone + 10) { /** New milestone reached */
                lastMilestone += 10;
                waveHelper.cancel(); /** Stop previous animations */
                waveHelper = new WaveHelper(waveView, (lastMilestone - 10) / 100.0f, lastMilestone / 100.0f);
                waveHelper.start(); /** Generate new animations and start*/
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progression = 0;
        lastMilestone = 0;

        pedometerService = new Intent(getApplicationContext(), PedometerService.class);
        startService(pedometerService);

        LocalBroadcastManager.getInstance(this).
                registerReceiver(pedometerReceiver, new IntentFilter("PEDOMETER_STEP_UPDATE"));

        waveView = findViewById(R.id.wave_view);
        waveView.setBorder(BORDER_WIDTH, BORDER_COLOR);
        waveView.setShapeType(WaveView.ShapeType.CIRCLE);
        waveView.setWaveColor(Color.parseColor("#28f16d7a"),
                Color.parseColor("#3cf16d7a"));

        waveHelper = new WaveHelper(waveView, lastMilestone, lastMilestone);
        waveHelper.start(); /** Startup animation for wave view at the current progress */
    }

    @Override
    protected void onDestroy() {
        stopService(pedometerService);
        Log.i(DEBUG_TAG, "Destroying Pedometer Service");
        super.onDestroy();
    }
}
