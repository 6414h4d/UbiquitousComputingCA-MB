package com.example.microbitca;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
/*
Add Firebase connectivity
When the app is started, attempt to connect to the database.
If the connection fails, create a toast to alert the user

 */
public class firebase_service extends Service {
    public firebase_service() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}