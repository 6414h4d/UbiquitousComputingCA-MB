package com.example.microbitca;

public interface BLEListener {
    void dataReceived(float xG, float yG, float zG, float pitch, float roll) throws InterruptedException;
}
