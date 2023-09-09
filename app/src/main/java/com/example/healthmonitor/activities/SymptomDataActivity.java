package com.example.healthmonitor.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import com.example.healthmonitor.R;
import com.example.healthmonitor.data.DatabaseHelper;
import com.example.healthmonitor.models.HealthData;

public class SymptomDataActivity extends AppCompatActivity {
    private HealthData healthData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom_data);

        // Retrieve the HealthData object from the Intent
        Intent intent = getIntent();
        healthData = intent.getParcelableExtra("health_data");

//        UI Elements
        Spinner spinnerSymptoms;
        RatingBar ratingBarSymptom;
        Button btnUploadData;

        spinnerSymptoms = findViewById(R.id.spn_symptoms);
        ratingBarSymptom = findViewById(R.id.rtb_symptom);
        btnUploadData = findViewById(R.id.btn_upload_data);

//        Initialize the Spinner with list of symptoms
//        String[] symptomsList = getSymptomsList();


        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                healthData.getSymptomRatings().keySet().toArray(new String[0]));
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSymptoms.setAdapter(spinnerAdapter);

        // Handle spinner item selection to update ratings
        spinnerSymptoms.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedSymptom =  (String) spinnerSymptoms.getSelectedItem();
                Float selectedRating = healthData.getSymptomRating(selectedSymptom);

//                // Update the rating for the selected symptom in the HashMap
//                symptomRatings.put(selectedSymptom, selectedRating);

                // Update the RatingBar to reflect the selected rating
                ratingBarSymptom.setRating(selectedRating);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle nothing selected if needed
            }
        });

        // Handle rating changes to update the HashMap
        ratingBarSymptom.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                String selectedSymptom = (String) spinnerSymptoms.getSelectedItem();

                // Update the rating for the selected symptom in the HashMap
                healthData.setSymptomRating(selectedSymptom, rating);
            }
        });

        btnUploadData.setOnClickListener(view -> {
            // Insert the HealthData object into the database
            DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
            long newRowId = dbHelper.insertHealthData(healthData);

            if (newRowId != -1) {
                // Insertion successful
                showAlertDialog("Data inserted successfully.");
            } else {
                // Insertion failed
                showAlertDialog("Data inserted unsuccessful. Something went wrong.");
            }
        });
    }

    private void showAlertDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Close the dialog when OK is clicked
                        dialog.dismiss();
                    }
                });

        // Create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

//    private String[] getSymptomsList() {
////        List<String> symptoms = new ArrayList<>();
//        String[] symptomsArray = getResources().getStringArray(R.array.symptoms_array);
////        Collections.addAll(symptoms, symptomsArray);
//        return symptomsArray;
//    }
}