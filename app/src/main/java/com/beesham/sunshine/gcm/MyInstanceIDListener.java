package com.beesham.sunshine.gcm;

import android.content.Intent;

import com.beesham.sunshine.RegistrationIntentService;
import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Created by Beesham on 10/25/2016.
 */
public class MyInstanceIDListener extends InstanceIDListenerService {

    @Override
    public void onTokenRefresh() {
        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);
    }
}
