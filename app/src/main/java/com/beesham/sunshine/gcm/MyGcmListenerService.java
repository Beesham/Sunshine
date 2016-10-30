package com.beesham.sunshine.gcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.beesham.sunshine.MainActivity;
import com.beesham.sunshine.R;
import com.google.android.gms.gcm.GcmListenerService;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by Beesham on 10/25/2016.
 */

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";

    private static final String EXTRA_DATA = "data";
    private static final String EXTRA_WEATHER = "weather";
    private static final String EXTRA_LOCATION = "location";

    public static final int NOTIFICATION_ID = 1;

    @Override
    public void onMessageReceived(String from, Bundle bundle) {

        if(!bundle.isEmpty()){
            String senderId = getString(R.string.gcm_defaultSenderId);
            if(senderId.length() == 0){
                Toast.makeText(this, "SenderID string needs to be set", Toast.LENGTH_SHORT).show();
            }
            Log.i(TAG, "Received" + bundle.toString());

            if((senderId).equals(from)){
                try{
                    if(bundle.get(EXTRA_DATA) != null) {
                        JSONObject jsonObject = new JSONObject(bundle.getString(EXTRA_DATA));   //This returns null and causes crash. JSON on server does not contain key "data" as payload
                        String weather = jsonObject.getString(EXTRA_WEATHER);
                        String location = jsonObject.getString(EXTRA_LOCATION);
                        String alert = String.format(getString(R.string.gcm_weather_alert), weather, location);
                        sendNotification(alert);
                    }else{
                        for (String key : bundle.keySet()) {
                            Log.v(TAG, "Failed to get proper JSON: " + key + " => " + bundle.get(key) + ";");
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void sendNotification(String alert) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        Bitmap largeIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.art_storm);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.art_clear)
                .setLargeIcon(largeIcon)
                .setContentTitle("Weather Alert!")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(alert))
                .setContentText(alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setContentIntent(contentIntent);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
