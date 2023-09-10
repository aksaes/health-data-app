package com.example.healthmonitor;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SlowTask extends AsyncTask<String, String, String> {
    private Context context;
    private HeartRateCallback callback;

    public SlowTask(Context context, HeartRateCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    protected String doInBackground(String... params) {


        Bitmap m_bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        List<Bitmap> frameList = new ArrayList<>();

        try {
            AssetFileDescriptor assetFileDescriptor = context.getAssets().openFd(params[0]);
            retriever.setDataSource(assetFileDescriptor.getFileDescriptor(),
                    assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT);
            int aduration = Integer.parseInt(duration);
            int i = 10;
            while (i < aduration) {
                Bitmap bitmap = retriever.getFrameAtIndex(i);
                frameList.add(bitmap);
                i += 5;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            long redBucket = 0;
            long pixelCount = 0;
            List<Long> a = new ArrayList<>();

            for (Bitmap i : frameList) {
                redBucket = 0;
                int bitmapWidth = i.getWidth();
                int bitmapHeight = i.getHeight();
                for (int y = 380; y < bitmapHeight; y++) {
                    for (int x = 170; x < bitmapWidth; x++) {
                        int c = i.getPixel(x, y);
                        pixelCount++;
                        redBucket += Color.red(c) + Color.blue(c) + Color.green(c);
                    }
                }
                a.add(redBucket);
            }

            List<Long> b = new ArrayList<>();
            for (int i = 0; i < a.size() - 5; i++) {
                long temp = (a.get(i) + a.get(i + 1) + a.get(i + 2) + a.get(i + 3) + a.get(i + 4)) / 4;
                b.add(temp);
            }
            long x = b.get(0);
            int count = 0;
            for (int i = 1; i < b.size() - 1; i++) {
                long p = b.get(i);
                if ((p - x) > 3500) {
                    count = count + 1;
                }
                x = b.get(i);
            }

            int rate = (int) ((count * 60.0f) / 45.0f);

            return String.valueOf(rate / 2);

        }
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (callback != null) {
            callback.onHeartRateCalculated(result);
        }
    }
}