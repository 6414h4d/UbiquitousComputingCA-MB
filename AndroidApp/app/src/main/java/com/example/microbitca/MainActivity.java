package com.example.microbitca;

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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements BLEListener {
    private ListView listView;
    private TextView scoreView;
    private TextView textView2;

    BLEService service;
    boolean mBound = false;

    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_ADVERTISE,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
    };


    ArrayList<String> testData = new ArrayList<>();
    ArrayList<String> highScoreArray = new ArrayList<String>();
    float highScore=0;
    float highScoreForArr=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the activity layout
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        listView = findViewById(R.id.listView);
        scoreView = findViewById(R.id.textView2);

        // Set default score value
        scoreView.setText("0");

        // Populate the ListView with sample data
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,  testData);
        listView.setAdapter(adapter);

        // Check permissions
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        // Start Firebase service
        startService(new Intent(this, firebase_service.class));
    }

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
        // Bind to BLEService
        Intent intent = new Intent(this, BLEService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder iBinder) {
            // Service connection established
            BLEService.BLEBinder binder = (BLEService.BLEBinder) iBinder;
            service = binder.getService();
            service.startScan();
            service.addBLEListener(MainActivity.this);
            mBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    public float dataReceived(float xG, float yG, float zG, float pitch, float roll) {
        /*
        * Handle data received from the Microbit.  Set the value threshold for data
        * received from the Microbit. While the threshold has been exceeded, add
        * data to a 'punch power' array. Once the threshold is no longer being exceeded,
        * exit the loop and select the Highest value and send this to the TenPunchTest
        * method to be sent to the database once Ten punches have been recorded.
        * */

        ArrayList<String> punchData = new ArrayList<>();
        ArrayList<String> tempPunchData = new ArrayList<String>();

        float currentScore= 0;

        if (xG >= 1200) {
            if(xG > highScore ) {
                highScore = xG;

                Log.i("testDatahighScore",String.valueOf(highScore));
//                String xGVal = String.valueOf(highScore);

                this.textView2 = (TextView) findViewById(R.id.textView2);

                punchData.add(String.valueOf(xG));
                highScore = xG;

                highScoreForArr=highScore;
                if (highScore <= highScoreForArr){
                    textView2.setText(""+xG);
                    sendNotification("Punch Detected", "X Value: " + highScoreForArr);
                    highScoreForArr=highScore;
                    testData.add(String.valueOf(highScoreForArr));
                    Log.i("testData",String.valueOf(testData));

                    Object[] listviewData = new ArrayList[]{testData};

                    ArrayAdapter<Object> updateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,  listviewData);


                    listView.setAdapter(updateAdapter);
                }
                xG = 0;
            }
            return highScore;
        }

        highScore=0;
        return highScore;
    }

    public void PunchTest(View view) {
        /*
        * Trigger when the Start Test button is pressed. When the 10 punches
        * have been recorded trigger the onPunchTestComplete method in
        * firebase_service.
        * */


        HashMap<String, String> TenPunchTest = new HashMap<String, String>();
        ArrayList<Integer> values = new ArrayList<Integer>();

//        Log.i("testDataPunchTest", highScore)
        int count = 9;
        int counter = 0;

        for (int i = 0; i <= count; i++) {
            counter++;
            if (i == count) {
                Log.i("TenPunchTest", "I = " + i + " Count = " + count + " Counter: " + counter);
                this.textView2 = findViewById(R.id.textView2);
                textView2.setText("0");
                values.add(2);
                TenPunchTest.put("asdf", String.valueOf(values));
                Log.i("TenPunchTestData", String.valueOf(TenPunchTest));
            }
        }


    }

    public void sendNotification(String titleText, String contentText) {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "10001";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Punch Notifications", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 300, 200, 100});
            mNotificationManager.createNotificationChannel(notificationChannel);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle(titleText)
                    .setContentText(contentText)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            mNotificationManager.notify((int) System.currentTimeMillis(), mBuilder.build());
        }
    }
}