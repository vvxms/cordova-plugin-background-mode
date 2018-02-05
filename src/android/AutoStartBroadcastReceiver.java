package de.appplant.cordova.plugin.background;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

/**
 * Created by loi on 2018/1/9.
 */

//开机自启动广播接受
public class AutoStartBroadcastReceiver extends BroadcastReceiver {
    static final String action_boot ="android.intent.action.BOOT_COMPLETED";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(action_boot)){
            Toast.makeText(context,"开机自启动",Toast.LENGTH_SHORT).show();
             Intent i = new Intent(context, VVServer.class);
             context.startService(i);
             Intent intent = new Intent(context, LocalCastielService.class);
             context.startService(intent);
             Intent intent1 = new Intent(context, RemoteCastielService.class);
             context.startService(intent1);
        }

        if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {//如果广播是每分钟发送一次的时间广播
            Log.e("timeBroad", "时间变化了");
            if (!isServiceWork(context,"in.lucasdup.bringtofront.VVServer")) {
                Intent i = new Intent(context, VVServer.class);
                context.startService(i);
            }  
        }
    }

    // 判断服务是否正在运行
    public boolean isServiceWork(Context mContext, String serviceName) {
        boolean isWork = false;
        ActivityManager myAM = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> myList = myAM.getRunningServices(100);
        if (myList.size() <= 0) {
            return false;
        }
        for (int i = 0; i < myList.size(); i++) {
            String mName = myList.get(i).service.getClassName().toString();
            if (mName.equals(serviceName)) {
                isWork = true;
                break;
            }
        }
        return isWork;
    }
}
