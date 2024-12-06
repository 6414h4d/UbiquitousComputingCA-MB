package com.example.microbitca;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class firebase_service extends Service {
    static FirebaseDatabase db;
    static DatabaseReference dbRef;

    public void onCreate(){
        db = FirebaseDatabase.getInstance();
        Toast.makeText(this, "Firebase Service started", Toast.LENGTH_SHORT).show();
    }

    public void onStart(Intent intent, int startid){}
    public void onDestroy(){}

    public firebase_service() {}



    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}