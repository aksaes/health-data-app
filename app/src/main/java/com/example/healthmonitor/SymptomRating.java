package com.example.healthmonitor;

public class SymptomRating {
    private String symptom;
    private float rating;

    public SymptomRating(String symptom, float rating) {
        this.symptom = symptom;
        this.rating = rating;
    }

    public String getSymptom() {
        return symptom;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }
}
