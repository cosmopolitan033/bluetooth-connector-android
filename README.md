# BluetoothConnect

BluetoothConnect is an Android application that allows users to manage Bluetooth connections through an embedded HTTP server. The app can pair with Bluetooth devices and connect or disconnect them using HTTP POST requests. It displays the device's current IP address and listens for commands to manage Bluetooth connections.

## Features

- Pair with Bluetooth devices.
- Connect to paired Bluetooth devices.
- Disconnect from connected Bluetooth devices.
- Display the device's current IP address.
- Handle HTTP POST requests to manage Bluetooth connections.

## Requirements

- Android device with Bluetooth capability.
- Minimum SDK version: 21
- Target SDK version: 33

## Permissions

Ensure the following permissions are included in your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
```

## Setup

1. Clone the repository:

```bash
git clone https://github.com/yourusername/BluetoothConnect.git
cd BluetoothConnect
```

2. Open the project in Android Studio.

3. Sync the project with Gradle to download the required dependencies.

4. Build and run the application on an Android device.

## Usage

### Display IP Address

When the application starts, it will display the device's current IP address. Ensure the device is connected to a network (Wi-Fi or mobile data) to get a valid IP address.

### HTTP Server

The embedded HTTP server listens for HTTP POST requests on port `12345`. You can manage Bluetooth connections using the following commands:

#### Connect to a Bluetooth Device

To connect to a Bluetooth device, send a POST request with the command `connect` and the MAC address of the device:

```sh
curl -X POST http://<IP_ADDRESS>:12345 -d "cmd=connect&mac=<DEVICE_MAC_ADDRESS>"
```

Replace `<IP_ADDRESS>` with the IP address displayed by the app and `<DEVICE_MAC_ADDRESS>` with the MAC address of the Bluetooth device you want to connect.

#### Disconnect from a Bluetooth Device

To disconnect from a Bluetooth device, send a POST request with the command `disconnect`:

```sh
curl -X POST http://<IP_ADDRESS>:12345 -d "cmd=disconnect"
```

### Example

1. Start the app on your Android device.
2. Note the IP address displayed by the app.
3. Open a terminal on your computer and run the following command to connect to a Bluetooth device:

```sh
curl -X POST http://192.168.1.100:12345 -d "cmd=connect&mac=50:5E:5C:14:98:69"
```

4. To disconnect from the Bluetooth device, run:

```sh
curl -X POST http://192.168.1.100:12345 -d "cmd=disconnect"
```

## Debugging

Use Logcat in Android Studio to view logs and debug information. The app provides detailed logs to help you understand the flow and identify any issues.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributions

Contributions are welcome! Please open an issue or submit a pull request.

## Acknowledgments

- [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd) for the embedded HTTP server.
- [Android Bluetooth API](https://developer.android.com/guide/topics/connectivity/bluetooth) for managing Bluetooth connections.


