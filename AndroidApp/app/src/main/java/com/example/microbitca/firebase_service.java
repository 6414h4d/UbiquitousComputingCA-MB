package com.example.microbitca;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/*
Add Firebase connectivity
When the app is started, attempt to connect to the database.
If the connection fails, create a toast to alert the user

 */
public class firebase_service extends Service {
    FirebaseDatabase db;
    DatabaseReference dbRef;

    public void onCreate(){
        Toast.makeText(this, "Firebase Service started", Toast.LENGTH_SHORT).show();
    }

    public void onStart(Intent intent, int startid){}
    public void onDestroy(){}

    public firebase_service() {

        db = FirebaseDatabase.getInstance();
        dbRef = db.getReference("test");

        FirebaseDatabase.getInstance();

        dbRef.setValue("Hello, World!");


        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                Toast.makeText(getApplicationContext(),"somedata value is: " + value, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(firebase_service.this, "Firebase conn Cancelled", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}