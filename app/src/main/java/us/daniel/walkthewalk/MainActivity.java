package us.daniel.walkthewalk;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;

    private Sensor mSensor;

    private int numberSteps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        numberSteps = 0;

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if(event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) { /* Detected a step! */
            numberSteps++;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        /* TODO: Maybe change amount of measurements or switch to another sensor */
    }
}
