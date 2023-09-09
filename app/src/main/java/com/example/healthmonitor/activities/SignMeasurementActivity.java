package com.example.healthmonitor.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import com.example.healthmonitor.R;
import com.example.healthmonitor.models.HealthData;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class SignMeasurementActivity extends AppCompatActivity implements SensorEventListener {
    private HealthData healthData;
    // Recording duration in milliseconds (45 seconds)
    private static final long RECORDING_DURATION_MS = 45000;
    private TextView resultTextView;

    private static final int REQUEST_CODE_PERMISSIONS = 123;
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };
    private boolean isRecordingHeartRate = false;
    private Handler recordingHandler = new Handler();
    private Runnable stopRecordingRunnable;
    private Camera camera;
    private Recording recording = null;
    private VideoCapture<Recorder> videoCapture = null;

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
        resultTextView = findViewById(R.id.txv_signs);

        addSymptomsButton.setOnClickListener(view -> {
            // Create an Intent to start the SymptomDataActivity
            Intent intent = new Intent(SignMeasurementActivity.this,
                    SymptomDataActivity.class);
            intent.putExtra("health_data", healthData);
            startActivity(intent); // Start the SymptomDataActivity
        });

        // Heart rate code
        stopRecordingRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    stopRecordingHeartRate();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        measureHeartRateButton.setOnClickListener((view -> {
            startRecordingHeartRate();
        }));

        startCamera();

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
            Toast.makeText(this, "Please keep your phone on your chest for 45 sec.",
                    Toast.LENGTH_LONG).show();
            if (!isMonitoringRespiratoryRate) {
                resultTextView.setText("Started monitoring respiratory rate");
                startMonitoringRespiratoryRate();
            }
        });
    }

    private void startRecordingHeartRate() {
        isRecordingHeartRate = true;
        Log.e("VIDEO", "Started Recording");
        recordingHandler.postDelayed(stopRecordingRunnable, RECORDING_DURATION_MS);
        new CountDownTimer(RECORDING_DURATION_MS, 1000) {
            public void onTick(long millisUntilFinished) {
                resultTextView.setText("Time remaining: " + (int) millisUntilFinished / 1000 +
                        " s");
            }

            @Override
            public void onFinish() {

            }
        }.start();
        recordVideo();
    }

    private void stopRecordingHeartRate() throws IOException {
        isRecordingHeartRate = false;
        Log.e("VIDEO", "Finished Recording");
        recording.stop();
        recording = null;
        toggleFlash(false);
        recordingHandler.removeCallbacks(stopRecordingRunnable);
        processHeartRate();
    }

    private void recordVideo() {
        if (videoCapture != null && isRecordingHeartRate){
            ContentValues contentValues = getContentVideoFile();

            MediaStoreOutputOptions mediaStoreOutput = new MediaStoreOutputOptions.Builder(
                    getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                    .setContentValues(contentValues)
                    .build();

            // Configure Recorder and Start recording to the mediaStoreOutput
            toggleFlash(true);
            recording = videoCapture.getOutput()
                    .prepareRecording(SignMeasurementActivity.this, mediaStoreOutput)
                    .start(ContextCompat.getMainExecutor(this), videoRecordEvent -> {
                        if(videoRecordEvent instanceof VideoRecordEvent.Start){
                            //TODO: If time
                        }
                        else if(videoRecordEvent instanceof VideoRecordEvent.Finalize){
                            if(!((VideoRecordEvent.Finalize) videoRecordEvent).hasError()){
                                String msg = "Video capture succeeded : " +
                                        ((VideoRecordEvent.Finalize) videoRecordEvent)
                                                .getOutputResults().getOutputUri();
                                Log.i("VIDEO", msg);
                                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                            }
                            else{
                                recording.close();
                                recording = null;
                                String msg = "Error: " + ((VideoRecordEvent.Finalize) videoRecordEvent)
                                        .getError();
                                Log.e("VIDEO", msg);
                            }
                        }
                    });
        }
    }

    private void processHeartRate() throws IOException {
        ContentValues contentValues = getContentVideoFile();
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        ArrayList<Bitmap> frameList = new ArrayList<>();

        retriever.setDataSource(getApplicationContext(), getContentResolver().insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues));

        int duration = Integer.parseInt(Objects.requireNonNull(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)));

        for(int i=10; i< duration; i+=5){
            Bitmap bitmap = retriever.getFrameAtTime(i * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            frameList.add(bitmap);
        }

        retriever.release();

        long redBucket = 0, pixelCount = 0;
        ArrayList<Long> a = new ArrayList<>();
        int width = 100, height = 100;
        int xStart = 550, yStart = 550;

        for (Bitmap frame: frameList) {
            redBucket = 0;

            for (int y = yStart; y < yStart + height; y++) {
                for (int x = xStart; x < xStart + width; x++) {
                    Color color = frame.getColor(x, y);
                    pixelCount++;
                    redBucket += color.red() + color.blue() + color.green();
                }
            }
            a.add(redBucket);
        }

        ArrayList<Long> b = new ArrayList<>();
        for(int i=0; i< a.size() - 5; i++){
            long temp = (a.get(i) + a.get(i+1) + a.get(i+2) + a.get(i+3)) / 4;
            b.add(temp);
        }

        long x = b.get(0);
        int count = 0;
        for(int i=1; i < b.size(); i++){
            long p = b.get(i);
            if((p-x) > 3500)
                count++;
            x = p;
        }

        double rate = ((double) count/45.0)*60;
        healthData.setHeartRate((rate/2));
    }

    @NonNull
    private static ContentValues getContentVideoFile() {
        String fileName = "heartRateVideo";
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/cameraX/HeartRate");
        return contentValues;
    }

    private void toggleFlash(boolean setValue) {
        if(camera.getCameraInfo().hasFlashUnit())
            camera.getCameraControl().enableTorch(setValue);
        else
            runOnUiThread(()-> Toast.makeText(this, "Flash unavailable",
                    Toast.LENGTH_SHORT).show());
    }

