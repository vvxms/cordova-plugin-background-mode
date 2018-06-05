package de.appplant.cordova.plugin.background;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import android.app.PendingIntent;
import java.util.List;
import android.os.Build;

/**
 * Created by loi on 2018/1/9.
 */

//开机自启动广播接受
public class AutoStartBroadcastReceiver extends BroadcastReceiver {
    static final String action_boot ="android.intent.action.BOOT_COMPLETED";
    static final String action_WakePage ="VV_Test";
    @Override
    public void onReceive(Context context, Intent intent) {
        VVServer.WriteLog(context, " 进入AutoStartBroadcastReceiver\n");
        if (intent.getAction().equals(action_boot)){
            VVServer.WriteLog(context, " 启动VVServer、LocalCastielService、RemoteCastielService---开始\n");
            Intent i = new Intent(context, VVServer.class);
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
                context.startForegroundService(i);
            }
            else {
                context.startService(i);
            }            
//             Intent intent0 = new Intent(context, LocalCastielService.class);
//             context.startService(intent0);
//             Intent intent1 = new Intent(context, RemoteCastielService.class);
//             context.startService(intent1);
            VVServer.WriteLog(context, " 启动VVServer、LocalCastielService、RemoteCastielService---结束\n");
        }

        if(intent.getAction().equals(action_WakePage)){    
            VVServer.WriteLog(context, " 广播拉起程序---开始");
            String classinfo = intent.getExtras().getString("ClassInfo");
            Class<?> mClass;
            try {   
                if(classinfo != null){
                    mClass = Class.forName("com.limainfo.vv.Vv___");  
                    Intent notificationIntent;
                    notificationIntent = new Intent(context, mClass);
                    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
                    try 
                    {
                        pendingIntent.send();
                    }
                    catch (PendingIntent.CanceledException e) 
                    {
                        e.printStackTrace();
                    }
                }

            } catch (ClassNotFoundException e) 
            {    
                e.printStackTrace();  
            }   
            VVServer.WriteLog(context, " 广播拉起结束---开始");
        }
        
        if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {//如果广播是每分钟发送一次的时间广播
            VVServer.WriteLog(context, " 一分钟一次广播---进入");
            if (!isServiceWork(context,"de.appplant.cordova.plugin.background.VVServer")) {
                Intent intent5 = new Intent(context, VVServer.class);
                context.startService(intent5);
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
