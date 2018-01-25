package de.appplant.cordova.plugin.background;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class AssistServiceTwo extends Service {
    private static final String TAG = "AssistServiceTwo";

    public class LocalBinder extends Binder {
        public AssistServiceTwo getService() {
            return AssistServiceTwo.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "AssistServiceTwo: onBind()");
        return new LocalBinder();
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        Log.e(TAG, "AssistServiceTwo: onDestroy()");
        super.onDestroy();

    }

}
