package de.appplant.cordova.plugin.background;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;
import android.app.PendingIntent;
import android.content.SharedPreferences;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

/**
 * Created by loi on 2018/1/18.
 */

public class VVServer extends Service{
    private String TAG  = "VVServer";
    private final int PID = android.os.Process.myPid();
    private AssistServiceConnection mConnection;
    private Timer timer;
    private int curLeftTime;
    public static int wakeMainActivityTime = -1;//全局变量
    private boolean isOpenDebugModel = false;
    private static String classNameStr = "com.phonegap.helloworld.VV_KeppAlive_demo";
    Class<?> mClass;
    
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    Toast.makeText(VVServer.this,"时间到了",Toast.LENGTH_SHORT).show();
                    Log.e("LocalCastielService", String.valueOf(msg.what));
                    if(isOpenDebugModel)
                        Toast.makeText(VVServer.this,"时间到了",Toast.LENGTH_SHORT).show();
                    Intent notificationIntent;
//                     if(BackgroundMode.mActivity!=null){
//                         notificationIntent = new Intent(VVServer.this, BackgroundMode.mActivity.getClass());
//                         if(isOpenDebugModel)
//                             Toast.makeText(VVServer.this,BackgroundMode.mActivity.getClass().toString(),Toast.LENGTH_SHORT).show();
                        
//                     }else{
//                         Toast.makeText(VVServer.this,"activity没了",Toast.LENGTH_SHORT).show();
//                         notificationIntent = new Intent(VVServer.this, com.phonegap.helloworld.VV_KeppAlive_demo.class);
// //                         notificationIntent = new Intent(VVServer.this, mClass);
//                     }
                    
                    notificationIntent = new Intent(VVServer.this, com.phonegap.helloworld.VV_KeppAlive_demo.class);
//                     notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                     VVServer.this.startActivity(notificationIntent);
//                     Toast.makeText(VVServer.this,mClass.toString()+"****"+classNameStr ,Toast.LENGTH_SHORT).show();
                    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP |Intent.FLAG_ACTIVITY_NEW_TASK);
                    PendingIntent pendingIntent = PendingIntent.getActivity(VVServer.this, 0, notificationIntent, 0);
                    try 
                    {
                      pendingIntent.send();
                    }
                    catch (PendingIntent.CanceledException e) 
                    {
                      e.printStackTrace();
                    }
                    break;
                case 2:
                    Log.e("LocalCastielService", String.valueOf(msg.what));
                    Toast.makeText(VVServer.this,"wakeMainActivityTime: "+wakeMainActivityTime,Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    //BringToFront.executeGlobalJavascript("alert('你好啊')");
                    break;
            }
            return true;
        }
    });
    
  @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if(isOpenDebugModel)
            Toast.makeText(VVServer.this,"VVServer-onBind",Toast.LENGTH_LONG).show();
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        
        //读数据
        SharedPreferences alermTime  = VVServer.this.getSharedPreferences("alermTime ", 0);
        if(alermTime!=null && !alermTime.getString("time", "").equals("")){
            wakeMainActivityTime = Integer.parseInt(alermTime.getString("time", ""));
        }
        
//         if(isOpenDebugModel)
            Toast.makeText(VVServer.this,"VVServer-onStartCommand",Toast.LENGTH_LONG).show();
    
        if(timer == null){
            //curLeftTime = wakeMainActivityTime;
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    
                     Message message = new Message();
                     message.what = 1;
                     handler.sendMessage(message); 
                    
//                     if(wakeMainActivityTime == 0)
//                     {
//                         Message message = new Message();
//                         message.what = 1;
//                         handler.sendMessage(message);               
//                     }
//                     if(wakeMainActivityTime>=0){
//                         wakeMainActivityTime --;
//                     }
                }
            },30000,30000);
        }

        return START_STICKY;
