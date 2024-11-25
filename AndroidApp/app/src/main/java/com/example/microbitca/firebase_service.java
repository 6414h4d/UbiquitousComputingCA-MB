package com.example.microbitca;

import android.app.Service;
import android.content.Intent;
import android.health.connect.datatypes.units.Power;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class firebase_service extends Service {
    FirebaseDatabase db;
    DatabaseReference dbRef;

    public void onCreate(){
        Toast.makeText(this, "Firebase Service started", Toast.LENGTH_SHORT).show();
    }

    public void onStart(Intent intent, int startid){}
    public void onDestroy(){}

    public firebase_service() {
        PunchPowerModel punch = new PunchPowerModel("58", "12343");

        db = FirebaseDatabase.getInstance();
        dbRef = db.getReference("TopScores");

        FirebaseDatabase.getInstance();


        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                //Toast.makeText(getApplicationContext(),"somedata value is: " + value, Toast.LENGTH_LONG).show();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                //Toast.makeText(firebase_service.this, "Firebase conn Cancelled", Toast.LENGTH_SHORT).show();
            }
        });

//        dbRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                String value = dataSnapshot.getValue(String.class);
//                //Toast.makeText(firebase_service.this, "Value is"+value,Toast.LENGTH_LONG).show();
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//            }
//        });
    }
    

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}