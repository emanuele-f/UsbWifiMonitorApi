package com.usbwifimon.api;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Observable;

public class MyBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "MyBroadcastReceiver";

    public static class CaptureObservable extends Observable {
        private static CaptureObservable instance = new CaptureObservable();
        private CaptureObservable() {};

        public static CaptureObservable getInstance() {
            return instance;
        }

        public void update(boolean running) {
            setChanged();
            notifyObservers(running);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("MyBroadcastReceiver", "onReceive " + action);

        if(action.equals(MainActivity.CAPTURE_STATUS_ACTION)) {
            // Notify via the CaptureObservable
            boolean running = intent.getBooleanExtra("running", true);
            CaptureObservable.getInstance().update(running);
        }
    }
}
