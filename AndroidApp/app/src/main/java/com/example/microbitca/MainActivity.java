package com.example.microbitca;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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


   // public void dataReceived(float xG, float yG, float zG, float pitch, float roll)
   // {
        //Log.i("TAG", Thread.currentThread().getName());
       // graphView.updateGraph(new float[]{xG,yG,zG},xGcb.isChecked(),yGcb.isChecked(),zGcb.isChecked(),mGcb.isChecked());
       //// pitchTV.setText("θ:"+(int)pitch);
        //rollTV.setText("φ:"+(int)roll);
    //}

    /** Defines callbacks for service binding, passed to bindService(). */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder iBinder) {
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

    }
}