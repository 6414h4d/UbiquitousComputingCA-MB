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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BLEListener {
    private ListView listView;
    private TextView scoreView;

    BLEService service;
    boolean mBound = false;

    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_ADVERTISE,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
    };

    // Firebase database reference
    private DatabaseReference database;

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

        // Firebase setup
        database = FirebaseDatabase.getInstance().getReference("scores");

        // Populate the ListView with sample data
        updateHighScores();

        // Check permissions
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        // Start Firebase service (if needed)
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
    public void dataReceived(float xG, float yG, float zG, float pitch, float roll) {
        // Handle data received from the Microbit
        if (xG >= 800) {
            int score = (int) (xG * 10); // Example scoring logic
            runOnUiThread(() -> scoreView.setText(String.valueOf(score)));

            // Save to Firebase
            String scoreId = database.push().getKey();
            if (scoreId != null) {
                database.child(scoreId).setValue(score).addOnSuccessListener(aVoid -> {
                    Log.i("Firebase", "Score saved: " + score);
                    updateHighScores();
                }).addOnFailureListener(e -> Log.e("Firebase", "Failed to save score", e));
            }
        }
    }

    private void updateHighScores() {
        // Retrieve highest scores from Firebase and update the ListView
        database.orderByValue().limitToLast(10).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<String> highScores = new ArrayList<>();
                for (DataSnapshot scoreSnapshot : task.getResult().getChildren()) {
                    highScores.add(String.valueOf(scoreSnapshot.getValue(Integer.class)));
                }

                // Sort scores in descending order
                highScores.sort((a, b) -> Integer.compare(Integer.parseInt(b), Integer.parseInt(a)));

                // Update the ListView
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, highScores);
                listView.setAdapter(adapter);

                Log.i("Firebase", "High scores updated: " + highScores);
            } else {
                Log.e("Firebase", "Failed to retrieve scores", task.getException());
            }
        });
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