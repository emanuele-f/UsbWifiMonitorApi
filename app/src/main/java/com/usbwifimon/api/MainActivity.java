package com.usbwifimon.api;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.usbwifimonitor.api.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

public class MainActivity extends AppCompatActivity implements Observer {
    static final String USBWIFIMON_PACKAGE = "com.usbwifimon.app";
    static final String CAPTURE_CTRL_ACTIVITY = "com.usbwifimon.app.CaptureCtrl";
    static final String CAPTURE_STATUS_ACTION = "com.usbwifimon.app.CaptureStatus";
    static final String TAG = "MainActivity";

    Button mStart;
    boolean mCaptureRunning = false;

    private final ActivityResultLauncher<Intent> captureStartLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::handleCaptureStartResult);
    private final ActivityResultLauncher<Intent> captureStopLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::handleCaptureStopResult);
    private final ActivityResultLauncher<Intent> captureStatusLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::handleCaptureStatusResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStart = findViewById(R.id.start_btn);
        mStart.setOnClickListener(v -> {
            if(!mCaptureRunning)
                startCapture();
            else
                stopCapture();
        });

        if((savedInstanceState != null) && savedInstanceState.containsKey("capture_running"))
            setCaptureRunning(savedInstanceState.getBoolean("capture_running"));
        else
            queryCaptureStatus();

        // will call the "update" method when the capture status changes
        MyBroadcastReceiver.CaptureObservable.getInstance().addObserver(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyBroadcastReceiver.CaptureObservable.getInstance().deleteObserver(this);
    }

    @Override
    public void update(Observable o, Object arg) {
        boolean capture_running = (boolean)arg;
        Log.d(TAG, "capture_running: " + capture_running);
        setCaptureRunning(capture_running);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle bundle) {
        bundle.putBoolean("capture_running", mCaptureRunning);
        super.onSaveInstanceState(bundle);
    }

    void queryCaptureStatus() {
        Log.d(TAG, "Querying USBWiFiMonitor");

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClassName(USBWIFIMON_PACKAGE, CAPTURE_CTRL_ACTIVITY);
        intent.putExtra("action", "get_status");

        try {
            captureStatusLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "The USB WiFi Monitor package not found: " + USBWIFIMON_PACKAGE, Toast.LENGTH_LONG).show();
        }
    }

    void startCapture() {
        Log.d(TAG, "Starting USBWiFiMonitor");

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClassName(USBWIFIMON_PACKAGE, CAPTURE_CTRL_ACTIVITY);

        intent.putExtra("action", "start");

        // Channel 1-11 or -1 (default) for channel hopping
        intent.putExtra("channel", -1);

        // 0: NO_HT
        // 1: HT20 (default)
        // 2: HT40-
        // 3: HT40+
        intent.putExtra("channel_width", 1);

        // Enable the dump to PCAP file
        final DateFormat fmt = new SimpleDateFormat("dd_MMM_HH_mm_ss", Locale.US);
        String pcapName = "Capture_" + fmt.format(new Date()) + ".pcap";
        intent.putExtra("pcap_name", pcapName);

        // Specify the BroadcastReceiver to get notified if the capture is stopped
        intent.putExtra("broadcast_receiver", "com.usbwifimon.api.MyBroadcastReceiver");

        captureStartLauncher.launch(intent);
    }

    void stopCapture() {
        Log.d(TAG, "Stopping USBWiFiMonitor");

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClassName(USBWIFIMON_PACKAGE, CAPTURE_CTRL_ACTIVITY);
        intent.putExtra("action", "stop");

        captureStopLauncher.launch(intent);
    }

    void setCaptureRunning(boolean running) {
        mCaptureRunning = running;
        mStart.setText(running ? "Stop Capture" : "Start Capture");
    }

    void handleCaptureStartResult(final ActivityResult result) {
        Log.d(TAG, "USBWiFiMonitor start result: " + result);

        if(result.getResultCode() == RESULT_OK) {
            Toast.makeText(this, "Capture started!", Toast.LENGTH_SHORT).show();
            setCaptureRunning(true);
        } else
            Toast.makeText(this, "Capture failed to start", Toast.LENGTH_SHORT).show();
    }

    void handleCaptureStopResult(final ActivityResult result) {
        Log.d(TAG, "USBWiFiMonitor stop result: " + result);

        if(result.getResultCode() == RESULT_OK) {
            Toast.makeText(this, "Capture stopped!", Toast.LENGTH_SHORT).show();
            setCaptureRunning(false);
        } else
            Toast.makeText(this, "Could not stop capture", Toast.LENGTH_SHORT).show();
    }

    void handleCaptureStatusResult(final ActivityResult result) {
        Log.d(TAG, "USBWiFiMonitor status result: " + result);

        if((result.getResultCode() == RESULT_OK) && (result.getData() != null)) {
            Intent intent = result.getData();
            boolean running = intent.getBooleanExtra("running", false);
            int verCode = intent.getIntExtra("version_code", 0);
            String verName = intent.getStringExtra("version_name");

            Log.d(TAG, "USBWiFiMonitor version " + verName + " (" + verCode + "): running=" + running);
            setCaptureRunning(running);
        }
    }
}