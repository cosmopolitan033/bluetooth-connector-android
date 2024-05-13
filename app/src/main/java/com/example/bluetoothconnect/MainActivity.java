package com.example.bluetoothconnect;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private SparseArray<BluetoothProfile> profileProxies = new SparseArray<>();
    private BroadcastReceiver connectionBroadcastReceiver;
    private List<StateChangeListener> stateChangeListeners = new ArrayList<>();
    private BluetoothDevice device;

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.BLUETOOTH_CONNECT }, 101);
        }

        // Initialize listeners and receiver
        initializeBluetoothProfileService();

        Button connectButton = findViewById(R.id.buttonConnect);
        Button disconnectButton = findViewById(R.id.buttonDisconnect);

        // Get the BluetoothDevice instance
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        device = bluetoothAdapter.getRemoteDevice("50:5E:5C:14:98:69");

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
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

    private void connect(BluetoothDevice device) {
        callBluetoothProfileMethod("connect", BluetoothProfile.HEADSET, device);
        callBluetoothProfileMethod("connect", BluetoothProfile.A2DP, device);
    }

    private void disconnect(BluetoothDevice device) {
        callBluetoothProfileMethod("disconnect", BluetoothProfile.HEADSET, device);
        callBluetoothProfileMethod("disconnect", BluetoothProfile.A2DP, device);
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
