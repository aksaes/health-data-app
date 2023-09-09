package com.example.healthmonitor.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.healthmonitor.R;
import com.example.healthmonitor.models.HealthData;

import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "HealthData.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_HEALTH_DATA = "health_data";
    // Column names
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_HEART_RATE = "heart_rate";
    private static final String COLUMN_RESPIRATORY_RATE = "respiratory_rate";

    private final Context context;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Dynamically create symptom columns
        Resources res = this.context.getResources();
        String[] symptomNames = res.getStringArray(R.array.symptoms_array);

         String DATABASE_CREATE =
                "CREATE TABLE " + TABLE_HEALTH_DATA + " (" +
                        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_HEART_RATE + " REAL NOT NULL, " +
                        COLUMN_RESPIRATORY_RATE + " REAL NOT NULL";

//        Loop through symptomNames and add columns for each symptom
        for (String symptom : symptomNames) {
            String cleanedSymptomName = symptom.toLowerCase().replace(' ', '_');
            DATABASE_CREATE += ", " + cleanedSymptomName + " REAL";
        }

        DATABASE_CREATE += ");";

        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database upgrades here
    }

    // Method to insert HealthData into the database
    public long insertHealthData(HealthData healthData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_HEART_RATE, healthData.getHeartRate());
        values.put(COLUMN_RESPIRATORY_RATE, healthData.getRespiratoryRate());

        // Assuming you have a HealthData object with symptom ratings
        for (Map.Entry<String, Float> entry : healthData.getSymptomRatings().entrySet()) {
            String symptomName = entry.getKey();
            String columnName = symptomName.toLowerCase().replace(' ', '_');
            float rating = entry.getValue();
            values.put(columnName, rating);
        }

        long newRowId = db.insert(TABLE_HEALTH_DATA, null, values);
        db.close();

        return newRowId;
    }
}