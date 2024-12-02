package com.example.microbitca;

public interface BLEListener {
    float dataReceived(float xG, float yG, float zG, float pitch, float roll);
}
