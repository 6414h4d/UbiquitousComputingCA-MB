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
    //arrays for low pass filtering
    float[] accel_input;
    float[] accel_output;

    //microbit accelerometer BLE service UUIDs
    final UUID ACC_SERVICE_SERVICE_UUID = UUID.fromString("E95D0753-251D-470A-A062-FA1922DFA9A8");
    final UUID ACC_DATA_CHARACTERISTIC_UUID = UUID.fromString("E95DCA4B-251D-470A-A062-FA1922DFA9A8");
    final UUID ACC_PERIOD_CHARACTERISTIC_UUID = UUID.fromString("E95DFB24-251D-470A-A062-FA1922DFA9A8");
    final String TAG = "MicroBitConnectService";
    final String uBit_name = "BBC micro:bit";

    //list of listeners for data received events
    private List<BLEListener> listeners = new ArrayList<BLEListener>();

    public BLEService() {
        bleBinder = new BLEBinder();
        accel_input = new float[3];
        accel_output = new float[3];
    }

    public void addBLEListener(BLEListener listener) {
        listeners.add(listener);
    }

    /**
     * Class used for the client Binder. The Binder object is responsible for returning an instance
     * of "BLEService" to the client.
     */
    public class BLEBinder extends Binder {
        BLEService getService() {
            // Return this instance of MyService so clients can call public methods
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

    // BLUETOOTH CONNECTION
    private void connectDevice(BluetoothDevice device) {
        if (device == null) {
            Log.i(TAG, "Device is null");
            return;
        }
        GattClientCallback gattClientCallback = new GattClientCallback();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        gattClient = device.connectGatt(this, false, gattClientCallback);
    }

    // BLE Scan Callbacks
    private class BluetoothScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i(TAG, "onScanResult" + result.getDevice().getName());
            if (result.getDevice().getName() != null) {
                if (result.getDevice().getName().equals(uBit_name)) {
                    // When find your device, connect.
                    connectDevice(result.getDevice());
                    bluetoothLeScanner.stopScan(bluetoothScanCallback); // stop scan
                    Toast.makeText(BLEService.this, "Connected to Microbit", Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.i(TAG, "onBatchScanResults");
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.i(TAG, "ErrorCode: " + errorCode);
        }
    }

    // Bluetooth GATT Client Callback
    private class GattClientCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.i(TAG, "onConnectionStateChange");
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "GATT operation unsuccessful (status): " + status);
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "onConnectionStateChange CONNECTED");
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "onConnectionStateChange DISCONNECTED");
                reconnectDevice(gatt.getDevice());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.i(TAG, "onServicesDiscovered");
            if (status != BluetoothGatt.GATT_SUCCESS) return;
            gattClient = gatt;
            BluetoothGattService service = gatt.getService(ACC_SERVICE_SERVICE_UUID);
            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            // List and display in log the characteristics of this service
            for (BluetoothGattCharacteristic characteristic : characteristics) {
                Log.i(TAG, characteristic.getUuid().toString());
            }

            // Reference your UUIDs
            BluetoothGattCharacteristic ACC_DATA_characteristicID = gatt.getService(ACC_SERVICE_SERVICE_UUID).getCharacteristic(ACC_DATA_CHARACTERISTIC_UUID);
            gatt.setCharacteristicNotification(ACC_DATA_characteristicID, true);

            // Activate any descriptors for the characteristics
            List<BluetoothGattDescriptor> ACC_DATA_descriptors = ACC_DATA_characteristicID.getDescriptors();
            for (BluetoothGattDescriptor descriptor : ACC_DATA_descriptors) {
                Log.i(TAG, descriptor.getUuid().toString());
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }
        }

        // This is the callback that receives the accelerometer data
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

            // Calculate the refresh rate (Hz)
            if (numMeasurements == 0) {
                t0 = System.currentTimeMillis();
                numMeasurements++;
            } else if (numMeasurements == 100) {
                t = System.currentTimeMillis();
                float hz = 100.0f / (t - t0) * 100;
                Log.i(TAG, "HZ: " + hz);
                numMeasurements = 0;
            } else {
                numMeasurements++;
            }

            accel_input[0] = xG;
            accel_input[1] = yG;
            accel_input[2] = zG;


            private float[] lowPass(float[] input, float[] output) {
                final float ALPHA = 0.1f; // Filter coefficient (adjust as needed)

                if (output == null) {
                    // If no output exists, initialize it
                    output = new float[input.length];
                    System.arraycopy(input, 0, output, 0, input.length);
                }

                for (int i = 0; i < input.length; i++) {
                    output[i] = output[i] + ALPHA * (input[i] - output[i]);
                }

                return output;
            }

            double pitch = Math.atan((accel_output[0] / 1024.0f) / Math.sqrt(Math.pow((accel_output[1] / 1024.0f), 2) + Math.pow((accel_output[2] / 1024.0f), 2)));
            double roll = Math.atan((accel_output[1] / 1024.0f) / Math.sqrt(Math.pow((accel_output[0] / 1024.0f), 2) + Math.pow((accel_output[2] / 1024.0f), 2)));

            // Convert radians into degrees
            pitch = pitch * (180.0 / Math.PI);
            roll = -1 * roll * (180.0 / Math.PI);

            // Notify listeners on the main thread
            for (BLEListener listener : listeners) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    listener.dataReceived(accel_output[0], accel_output[1], accel_output[2], (float) pitch, (float) roll);
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
