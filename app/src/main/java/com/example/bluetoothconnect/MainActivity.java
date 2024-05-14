// File: MainActivity.java
package com.example.bluetoothconnect;

import android.Manifest;
import android.app.Activity;
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
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_CONNECT = 2;
    private SparseArray<BluetoothProfile> profileProxies = new SparseArray<>();
    private BroadcastReceiver connectionBroadcastReceiver;
    private List<StateChangeListener> stateChangeListeners = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice device;
    private BluetoothSocket mBluetoothSocket;

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e("BLUETOOTH", "Device doesn't support Bluetooth");
            finish(); // Close the app or notify the user
        }

        checkBluetoothPermissions();

        Button connectButton = findViewById(R.id.buttonConnect);
        Button disconnectButton = findViewById(R.id.buttonDisconnect);

        // Get the BluetoothDevice instance
        device = bluetoothAdapter.getRemoteDevice("50:5E:5C:14:98:69");

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pairDevice(device);
                connect(device);
            }
        });

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disconnect(device);
            }
        });
    }

    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                }, REQUEST_BLUETOOTH_CONNECT);
            } else {
                initializeBluetooth();
            }
        } else {
            initializeBluetooth();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeBluetooth();
            } else {
                Log.e("BLUETOOTH", "Bluetooth permission denied");
                finish(); // Close the app or notify the user
            }
        }
    }

    private void initializeBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        initializeBluetoothProfileService();
    }

    private void initializeBluetoothProfileService() {
        BluetoothProfile.ServiceListener serviceListener = new BluetoothProfile.ServiceListener() {
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                BluetoothProfile oldProxy = profileProxies.get(profile);
                if (oldProxy != null) {
                    BluetoothAdapter.getDefaultAdapter().closeProfileProxy(profile, oldProxy);
                }
                profileProxies.put(profile, proxy);
            }

            public void onServiceDisconnected(int profile) {
                BluetoothProfile oldProxy = profileProxies.get(profile);
                if (oldProxy != null) {
                    BluetoothAdapter.getDefaultAdapter().closeProfileProxy(profile, oldProxy);
                    profileProxies.delete(profile);
                }
            }
        };

        bluetoothAdapter.getProfileProxy(this, serviceListener, BluetoothProfile.HEADSET);
        bluetoothAdapter.getProfileProxy(this, serviceListener, BluetoothProfile.A2DP);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);

        connectionBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                handleConnectionStateChange(intent);
            }
        };
        registerReceiver(connectionBroadcastReceiver, filter);
    }

    private void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connect(BluetoothDevice device) {
        connectToBluetoothSocket(device);
        callBluetoothProfileMethod("connect", BluetoothProfile.HEADSET, device);
        callBluetoothProfileMethod("connect", BluetoothProfile.A2DP, device);
    }

    private void connectToBluetoothSocket(BluetoothDevice bluetoothDevice) {
        try {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard SerialPortService ID
            mBluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothAdapter.cancelDiscovery();
            if (!mBluetoothSocket.isConnected()) {
                mBluetoothSocket.connect();
            }
        } catch (IOException connectException) {
            connectException.printStackTrace();
            try {
                Method m = bluetoothDevice.getClass().getMethod("createRfcommSocket", int.class);
                mBluetoothSocket = (BluetoothSocket) m.invoke(bluetoothDevice, 1);
                mBluetoothSocket.connect();
            } catch (Exception e) {
                Log.e("BLUETOOTH_ERROR", e.toString());
                try {
                    mBluetoothSocket.close();
                } catch (IOException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }

    private void disconnect(BluetoothDevice device) {
        callBluetoothProfileMethod("disconnect", BluetoothProfile.HEADSET, device);
        callBluetoothProfileMethod("disconnect", BluetoothProfile.A2DP, device);
        closeBluetoothSocket();
    }

    private void closeBluetoothSocket() {
        try {
            if (mBluetoothSocket != null && mBluetoothSocket.isConnected()) {
                mBluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void callBluetoothProfileMethod(String methodName, int profile, BluetoothDevice device) {
        BluetoothProfile proxy = profileProxies.get(profile);
        if (proxy != null) {
            try {
                Method method = proxy.getClass().getMethod(methodName, BluetoothDevice.class);
                method.invoke(proxy, device);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleConnectionStateChange(Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        int prevState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1);
        int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
        int profile = intent.getAction().equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED) ? BluetoothProfile.HEADSET : BluetoothProfile.A2DP;
        for (StateChangeListener listener : stateChangeListeners) {
            listener.onConnectionStatusChanged(profile, prevState, newState, device);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(connectionBroadcastReceiver);
        for (int i = 0; i < profileProxies.size(); i++) {
            int profile = profileProxies.keyAt(i);
            BluetoothProfile proxy = profileProxies.get(profile);
            BluetoothAdapter.getDefaultAdapter().closeProfileProxy(profile, proxy);
        }
    }

    public interface StateChangeListener {
        void onConnectionStatusChanged(int profile, int prevState, int newState, BluetoothDevice device);
    }
}
