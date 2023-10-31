package com.example.healthmonitor.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.healthmonitor.BuildConfig;
import com.example.healthmonitor.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class TrafficDataActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traffic_data);

        final EditText startAddressEditText = findViewById(R.id.startAddressEditText);
        final EditText endAddressEditText = findViewById(R.id.endAddressEditText);
        Button getTrafficDataButton = findViewById(R.id.getTrafficDataButton);
        final TextView resultTextView = findViewById(R.id.resultTextView);

        getTrafficDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String startAddress = startAddressEditText.getText().toString();
                String endAddress = endAddressEditText.getText().toString();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String apiKey = BuildConfig.API_KEY;
                            String request = "https://maps.googleapis.com/maps/api/distancematrix/json" +
                                    "?origins=" + startAddress +
                                    "&destinations=" + endAddress +
                                    "&departure_time=now" + // For duration_in_traffic
                                    "&traffic_model=best_guess" +
                                    "&key=" + apiKey;

                            URL url = new URL(request);
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.setRequestMethod("GET");

                            int responseCode = connection.getResponseCode();

                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                                StringBuilder response = new StringBuilder();
                                String line;

                                while ((line = reader.readLine()) != null) {
                                    response.append(line);
                                }

                                reader.close();

                                final String jsonResponse = response.toString();
                                JSONObject jsonObject = new JSONObject(jsonResponse);
                                JSONArray rows = jsonObject.getJSONArray("rows");
                                JSONObject elements = rows.getJSONObject(0).getJSONArray("elements").getJSONObject(0);
                                int duration = elements.getJSONObject("duration").getInt("value");
                                int durationInTraffic = elements.getJSONObject("duration_in_traffic").getInt("value");
                                // Do DB creation
                                String workloadStatus;
                                if (durationInTraffic > duration) {
                                    workloadStatus = "Road Condition: Poor\nCognitive Workload: High (HCW)";
                                }
                                else {
                                    workloadStatus = "Road Condition: Normal\nCognitive Workload: Low (LCW)";
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        resultTextView.setText(workloadStatus);
                                    }
                                });
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        resultTextView.setText("HTTP Request failed : " + responseCode + "response code");
                                    }
                                });
                            }

                            connection.disconnect();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
    }
}