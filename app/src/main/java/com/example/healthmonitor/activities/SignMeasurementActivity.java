package com.example.healthmonitor.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.widget.Button;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import com.example.healthmonitor.R;
import com.example.healthmonitor.SlowTask;
import com.example.healthmonitor.models.HealthData;
import com.example.healthmonitor.HeartRateCallback;

import java.util.ArrayList;

public class SignMeasurementActivity extends AppCompatActivity implements SensorEventListener,
        HeartRateCallback {
    private HealthData healthData;
    // Recording duration in milliseconds (45 seconds)
    private static final long RECORDING_DURATION_MS = 45000;
    private TextView resultTextView;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean isMonitoringRespiratoryRate = false;

    private ArrayList<Float> accelValuesX = new ArrayList<>();
    private ArrayList<Float> accelValuesY = new ArrayList<>();
    private ArrayList<Float> accelValuesZ = new ArrayList<>();

    private Handler resphandler = new Handler();
    private Runnable stopMonitoringRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_measurement);

        healthData = new HealthData(0, 0,
                getApplicationContext());

        Button measureHeartRateButton = findViewById(R.id.btn_heart_rate);
        Button measureRespiratoryRateButton = findViewById(R.id.btn_resp_rate);
        Button addSymptomsButton = findViewById(R.id.btn_add_symptoms);
        Button generateWorkloadButton = findViewById(R.id.btn_generate_workload);
        resultTextView = findViewById(R.id.txv_signs);

        addSymptomsButton.setOnClickListener(view -> {
            resultTextView.setText("");
            // Create an Intent to start the SymptomDataActivity
            Intent intent = new Intent(SignMeasurementActivity.this,
                    SymptomDataActivity.class);
            intent.putExtra("health_data", healthData);
            startActivity(intent); // Start the SymptomDataActivity
        });

        generateWorkloadButton.setOnClickListener(view -> {
            resultTextView.setText("");
            Intent intent = new Intent(SignMeasurementActivity.this,
                    TrafficDataActivity.class);
            startActivity(intent);
        });

        measureHeartRateButton.setOnClickListener((view -> {
            resultTextView.setText("Measuring heart rate");
            new SlowTask(this,this).execute("heartRateVideo.mp4");
            Toast.makeText(this,
                    "Might take few minutes. Please wait.",
                    Toast.LENGTH_SHORT).show();
        }));

        // Sensor code
        stopMonitoringRunnable = new Runnable() {
            @Override
            public void run() {
                stopMonitoringRespiratoryRate();
            }
        };
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        measureRespiratoryRateButton.setOnClickListener(view -> {
            resultTextView.setText("");
            Toast.makeText(this, "Please keep your phone on your chest for 45 sec.",
                    Toast.LENGTH_LONG).show();
            if (!isMonitoringRespiratoryRate) {
                resultTextView.setText("Started monitoring respiratory rate");
                startMonitoringRespiratoryRate();
            }
        });
    }

    @Override
    public void onHeartRateCalculated(String heartRate) {
        // This method will be called when the heart rate is calculated
        healthData.setHeartRate(Double.parseDouble(heartRate));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultTextView.setText("Heart rate is " + heartRate);
                healthData.setHeartRate(Double.parseDouble(heartRate));
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event){
        if(isMonitoringRespiratoryRate && event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            accelValuesX.add(event.values[0]);
            accelValuesY.add(event.values[0]);
            accelValuesZ.add(event.values[0]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // DO NOTHING
    }

    private void startMonitoringRespiratoryRate(){
        isMonitoringRespiratoryRate = true;
        sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);
        resphandler.postDelayed(stopMonitoringRunnable, RECORDING_DURATION_MS);
        new CountDownTimer(RECORDING_DURATION_MS, 1000) {
            public void onTick(long millisUntilFinished) {
                resultTextView.setText("Time remaining: " + (int) millisUntilFinished / 1000 +
                        " s");
            }

            @Override
            public void onFinish() {

            }
        }.start();
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(accelerometer != null){
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    private void stopMonitoringRespiratoryRate(){
        resultTextView.setText("Finished monitoring respiratory rate");
        isMonitoringRespiratoryRate = false;
        resphandler.removeCallbacks(stopMonitoringRunnable);
        calculateRespiratoryRate();
    }

    private void calculateRespiratoryRate(){
        resultTextView.setText("Processing respiratory rate");
        float previousValue = 10f;
        float currentValue = 0.0f;

        int k = 0;

        int dataSize = accelValuesX.size();

        for(int i=11; i <=450 && i < dataSize; i++){
            currentValue = (float) Math.sqrt(accelValuesX.get(i) * accelValuesX.get(i) +
                    accelValuesY.get(i) * accelValuesY.get(i) +
                    accelValuesZ.get(i) * accelValuesZ.get(i) );
            if(Math.abs(previousValue - currentValue) > 0.15){
                k++;
            }
            previousValue = currentValue;
        }
        double ret = (k/45.00);
        int respiratoryRate = (int) (ret * 30);
        resultTextView.setText("Respiratory rate is " + Integer.toString(respiratoryRate));
        healthData.setRespiratoryRate(respiratoryRate);
        accelValuesX.clear();
        accelValuesY.clear();
        accelValuesZ.clear();
    }
}

