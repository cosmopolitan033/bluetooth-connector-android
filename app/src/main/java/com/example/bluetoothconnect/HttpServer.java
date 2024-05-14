package com.example.bluetoothconnect;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpServer extends NanoHTTPD {
    private static final String TAG = "HttpServer";
    private BluetoothService bluetoothService;

    public HttpServer(int port, BluetoothService bluetoothService) {
        super(port);
        this.bluetoothService = bluetoothService;
    }

    @Override
    public Response serve(IHTTPSession session) {
        if (Method.POST.equals(session.getMethod())) {
            try {
                session.parseBody(new HashMap<>());
                Map<String, String> postData = session.getParms();
                String cmd = postData.get("cmd");
                String mac = postData.get("mac");

                if ("connect".equals(cmd) && mac != null) {
                    BluetoothDevice device = bluetoothService.getBluetoothAdapter().getRemoteDevice(mac);
                    bluetoothService.pairDevice(device);
                    bluetoothService.connect(device);
                    return newFixedLengthResponse("Connected to " + mac);
                } else if ("disconnect".equals(cmd)) {
                    BluetoothDevice device = bluetoothService.getBluetoothAdapter().getRemoteDevice(BluetoothService.DEVICE_ADDRESS);
                    bluetoothService.disconnect(device);
                    return newFixedLengthResponse("Disconnected from " + BluetoothService.DEVICE_ADDRESS);
                } else {
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid command");
                }
            } catch (IOException | ResponseException e) {
                Log.e(TAG, "Error parsing request", e);
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal Server Error");
            }
        } else {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Method Not Allowed");
        }
    }

    public void startServer() {
        try {
            start(SOCKET_READ_TIMEOUT, false);
            Log.i(TAG, "HTTP server started");
        } catch (IOException e) {
            Log.e(TAG, "Failed to start HTTP server", e);
        }
    }

    public void stopServer() {
        stop();
        Log.i(TAG, "HTTP server stopped");
    }
}
