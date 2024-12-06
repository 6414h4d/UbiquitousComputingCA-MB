package com.example.microbitca;


import static com.example.microbitca.LoginActivity.userNameGlobal;

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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements BLEListener {
    private ListView listView;
    private TextView scoreView;
    private TextView textView2;

    BLEService service;
    boolean mBound = false;

    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = { android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_ADVERTISE, android.Manifest.permission.ACCESS_FINE_LOCATION,};

    FirebaseDatabase db = FirebaseDatabase.getInstance();
    DatabaseReference dbRef = db.getReference();

    ArrayList<String> testData = new ArrayList<>();
    ArrayList<Object> highScoreArray = new ArrayList<Object>();
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

        // Initialize adapter with empty data
        ArrayAdapter<Object> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, highScoreArray);
        listView.setAdapter(adapter);

        // Check permissions
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        // Start Firebase service
        startService(new Intent(this, firebase_service.class));

        // Load scores from Firebase
        Log.i("Firebase-Load", "called loadScoresFromFirebase");
        loadScoresFromFirebase(adapter);
        Log.i("Login", userNameGlobal);
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


    public void dataReceived(float xG, float yG, float zG, float pitch, float roll) {

        ArrayList<Float> punchData = new ArrayList<Float>();
        ArrayList<Float> tempPunchData = new ArrayList<Float>();
        this.textView2 = (TextView) findViewById(R.id.textView2);

        float currentScore= 0;
        // Set the threshold value to greater than 1200
        while (xG >= 1200) {
            // if current output from microbit is greater than the previously set highscore continue
            if(xG > highScore ) {
                highScore = xG;
                // Log the current value for testing

                // add the current highest value to an array
                if(tempPunchData.size() < 1 ){
                    if(tempPunchData.get(-1) < highScore ){
                        tempPunchData.add(highScore);
                    }
                } else {
                    tempPunchData.add(highScore);
                    Log.i("testDataPunchData", ""+tempPunchData);
                }
                // once finished adding data to the tempPunchData listArray and take the final value
                // from the array and add it to the listView value array

                // Set the score textView
                textView2.setText(""+highScore);

                // reset the highscore to 0
            } else{
                // If the current value is not greater than the threshold value log a message to track
                Log.i("testData", "Current output is not greater than threshold value of 1200");
            }

            highScoreForArr = highScore;
            xG = 0;
            if (String.valueOf(highScoreForArr) !="0.0" ) {
                saveScoreToFirebase(String.valueOf(tempPunchData.get(-1)));

                sendNotification("Punch Detected", "X Value: " + tempPunchData.get(-1)); // Send a notification to the user containing their punch data

                punchData.add(highScoreForArr);
                Log.i("testData", "PunchData array"+String.valueOf(punchData));
                String[] punchArray = punchData.toArray(new String[0]);
                Log.i("testData", String.valueOf(punchArray));
            } else {
                Log.i("testData", "Not sending data to firebase");
            }
        }
    }

    private void saveScoreToFirebase(String highScore) {
        PunchPowerModel punch = new PunchPowerModel(userNameGlobal, highScore);

        // Create a unique key for the score entry
        String key = dbRef.child("scores").push().getKey();
        Log.i("Firebase-Save", "Generated key: " + key);

        if (key != null) {
            // Save the score under the "scores" path
            dbRef.child("scores").child(key).setValue(punch)
                    .addOnSuccessListener(aVoid -> Log.i("Firebase-Save", "Score saved successfully: " + highScore))
                    .addOnFailureListener(e -> Log.e("Firebase-Save", "Error saving score", e));
        } else {
            Log.e("Firebase-Save", "Firebase key is null. Unable to save score.");
        }
        ArrayAdapter<Object> refreshAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, highScoreArray);
        loadScoresFromFirebase(refreshAdapter);
    }

    // Use your desired path for scores
    private void loadScoresFromFirebase(ArrayAdapter<Object> adapter) {
        Log.i("Firebase-Load", "loadScoresFromFirebase called");
        DatabaseReference scoresRef = dbRef.child("scores");
        scoresRef.addValueEventListener(new ValueEventListener() {

            @Override
            // Clear existing data to avoid duplicates
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.i("Firebase", "Firebase data returned"+String.valueOf(dataSnapshot));
                highScoreArray.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Log.i("Firebase", "Firebase data returned"+String.valueOf(dataSnapshot));
                    Map<String, Object> score = (Map<String, Object>) snapshot.getValue();
                    if (score != null) {
                        highScoreArray.add(String.valueOf(score));
                    }
                }
                // Notify adapter to refresh the ListView
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Firebase", "Failed to read scores", databaseError.toException());
            }
        });
    }

    public void TenPunchTest(View view) {
        /*
         * Trigger when the Start Test button is pressed. When the 10 punches
         * have been recorded trigger the onPunchTestComplete method in
         * firebase_service.
         * */

        HashMap<String, String> TenPunchTest = new HashMap<String, String>();
        ArrayList<Integer> values = new ArrayList<Integer>();

//        Log.i("testDataPunchTest" highScore)
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
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(titleText)
                    .setContentText(contentText)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            mNotificationManager.notify((int) System.currentTimeMillis(), mBuilder.build());
        }
    }
}