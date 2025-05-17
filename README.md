USB WiFi Monitor API
--------------------

This repository contains the API specification of the [USB WiFi Monitor](https://play.google.com/store/apps/details?id=com.usbwifimon.app) app, along with a sample app.

## The API

USB WiFi Monitor exposes an Intent-based API to control the capture. This is available since version 1.2.0.

Here is, for example, how to start the capture via adb:

```bash
adb shell am start -e action [ACTION] start -n com.usbwifimon.app/.CaptureCtrl
```

where ACTION is one of:
  - `start`: starts the capture with the specified parameters
  - `stop`: stops the capture
  - `get_status`: get the capture status

The Intent above can also be triggered programmatically from your app:

```java
class YourActivity extends Activity {
  private final ActivityResultLauncher<Intent> captureLauncher =
    registerForActivityResult(new StartActivityForResult(), this::handleCaptureResult);

  void startCapture() {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setClassName("com.usbwifimon.app", "com.usbwifimon.app.CaptureCtrl");

    intent.putExtra("action", "start");
    intent.putExtra("channel", 6);
    intent.putExtra("pcap_name", "capture.pcap");

    captureLauncher.launch(intent);
  }

  void handleCaptureResult(final ActivityResult result) {
    if(result.getResultCode() == RESULT_OK) {
      // command executed successfully
    }
  }
}
```

You can load this project in Android Studio to build the sample app, to try the API interaction.

## Capture Settings

The capture settings are specified via the Intent extras (`-e param value` when using adb).

Here is the list of supported paramters.

| Parameter               | Type   | Ver | Default | Value                                                                       |
|-------------------------|--------|-----|-------- |-----------------------------------------------------------------------------|
| channel                 | int    |   4 |      -1 | fixed channel (1-11), or channel hope mode (-1)                             |
| channel_width           | int    |   4 |       1 | specifies the channel width - 0: no HT, 1: HT20, 2: HT40-, 3: HT40+         |
| pcap_name               | string |   4 |         | dump traffic to a PCAP file, at /sdcard/Download/UsbWifiMonitor/*pcap_name* |
| broadcast_receiver      | string |   4 |         | component name of a BroadcastReceiver for status updates, see below         |

The `Ver` column indicates the minimum app version required to use the given parameter. The app version can be queried via the `get_status` action as explained below (`version_code`).

## Query the Capture Status

You can check if the capture is currently running by sending an Intent with the `get_status` action. The response Intent contains the following extras:

| Field               | Type   | Value                                                             |
|---------------------|--------|-------------------------------------------------------------------|
| version_name        | string | the app versionName (e.g. "1.2.0")                                |
| version_code        | int    | the app versionCode, an incremental number for the release        |
| running             | bool   | true if the capture is currently running                          |

Other than via the API, the capture may be manually stopped by the user from the app or due to a bug in the driver.
In order to be notified when the capture is stopped, you can create a `BroadcastReceiver` and subscribe to the `com.usbwifimon.app.CaptureStatus` action. Here is an example:

```xml
<receiver android:name=".MyBroadcastReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="com.usbwifimon.app.CaptureStatus" />
    </intent-filter>
</receiver>
```

To tell USB WiFi Monitor to send the Intent to your receiver, you must specify its component name in the `broadcast_receiver` extra of the start intent:

```java
intent.putExtra("action", "start");
intent.putExtra("broadcast_receiver", "com.usbwifimon.api.MyBroadcastReceiver"");
...
captureStartLauncher.launch(intent);
```

The receiver will get an intent with the `running` extra set to `false` when the capture is stopped.
