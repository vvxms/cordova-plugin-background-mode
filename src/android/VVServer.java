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

import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import android.content.Context;

/**
 * Created by loi on 2018/1/18.
 */

public class VVServer extends Service{
    private String TAG  = "VVServer";
    private final int PID = android.os.Process.myPid();
    private AssistServiceConnection mConnection;
    private Timer timer;
    private int curLeftTime;
    public static long wakeMainActivityTime = -1;//全局变量
    private boolean isOpenDebugModel = true;
    Class<?> mClass;
    
    private Timer mTimer = null;
    private TimerTask mTimerTask = null;
    private boolean isStop = true;
    
    private String testLog = "";
    
    private void startTimer(Date date){
        if (mTimer == null) {
            mTimer = new Timer();
        }
        if (mTimerTask == null) {
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    Message message = new Message();
                    message.what = 1;
                    handler.sendMessage(message);
                }
            };
        }

        if(mTimer != null && mTimerTask != null)
        {
            mTimer.schedule(mTimerTask, date);
            isStop = false;
        }
    }
    
    private void stopTimer(){    
        if (mTimer != null) {    
            mTimer.cancel();    
            mTimer = null;    
        }    
        if (mTimerTask != null) {    
            mTimerTask.cancel();    
            mTimerTask = null;    
        }       
        isStop = true;  
    }    
    
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    Log.e("LocalCastielService", String.valueOf(msg.what));
//                     if(isOpenDebugModel)
                        Toast.makeText(VVServer.this,"时间到了",Toast.LENGTH_SHORT).show();
                    
                    Intent notificationIntent;
                    if(mClass!=null){
                        notificationIntent = new Intent(VVServer.this, mClass);
                    }else{
//                         if(isOpenDebugModel)
                            Toast.makeText(VVServer.this,"无法获取activity类名",Toast.LENGTH_SHORT).show();
                        break;
                    }
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
//                     if(isOpenDebugModel)
                        Toast.makeText(VVServer.this,testLog,Toast.LENGTH_SHORT).show();
                    break;
                case 3:
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
        if(isOpenDebugModel)
            Toast.makeText(VVServer.this,"VVServer-onStartCommand",Toast.LENGTH_LONG).show();
        
        //读数据
        if(prop==null){     
            initPropertiesFile(VVServer.this);
        }

        try {
            mClass = Class.forName(prop.get("class").toString());
            if(mClass != null){
                Toast.makeText(VVServer.this,mClass.toString(),Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(VVServer.this,"获取包名失败",Toast.LENGTH_LONG).show();
            }
        } catch (ClassNotFoundException e) 
        {    
            Toast.makeText(VVServer.this,e.toString(),Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return START_STICKY;
        }              
       
       try {
           wakeMainActivityTime = Long.parseLong(prop.get("time").toString());
           if(wakeMainActivityTime == 100){
               if(isOpenDebugModel)
                   Toast.makeText(VVServer.this,"未配置时间："+prop.get("class").toString(),Toast.LENGTH_LONG).show();
               return START_STICKY;
           }
       } catch (NumberFormatException nfe) {
               return START_STICKY;
       }
           
        if(isOpenDebugModel)
            Toast.makeText(VVServer.this,"时间值对比 "+ "当前的："+new Date(System.currentTimeMillis()).toString()+" 储存的："+new Date(wakeMainActivityTime).toString(),Toast.LENGTH_LONG).show();
         
        if(System.currentTimeMillis()>wakeMainActivityTime)
        {
            if(isOpenDebugModel)
               Toast.makeText(VVServer.this,"时间点已错过: "+ new Date(wakeMainActivityTime).toString(),Toast.LENGTH_LONG).show();
        }else 
        {
            if(isOpenDebugModel)
                Toast.makeText(VVServer.this,"时间点未到达: "+ new Date(wakeMainActivityTime).toString(),Toast.LENGTH_LONG).show();
            if(isStop){
                if(isOpenDebugModel)
                    Toast.makeText(VVServer.this,"定时器未开启",Toast.LENGTH_LONG).show();                          
                startTimer(new Date(wakeMainActivityTime));
            }else{
                if(isOpenDebugModel)
                      Toast.makeText(VVServer.this,"未关闭，关闭后重新开启"+ new Date(wakeMainActivityTime).toString(),Toast.LENGTH_LONG).show();    
                stopTimer();    
                startTimer(new Date(wakeMainActivityTime));
            }

      
        }
        return START_STICKY;
//         return super.onStartCommand(intent, flags, startId);
    }
    
    

    @Override
    public void onDestroy() {
        if(isOpenDebugModel)
            Toast.makeText(VVServer.this,"VVServer-onDestroy",Toast.LENGTH_LONG).show();
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setForeground(); 
        
            //读数据
        if(prop==null){     
            initPropertiesFile(VVServer.this);
        }

        try {
            mClass = Class.forName(prop.get("class").toString());
            if(mClass != null){
                Toast.makeText(VVServer.this,mClass.toString(),Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(VVServer.this,"获取包名失败",Toast.LENGTH_LONG).show();
            }
        } catch (ClassNotFoundException e) 
        {    
            Toast.makeText(VVServer.this,e.toString(),Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return;
        }              
       
       try {
           wakeMainActivityTime = Long.parseLong(prop.get("time").toString());
           if(wakeMainActivityTime == 100){
               if(isOpenDebugModel)
                   Toast.makeText(VVServer.this,"未配置时间："+prop.get("class").toString(),Toast.LENGTH_LONG).show();
               return;
           }
       } catch (NumberFormatException nfe) {
               return;
       }
           
        if(isOpenDebugModel)
            Toast.makeText(VVServer.this,"时间值对比 "+ "当前的："+new Date(System.currentTimeMillis()).toString()+" 储存的："+new Date(wakeMainActivityTime).toString(),Toast.LENGTH_LONG).show();
         
        if(System.currentTimeMillis()>wakeMainActivityTime)
        {
            if(isOpenDebugModel)
               Toast.makeText(VVServer.this,"时间点已错过: "+ new Date(wakeMainActivityTime).toString(),Toast.LENGTH_LONG).show();
        }else 
        {
            if(isOpenDebugModel)
                Toast.makeText(VVServer.this,"时间点未到达: "+ new Date(wakeMainActivityTime).toString(),Toast.LENGTH_LONG).show();
            if(isStop){
                if(isOpenDebugModel)
                    Toast.makeText(VVServer.this,"定时器未开启",Toast.LENGTH_LONG).show();                          
                startTimer(new Date(wakeMainActivityTime));
            }else{
                if(isOpenDebugModel)
                      Toast.makeText(VVServer.this,"未关闭，关闭后重新开启"+ new Date(wakeMainActivityTime).toString(),Toast.LENGTH_LONG).show();    
                stopTimer();    
                startTimer(new Date(wakeMainActivityTime));
            }

      
        }
        
        
//         if(isOpenDebugModel)
            Toast.makeText(VVServer.this,"VVServer-onCreate",Toast.LENGTH_LONG).show();
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
                .build();

        return notification;
    }
    
   
    public static long getCurrentTime2Stamp() {
        return  System.currentTimeMillis();
    }

    public static Date getStamp2Date(long time) {
        return new Date(time);
    }
    
    
    public static Properties prop;
    public static void initPropertiesFile(Context context) {
        prop = loadConfig(context, "/data/data/" + context.getPackageName()+ "/config.properties");
        Toast.makeText(context,"路径" + "/data/data/" + context.getPackageName()+ "/config.properties",Toast.LENGTH_LONG).show();
        if (prop == null) {
            // 配置文件不存在的时候创建配置文件 初始化配置信息
            Toast.makeText(context,"配置文件新建了",Toast.LENGTH_LONG).show();
       
            prop = new Properties();
            prop.put("time","100");
            prop.put("class","com.limainfo.vv.Vv___");
            saveConfig(context, "/data/data/" + context.getPackageName()+ "/config.properties", prop);
        }
    }

    /**
     * 保存配置文件
     * <p>
     * Title: saveConfig
     * <p>
     * <p>
     * Description:
     * </p>
     *
     * @param context
     * @param file
     * @param properties
     * @return
     */
    public static boolean saveConfig(Context context, String file,
                                     Properties properties) {
        try {
            File fil = new File(file);
            if (!fil.exists())
                fil.createNewFile();
            FileOutputStream s = new FileOutputStream(fil);
            properties.store(s, "");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static Properties loadConfig(Context context, String file) {
        Properties properties = new Properties();
        try {
            FileInputStream s = new FileInputStream(file);
            properties.load(s);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return properties;
    }
    
}
