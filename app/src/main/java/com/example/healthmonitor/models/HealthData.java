package com.example.healthmonitor.models;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;

import com.example.healthmonitor.R;

import java.util.HashMap;
import java.util.Map;

public class HealthData implements Parcelable {
    private double heartRate;
    private double respiratoryRate;
    private Map<String, Float> symptomRatings; // HashMap to store symptom ratings

    // Constructors
    // Constructor with heart rate and respiratory rate
    public HealthData(double heartRate, double respiratoryRate, Context context) {
        this.heartRate = heartRate;
        this.respiratoryRate = respiratoryRate;
        symptomRatings = new HashMap<>();
        initializeSymptomRatingsToDefault(context);
    }

    // Initialize symptom ratings to default values based on symptom names in strings.xml
    private void initializeSymptomRatingsToDefault(Context context) {
        Resources res = context.getResources();
        String[] symptomNames = res.getStringArray(R.array.symptoms_array);

        for (String symptom : symptomNames) {
            symptomRatings.put(symptom, 0.0f); // Default rating is 0.0
        }
    }

    // Getter and Setter for Heart Rate
    public double getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(double heartRate) {
        this.heartRate = heartRate;
    }

    // Getter and Setter for Respiratory Rate
    public double getRespiratoryRate() {
        return respiratoryRate;
    }

    public void setRespiratoryRate(double respiratoryRate) {
        this.respiratoryRate = respiratoryRate;
    }

    // Getter and Setter for Symptom Ratings
    public void setSymptomRating(String symptom, float rating) {
        symptomRatings.put(symptom, rating);
    }

    public float getSymptomRating(String symptom) {
        Float rating = symptomRatings.get(symptom);
        return rating;
    }

    public Map<String, Float> getSymptomRatings() {
        return symptomRatings;
    }

    // Parcelable implementation
    public static final Parcelable.Creator<HealthData> CREATOR = new Parcelable.Creator<HealthData>() {
        public HealthData createFromParcel(Parcel in) {
            return new HealthData(in);
        }

        public HealthData[] newArray(int size) {
            return new HealthData[size];
        }
    };

    private HealthData(Parcel in) {
        heartRate = in.readDouble();
        respiratoryRate = in.readDouble();

        int size = in.readInt();
        symptomRatings = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String symptom = in.readString();
            float rating = in.readFloat();
            symptomRatings.put(symptom, rating);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(heartRate);
        dest.writeDouble(respiratoryRate);

        // Write the size of the symptomRatings map
        dest.writeInt(symptomRatings.size());

        // Write each symptom and its rating to the Parcel
        for (Map.Entry<String, Float> entry : symptomRatings.entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeFloat(entry.getValue());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