//    private boolean allPermissionsGranted() {
//        for (String permission : REQUIRED_PERMISSIONS) {
//            if (ContextCompat.checkSelfPermission(this, permission)
//                    != PackageManager.PERMISSION_GRANTED) {
//                return false;
//            }
//        }
//        return true;
//    }

    // Handle the result of permission requests
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//
//        if (requestCode == REQUEST_CODE_PERMISSIONS) {
//            boolean allPermissionsGranted = true;
//            for (int grantResult : grantResults) {
//                if (grantResult != PackageManager.PERMISSION_GRANTED) {
//                    allPermissionsGranted = false;
//                    break;
//                }
//            }
//
//            if (allPermissionsGranted) {
//                // Permissions granted, proceed with your logic
////                startCamera();
//            } else {
//                // Permissions denied, show a message or take appropriate action
//                Toast.makeText(this, "Permission request denied", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }

    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture
                = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try{
                ProcessCameraProvider provider = cameraProviderFuture.get();
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSIONS);
                }
                provider.unbindAll();
                Log.i("CAMERA", "Camera started");

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                Preview preview = new Preview.Builder()
                        .build();
                PreviewView previewView = findViewById(R.id.viewFinder);
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                Recorder.Builder builder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HD));
                videoCapture = VideoCapture.withOutput(builder.build());

                camera = provider.bindToLifecycle((LifecycleOwner) this, cameraSelector,
                        preview, videoCapture);

            }
            catch (ExecutionException e){
                e.printStackTrace();
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }

//            // Used to bind the lifecycle of cameras to the lifecycle owner
//            ProcessCameraProvider cameraProvider = null;
//            try {
//                cameraProvider = cameraProviderFuture.get();
//            } catch (ExecutionException | InterruptedException e) {
//                // No errors need to be handled for this Future.
//                // This should never be reached.
//            }
//
//            Preview preview = new Preview.Builder()
//                    .build();
//
//
//
//            Recorder recorder = new Recorder.Builder()
//                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
//                    .build();
//
//            videoCapture = VideoCapture.withOutput(recorder);
//
//            CameraSelector cameraSelector = new CameraSelector.Builder()
//                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
//                    .build();
//
//            PreviewView previewView = findViewById(R.id.viewFinder);
//            preview.setSurfaceProvider(previewView.getSurfaceProvider());
//
//            try {
//                // Unbind use cases before rebinding
//                cameraProvider.unbindAll();
//                Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector,
//                        preview, videoCapture);
//            } catch (Exception e) {
//                Log.e("TAG", "Use case binding failed");
//            }
//
        }, ContextCompat.getMainExecutor(this));
    }

    //    public void startRecordingHeartRate() {
//        String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS",
//                Locale.getDefault()).format(System.currentTimeMillis());
//        ContentValues contentValues = new ContentValues();
//        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
//        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
//        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video");
//
//        MediaStoreOutputOptions options = new MediaStoreOutputOptions.Builder(getContentResolver(),
//                MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
//                .setContentValues(contentValues).build();
//
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        recording = videoCapture.getOutput()
//                .prepareRecording(SignMeasurementActivity.this, options)
//                .withAudioEnabled()
//                .start(ContextCompat.getMainExecutor(SignMeasurementActivity.this),
//                        videoRecordEvent -> {
//                    if (videoRecordEvent instanceof VideoRecordEvent.Start) {
//                    } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
//                        if (!((VideoRecordEvent.Finalize) videoRecordEvent).hasError()) {
//                            String msg = "Video capture succeeded: " + ((VideoRecordEvent.Finalize) videoRecordEvent).getOutputResults().getOutputUri();
//                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
//                        } else {
//                            recording.close();
//                            recording = null;
//                            String msg = "Error: " + ((VideoRecordEvent.Finalize) videoRecordEvent).getError();
//                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
//                        }
//                    }
//            });
//
//    }
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