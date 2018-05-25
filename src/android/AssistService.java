package de.appplant.cordova.plugin.background;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class AssistService extends Service {
    private static final String TAG = "AssistService";

    public class LocalBinder extends Binder {
        public AssistService getService() {
            return AssistService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "AssistService: onBind()");
        return new LocalBinder();
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        Log.e(TAG, "AssistService: onDestroy()");
        super.onDestroy();

    }

}
