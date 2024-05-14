package com.example.bluetoothconnect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_CONNECT = 2;
    private static final String TAG = "MainActivity";
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothService bluetoothService;
    private HttpServer httpServer;
    private TextView ipTextView;
    private TextView instructionsTextView;

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ipTextView = findViewById(R.id.ipTextView);
        instructionsTextView = findViewById(R.id.instructionsTextView);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Device doesn't support Bluetooth");
            finish();
            return;
        }

        bluetoothService = new BluetoothService(this, bluetoothAdapter);

        if (!checkBluetoothPermissions()) {
            return;
        }

        String ipAddress = NetworkUtils.getIPAddress(true);
        ipTextView.setText("IP Address: " + ipAddress);

        String instructions = "<b>Usage Instructions:</b><br><br>" +
                "1. <b>Connect to a Bluetooth device:</b><br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;<b>POST</b> http://" + ipAddress + ":12345<br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;<b>Headers:</b><br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Content-Type: application/x-www-form-urlencoded<br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;<b>Body:</b><br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;cmd=connect&mac=&lt;DEVICE_MAC_ADDRESS&gt;<br><br>" +
                "2. <b>Disconnect from a Bluetooth device:</b><br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;<b>POST</b> http://" + ipAddress + ":12345<br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;<b>Headers:</b><br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Content-Type: application/x-www-form-urlencoded<br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;<b>Body:</b><br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;cmd=disconnect<br><br>" +
                "<b>Example using curl:</b><br>" +
                "1. <b>Connect:</b><br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;curl -X POST http://" + ipAddress + ":12345 -d \"cmd=connect&mac=&lt;DEVICE_MAC_ADDRESS&gt;\"<br><br>" +
                "2. <b>Disconnect:</b><br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;curl -X POST http://" + ipAddress + ":12345 -d \"cmd=disconnect\"<br><br>" +
                "<b>Example using Postman:</b><br>" +
                "1. <b>Connect:</b><br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;- Set method to POST<br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;- Set URL to http://" + ipAddress + ":12345<br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;- Set Headers:<br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Content-Type: application/x-www-form-urlencoded<br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;- Set Body (x-www-form-urlencoded):<br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;key: cmd, value: connect<br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;key: mac, value: &lt;DEVICE_MAC_ADDRESS&gt;<br><br>" +
                "2. <b>Disconnect:</b><br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;- Set method to POST<br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;- Set URL to http://" + ipAddress + ":12345<br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;- Set Headers:<br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Content-Type: application/x-www-form-urlencoded<br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;- Set Body (x-www-form-urlencoded):<br>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;key: cmd, value: disconnect<br>";

        instructionsTextView.setText(Html.fromHtml(instructions));


        startHttpServer(ipAddress);
    }

    private boolean checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                }, REQUEST_BLUETOOTH_CONNECT);
                return false;
            }
        }
        initializeBluetooth();
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_CONNECT && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeBluetooth();
        } else {
            Log.e(TAG, "Bluetooth permission denied");
            finish();
        }
    }

    @SuppressLint("MissingPermission")
    private void initializeBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
        }

        if (bluetoothService != null) {
            bluetoothService.initializeBluetoothProfileService();
        }
    }

    private void startHttpServer(String ipAddress) {
        try {
            httpServer = new HttpServer(12345, bluetoothService);
            httpServer.start();
            Log.i(TAG, "HTTP server started at " + ipAddress + ":12345");
        } catch (IOException e) {
            Log.e(TAG, "Failed to start HTTP server", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothService != null) {
            bluetoothService.cleanup();
        }
        if (httpServer != null) {
            httpServer.stop();
        }
    }
}
