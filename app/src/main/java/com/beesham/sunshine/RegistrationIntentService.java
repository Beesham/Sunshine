package com.beesham.sunshine;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

/**
 * Created by Beesham on 10/25/2016.
 */
public class RegistrationIntentService extends IntentService{

    private static final String TAG = "RegIntentService";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Log.v(TAG, "in onHandleIntent");

        try{
            synchronized (TAG){
                InstanceID instanceID = InstanceID.getInstance(this);
                String senderId = getString(R.string.gcm_defaultSenderId);
                if(senderId.length() != 0) {
                    String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                            GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

                    Log.v(TAG, "gonna send token");
                    sendRegistrationToServer(token);
                }else{
                    Log.d(TAG, "senderId empty");
                }
                sharedPreferences.edit().putBoolean(MainActivity.SENT_TOKEN_TO_SERVER, true).apply();
            }
        }catch (Exception e){
            Log.e(TAG, "Failed to complete token refresh", e);
            sharedPreferences.edit().putBoolean(MainActivity.SENT_TOKEN_TO_SERVER, false).apply();
        }
    }

    private void sendRegistrationToServer(String token) {
        Log.i(TAG, "GCM Registration Token: " + token);
    }
}
