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
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the activity layout
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        listView = findViewById(R.id.listView);
        scoreView = findViewById(R.id.textView2);
        Button startButton = findViewById(R.id.button);

        // Set default score value
        scoreView.setText("0");

        // Populate the ListView with sample data
        String[] testData = {"Test Item 1", "Test Item 2", "Test Item 3"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, testData);
        listView.setAdapter(adapter);

        // Initialize the Start Button and set up the click listener
        startButton.setOnClickListener(v -> {
            // Send "BEGIN" command to the Microbit when the start button is pressed
            sendBeginCommand();
        });

        // Check permissions
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        // Start Firebase service
        startService(new Intent(this, firebase_service.class));
    }

    private void sendBeginCommand() {
        if (service != null) {
            // Send "BEGIN" command to Microbit via Bluetooth UART
            service.sendData("BEGIN");
        }
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
        // Handle received data here
        float speed = (float) Math.sqrt(xG * xG + yG * yG + zG * zG);  // Calculate the magnitude of acceleration

        // Update the UI with the calculated speed
        textView2 = findViewById(R.id.textView2);
        textView2.setText(String.format("%.2f", speed));

        // Upload speed data to Firebase
        uploadSpeedToFirebase(speed);

        // Optional: Add more processing or actions based on the speed value
        Log.i("SpeedData", "Speed: " + speed);
        sendNotification("Speed Detected", "Current Speed: " + speed);
    }

    private void uploadSpeedToFirebase(float speed) {
        // Get a reference to the Firebase Realtime Database
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("speeds");

        // Create a unique ID for the new speed entry
        String speedId = database.push().getKey();

        // If the ID is not null, save the speed value to the database
        if (speedId != null) {
            database.child(speedId).setValue(speed)
                    .addOnSuccessListener(aVoid -> Log.i("Firebase", "Speed uploaded successfully"))
                    .addOnFailureListener(e -> Log.e("Firebase", "Error uploading speed", e));
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

    // Other existing methods...
}