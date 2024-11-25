package com.example.microbitca;

import static kotlinx.coroutines.DelayKt.delay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity implements BLEListener {
    public ListView list_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
        startService(new Intent(this, firebase_service.class));
//        TenPunchTest();
    }

    BLEService service;
    boolean mBound = false;

    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_ADVERTISE,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
    };

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService.
        Intent intent = new Intent(this, BLEService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

//    public void dataReceived(float xG, float yG, float zG, float pitch, float roll){
//        Log.i("TAG", Thread.currentThread().getName());
//        graphView.updateGraph(new float[]{xG,yG,zG},xGcb.isChecked(),yGcb.isChecked(),zGcb.isChecked(),mGcb.isChecked());
//        pitchTV.setText("θ:"+(int)pitch);
//        rollTV.setText("φ:"+(int)roll);
//    }

    /** Defines callbacks for service binding, passed to bindService(). */
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder iBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            BLEService.BLEBinder binder = (BLEService.BLEBinder) iBinder;
            service = binder.getService();
            service.startScan();
            service.addBLEListener((BLEListener) MainActivity.this);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };


    @Override
    public void dataReceived(float xG, float yG, float zG, float pitch, float roll) {
        /*
        * Take recieved data and pass it to the punch array.
        * This function should only be allowed to send data once every couple of seconds to stop the array from being filled too quickly
        * A threshold value for what data will be selected is set to further help with weeding out false positives.
        *
        * An array of data is saved and the highest value be taken from it and moved to the array as this will be the maximum power of the punch.
        *
        * Empty the array after the highest value has been selected and moved to the punch array.
        *
        */

        String[] punchData = new String[500];
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "10001";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // create the notification channel
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,"NOTIFICATION_CHANNEL_NAME",NotificationManager.IMPORTANCE_HIGH);
            // 'build' the notification
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            mNotificationManager.createNotificationChannel(notificationChannel);
        }

        while ( xG >= 800 ) {
//            xG = xG*10;
            Log.i("MovementDetected:", String.valueOf(xG));
            xG =0;

        }
        // Create the notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Punch Detected").setContentText("X Value"+xG)
                .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true);

        // Display the notification
        mNotificationManager.notify((int) System.currentTimeMillis(), mBuilder.build());
    }

    public void TenPunchTest(){
        /*
        * Ten punch test - Loop through this 10 times adding the punch value to an array
        * Push to array
        * When finished, push to database
        * */
        int count = 9;
        int counter =0;
        for(int i = 0; i <= count; i++){
            counter++;

            if (i == count){
                //Toast.makeText(this, "I = "+i+" Count = "+count+"Counter: "+counter, Toast.LENGTH_SHORT).show();
            }
        }
    }


}