//         return super.onStartCommand(intent, flags, startId);
    }
    
    

    @Override
    public void onDestroy() {
//         if(isOpenDebugModel)
            Toast.makeText(VVServer.this,"VVServer-onDestroy",Toast.LENGTH_LONG).show();
        
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setForeground();
        //classNameStr = BackgroundMode.mActivity.getClass().getName();
        //mClass = BackgroundMode.mActivity.getClass();
        
        //读数据
        SharedPreferences alermTime  = VVServer.this.getSharedPreferences("alermTime ", 0);
        if(alermTime!=null && !alermTime.getString("time", "").equals("")){
            wakeMainActivityTime = Integer.parseInt(alermTime.getString("time", ""));
            Toast.makeText(VVServer.this,"读取数据成功: "+ wakeMainActivityTime,Toast.LENGTH_LONG).show();
        }
       Toast.makeText(VVServer.this,"VVServer-onCreate",Toast.LENGTH_LONG).show();
        
//         if(isOpenDebugModel)
//             Toast.makeText(VVServer.this,"VVServer-onCreate: "+ wakeMainActivityTime,Toast.LENGTH_LONG).show();

        if(timer == null){
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    
                    Message message = new Message();
                    message.what = 1;
                    handler.sendMessage(message);
                    
                    
//                     if( wakeMainActivityTime == 0 )
//                     {
//                         Message message = new Message();
//                         message.what = 1;
//                         handler.sendMessage(message);
//                     }
//                     if(wakeMainActivityTime>=0){
                        
//                         if(wakeMainActivityTime%4 == 0){
//                             Message messages = new Message();
//                             messages.what = 2;
//                             handler.sendMessage(messages);
//                         }
//                         wakeMainActivityTime --;
//                     }
                    
                }
            },0,30000);
        }

    }
    
      public void setForeground() {
        // sdk < 18 , 直接调用startForeground即可,不会在通知栏创建通知
        if (Build.VERSION.SDK_INT < 18) {
            VVServer.this.startForeground(PID, getNotification());
            return;
        }

        if (null == mConnection) {
            mConnection = new AssistServiceConnection();
        }

        this.bindService(new Intent(VVServer.this, AssistService.class), mConnection,
                Service.BIND_AUTO_CREATE);
    }

    private class AssistServiceConnection implements ServiceConnection {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "VVServer: onServiceDisconnected");
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d(TAG, "VVServer: onServiceConnected");

            // sdk >=18
            // 的，会在通知栏显示service正在运行，这里不要让用户感知，所以这里的实现方式是利用2个同进程的service，利用相同的notificationID，
            // 2个service分别startForeground，然后只在1个service里stopForeground，这样即可去掉通知栏的显示
            Service assistService = ((AssistService.LocalBinder) binder)
                    .getService();
            VVServer.this.startForeground(PID, getNotification());
            assistService.startForeground(PID, getNotification());
            assistService.stopForeground(true);

            VVServer.this.unbindService(mConnection);
            mConnection = null;
        }
    }

    private Notification getNotification() {
        Notification notification = new NotificationCompat.Builder(VVServer.this)
                .setContentTitle("VV")
                /**设置通知的内容**/
                .setContentText("点击跳转")
                /**通知产生的时间，会在通知信息里显示**/
                .setWhen(System.currentTimeMillis())
                /**设置该通知优先级**/
                .setPriority(Notification.PRIORITY_DEFAULT)
                /**设置这个标志当用户单击面板就可以让通知将自动取消**/
                .setAutoCancel(true)
                /**设置他为一个正在进行的通知。他们通常是用来表示一个后台任务,用户积极参与(如播放音乐)或以某种方式正在等待,因此占用设备(如一个文件下载,同步操作,主动网络连接)**/
                .setOngoing(false)
                /**向通知添加声音、闪灯和振动效果的最简单、最一致的方式是使用当前的用户默认设置，使用defaults属性，可以组合：**/
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                .setContentIntent(PendingIntent.getActivity(VVServer.this, 2, new Intent(VVServer.this, com.phonegap.helloworld.VV_KeppAlive_demo.class), PendingIntent.FLAG_CANCEL_CURRENT))
                .build();

        return notification;
    }
    
    
    /**
     * 起始时间为2010-01-01 00:00:00
     * 将获取当前时间并转换为时间戳
     *
     * @return 时间戳
     */
    public static int getCurrentTime2Stamp() {
        Date startTime = null;
        try {
            startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse("2010-01-01 00:00:00");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (startTime != null) {
            return (int) ((System.currentTimeMillis() - startTime.getTime()) / 1000);
        }
        return -1;
    }
}
