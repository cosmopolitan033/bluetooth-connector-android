package com.example.bluetoothconnect;

import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class NetworkUtils {
    private static final String TAG = "NetworkUtils";

    public static String getIPAddress(boolean useIPv4) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                Log.e(TAG, "No network interfaces found");
                return "";
            }
            for (NetworkInterface intf : Collections.list(interfaces)) {
                Enumeration<InetAddress> addresses = intf.getInetAddresses();
                if (addresses == null) {
                    continue;
                }
                for (InetAddress addr : Collections.list(addresses)) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':') < 0;
                        if (useIPv4) {
                            if (isIPv4) {
                                Log.d(TAG, "IPv4 Address: " + sAddr);
                                return sAddr;
                            }
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                                String ip6Addr = delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                                Log.d(TAG, "IPv6 Address: " + ip6Addr);
                                return ip6Addr;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting IP address", e);
        }
        return "";
    }
}
