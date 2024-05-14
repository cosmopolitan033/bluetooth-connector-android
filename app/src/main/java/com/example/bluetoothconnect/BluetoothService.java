package com.example.bluetoothconnect;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.util.SparseArray;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class BluetoothService {
    private static final String TAG = "BluetoothService";
    public static final String DEVICE_ADDRESS = "50:5E:5C:14:98:69"; // Changed to public for accessibility
    private SparseArray<BluetoothProfile> profileProxies = new SparseArray<>();
    private BroadcastReceiver connectionBroadcastReceiver;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket mBluetoothSocket;
    private Context context;

    public BluetoothService(Context context, BluetoothAdapter bluetoothAdapter) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
    }

    public BluetoothAdapter getBluetoothAdapter() { // Added getter method
        return bluetoothAdapter;
    }

    public void initializeBluetoothProfileService() {
        BluetoothProfile.ServiceListener serviceListener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                BluetoothProfile oldProxy = profileProxies.get(profile);
                if (oldProxy != null) {
                    BluetoothAdapter.getDefaultAdapter().closeProfileProxy(profile, oldProxy);
                }
                profileProxies.put(profile, proxy);
            }

            @Override
            public void onServiceDisconnected(int profile) {
                BluetoothProfile oldProxy = profileProxies.get(profile);
                if (oldProxy != null) {
                    BluetoothAdapter.getDefaultAdapter().closeProfileProxy(profile, oldProxy);
                    profileProxies.delete(profile);
                }
            }
        };

        bluetoothAdapter.getProfileProxy(context, serviceListener, BluetoothProfile.HEADSET);
        bluetoothAdapter.getProfileProxy(context, serviceListener, BluetoothProfile.A2DP);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);

        connectionBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleConnectionStateChange(intent);
            }
        };
        context.registerReceiver(connectionBroadcastReceiver, filter);
    }

    public void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.e(TAG, "Pairing failed", e);
        }
    }

    public void connect(BluetoothDevice device) {
        connectToBluetoothSocket(device);
        callBluetoothProfileMethod("connect", BluetoothProfile.HEADSET, device);
        callBluetoothProfileMethod("connect", BluetoothProfile.A2DP, device);
    }

    public void disconnect(BluetoothDevice device) {
        callBluetoothProfileMethod("disconnect", BluetoothProfile.HEADSET, device);
        callBluetoothProfileMethod("disconnect", BluetoothProfile.A2DP, device);
        closeBluetoothSocket();
    }

    @SuppressLint("MissingPermission")
    private void connectToBluetoothSocket(BluetoothDevice bluetoothDevice) {
        try {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            mBluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothAdapter.cancelDiscovery();
            if (!mBluetoothSocket.isConnected()) {
                mBluetoothSocket.connect();
            }
        } catch (IOException connectException) {
            Log.e(TAG, "Connection failed", connectException);
            fallbackBluetoothSocket(bluetoothDevice);
        }
    }

    @SuppressLint("MissingPermission")
    private void fallbackBluetoothSocket(BluetoothDevice bluetoothDevice) {
        try {
            Method m = bluetoothDevice.getClass().getMethod("createRfcommSocket", int.class);
            mBluetoothSocket = (BluetoothSocket) m.invoke(bluetoothDevice, 1);
            mBluetoothSocket.connect();
        } catch (Exception e) {
            Log.e(TAG, "Fallback connection failed", e);
            closeBluetoothSocket();
        }
    }

    private void closeBluetoothSocket() {
        try {
            if (mBluetoothSocket != null && mBluetoothSocket.isConnected()) {
                mBluetoothSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to close socket", e);
        }
    }

    private void callBluetoothProfileMethod(String methodName, int profile, BluetoothDevice device) {
        BluetoothProfile proxy = profileProxies.get(profile);
        if (proxy == null) return;

        try {
            Method method = proxy.getClass().getMethod(methodName, BluetoothDevice.class);
            method.invoke(proxy, device);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Log.e(TAG, "Method call failed", e);
        }
    }

    private void handleConnectionStateChange(Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        int prevState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1);
        int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
        int profile = intent.getAction().equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED) ? BluetoothProfile.HEADSET : BluetoothProfile.A2DP;
        // Notify listeners
    }

    public void cleanup() {
        context.unregisterReceiver(connectionBroadcastReceiver);
        for (int i = 0; i < profileProxies.size(); i++) {
            int profile = profileProxies.keyAt(i);
            BluetoothProfile proxy = profileProxies.get(profile);
            BluetoothAdapter.getDefaultAdapter().closeProfileProxy(profile, proxy);
        }
    }
}
