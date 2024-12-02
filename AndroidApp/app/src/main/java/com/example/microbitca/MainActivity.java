package com.example.microbitca;

import static kotlinx.coroutines.DelayKt.delay;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements BLEListener {

    private static final String TAG = "MainActivity";

    private TextView scoreView;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> scoresList = new ArrayList<>();

    private DatabaseReference databaseRef;
    private BLEService service;
    private boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firebase setup
        databaseRef = FirebaseDatabase.getInstance().getReference("scores");

        // UI setup
        scoreView = findViewById(R.id.textView2);
        listView = findViewById(R.id.listView);

        // ListView adapter
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, scoresList);
        listView.setAdapter(adapter);

        // Load scores from Firebase
        loadScoresFromFirebase();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, BLEService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            BLEService.BLEBinder serviceBinder = (BLEService.BLEBinder) binder;
            service = serviceBinder.getService();
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
        if (xG > 900) { // Threshold for punch detection
            String score = String.valueOf((int) xG);
            saveScoreToFirebase(score);

            runOnUiThread(() -> {
                scoreView.setText(score);
                scoresList.add(score);
                adapter.notifyDataSetChanged();
            });

            Log.i(TAG, "Punch detected: " + score);
        }
    }

    private void saveScoreToFirebase(String score) {
        String key = databaseRef.push().getKey();
        if (key != null) {
            databaseRef.child(key).setValue(score)
                    .addOnSuccessListener(aVoid -> Log.i(TAG, "Score saved: " + score))
                    .addOnFailureListener(e -> Log.e(TAG, "Error saving score", e));
        } else {
            Log.e(TAG, "Firebase key is null");
        }
    }

    private void loadScoresFromFirebase() {
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                scoresList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String score = data.getValue(String.class);
                    if (score != null) {
                        scoresList.add(score);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading scores", error.toException());
            }
        });
    }

    public void startTest(View view) {
        if (mBound && service != null) {
            service.startScan(); // Ensure BLE scanning starts
            Toast.makeText(this, "Test Started! Punch now!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Service not bound or unavailable", Toast.LENGTH_SHORT).show();
        }
    }
}