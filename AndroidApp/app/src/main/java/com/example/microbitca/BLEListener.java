package com.example.microbitca;

import android.util.Log;

public interface BLEListener {
    void dataReceived(float xG, float yG, float zG, float pitch, float roll);
}
