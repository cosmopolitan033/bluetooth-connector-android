package com.example.bluetoothconnect;

import android.bluetooth.BluetoothDevice;

public interface StateChangeListener {
    void onConnectionStatusChanged(int profile, int prevState, int newState, BluetoothDevice device);
}
