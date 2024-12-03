package com.example.microbitca;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import android.os.Handler;
import android.os.Looper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import android.app.Service;
// Other imports remain unchanged

public class BLEService extends Service {

    // Bluetooth variables
    BluetoothAdapter bluetoothAdapter;
    BluetoothLeScanner bluetoothLeScanner;
    BluetoothManager bluetoothManager;
    ScanCallback bluetoothScanCallback;
    BluetoothGatt gattClient;
    BLEBinder bleBinder;
    long numMeasurements, t0, t;
    boolean filter = false;

    // Arrays for low-pass filtering
    float[] accel_input;
    float[] accel_output;

    // micro:bit accelerometer BLE service UUIDs
    final UUID ACC_SERVICE_SERVICE_UUID = UUID.fromString("E95D0753-251D-470A-A062-FA1922DFA9A8");
    final UUID ACC_DATA_CHARACTERISTIC_UUID = UUID.fromString("E95DCA4B-251D-470A-A062-FA1922DFA9A8");
    final UUID ACC_PERIOD_CHARACTERISTIC_UUID = UUID.fromString("E95DFB24-251D-470A-A062-FA1922DFA9A8");
    final String TAG = "MicroBitConnectService";
    final String uBit_name = "BBC micro:bit";

    // List of listeners for data received events
    private List<BLEListener> listeners = new ArrayList<>();

    public BLEService() {
        bleBinder = new BLEBinder();
        accel_input = new float[3];
        accel_output = new float[3];
    }

    public void addBLEListener(BLEListener listener) {
        listeners.add(listener);
    }

    public class BLEBinder extends Binder {
        BLEService getService() {
            return BLEService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return bleBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public void startScan() {
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothScanCallback = new BluetoothScanCallback();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        bluetoothLeScanner.startScan(bluetoothScanCallback);
        Log.i(TAG, "startScan()");
    }

    // Low-pass filter method (correctly placed in the class)
    private float[] lowPass(float[] input, float[] output) {
        final float ALPHA = 0.1f; // Adjust this value as needed

        if (input == null || input.length == 0) {
            throw new IllegalArgumentException("Input array must not be null or empty.");
        }

        if (output == null || output.length != input.length) {
            output = new float[input.length];
            System.arraycopy(input, 0, output, 0, input.length);
        }

        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }

        return output;
    }

    // BLE Scan Callbacks remain unchanged
    // GattClientCallback and other methods remain unchanged

    private class GattClientCallback extends BluetoothGattCallback {
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] bytes = characteristic.getValue();
            byte[] xG_bytes = new byte[2];
            byte[] yG_bytes = new byte[2];
            byte[] zG_bytes = new byte[2];
            System.arraycopy(bytes, 0, xG_bytes, 0, 2);
            System.arraycopy(bytes, 2, yG_bytes, 0, 2);
            System.arraycopy(bytes, 4, zG_bytes, 0, 2);
            float xG = byteArray2short(xG_bytes);
            float yG = byteArray2short(yG_bytes);
            float zG = byteArray2short(zG_bytes);

            // Low-pass filtering
            accel_input[0] = xG;
            accel_input[1] = yG;
            accel_input[2] = zG;

            accel_output = lowPass(accel_input, accel_output);

            double pitch = Math.atan((accel_output[0] / 1024.0f) /
                    Math.sqrt(Math.pow((accel_output[1] / 1024.0f), 2) + Math.pow((accel_output[2] / 1024.0f), 2)));
            double roll = Math.atan((accel_output[1] / 1024.0f) /
                    Math.sqrt(Math.pow((accel_output[0] / 1024.0f), 2) + Math.pow((accel_output[2] / 1024.0f), 2)));

            pitch = pitch * (180.0 / Math.PI);
            roll = -1 * roll * (180.0 / Math.PI);

            for (BLEListener listener : listeners) {
                double finalPitch = pitch;
                double finalRoll = roll;
                new Handler(Looper.getMainLooper()).post(() -> {
                    listener.dataReceived(accel_output[0], accel_output[1], accel_output[2], (float) finalPitch, (float) finalRoll);
                });
            }
        }
    }

    private void reconnectDevice(BluetoothDevice device) {
        if (device != null && bluetoothAdapter != null) {
            bluetoothLeScanner.startScan(bluetoothScanCallback);
            Log.i(TAG, "Reconnecting...");
        }
    }

    // Convert byte array to short
    private float byteArray2short(byte[] byteArray) {
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getShort();
    }

    public void stopScan() {
        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(bluetoothScanCallback);
            Log.i(TAG, "Scan stopped.");
        }
    }

    // Stop service
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(bluetoothScanCallback);
        }
        if (gattClient != null) {
            gattClient.disconnect();
            gattClient.close();
        }
        Log.i(TAG, "Service destroyed and resources cleaned up.");
    }
}
