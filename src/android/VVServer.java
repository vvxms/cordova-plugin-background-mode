package de.appplant.cordova.plugin.background;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Looper;
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
import android.app.NotificationChannel;
import android.app.NotificationManager;
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

import android.os.SystemClock;
import android.app.AlarmManager;
import android.os.PowerManager;
/**
 * Created by loi on 2018/1/18.
 */
import com.tencent.bugly.crashreport.CrashReport;
import static android.os.PowerManager.PARTIAL_WAKE_LOCK;
import static android.content.Context.MODE_PRIVATE;

public class VVServer extends Service{
    private String TAG  = "VVServer";
    private final int PID = android.os.Process.myPid();
    private AssistServiceConnection mConnection;
    private Timer timer;
    private int curLeftTime;
    public static long wakeMainActivityTime = 1000;//全局变量
    private static boolean isOpenDebugModel = false;
    private Class<?> mClass = null;
    
    private static Timer mTimer = null;
    private static TimerTask mTimerTask = null;
    private static boolean isStop = true;
    private static String testLog = "-";
    
        
    public static void  WriteLog(Context context,String strLog)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences("TimeFile", MODE_PRIVATE);
        if (sharedPreferences != null) {
            String log = sharedPreferences.getString("Log","");
            log += "  ";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// HH:mm:ss 
            Date date = new Date(System.currentTimeMillis()); 
            log += simpleDateFormat.format(date)+" ";
            log += strLog;
            sharedPreferences.edit().putString("Log", log).commit();
        }
    }
    
    private static int tempTime = 0;
    private void startTimer(boolean isUseDate,Date date,int delay,int period){
        if (mTimer == null) {
            mTimer = new Timer();
        }
        if (mTimerTask == null) {
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    tempTime++;
                    if(tempTime%30 == 0){
                        WriteLog(VVServer.this,"----30s----\n");
                    }
              
                    if(wakeMainActivityTime/1000 - System.currentTimeMillis()/1000 == 0)
                    {
                        WriteLog(VVServer.this,"VVServer定时器读配置文件尝试拉起 \n");
                        Message message = new Message();
                        message.what = 1;
                        handler.sendMessage(message);
                    }
                }
            };
        }

        if(mTimer != null && mTimerTask != null)
        {
            if(isUseDate){
                mTimer.schedule(mTimerTask, date);
            }else{
                mTimer.schedule(mTimerTask,delay,period);
            }
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
                    WriteLog(VVServer.this,"尝试拉起--开始\n");
                    Intent notificationIntent;     
                    notificationIntent = new Intent(VVServer.this, com.limainfo.vv.Vv___.class);     
                    WakeScreen();    
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
                    WriteLog(VVServer.this,"尝试拉起--结束\n");
                    break;
            }
            return true;
        }
    });
    
  @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static String ACTION_ALARM = "action_alarm";
    private Handler mHanler = new Handler(Looper.getMainLooper());
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent!=null)
        {
            if(intent.getAction()!=null){
                if(intent.getAction().equals(ACTION_ALARM))
                {
                    mHanler.post(new Runnable() {
                    @Override
                    public void run() {
                        WriteLog(VVServer.this,"AlarmManager尝试启动闹钟\n");
                        Message message = new Message();
                        message.what = 1;
                        handler.sendMessage(message);
                    }
                    });
                }
            }

        }
        
        return START_STICKY;
    }
    
    
    @Override
    public void onTaskRemoved(Intent rootIntent){
        super.onTaskRemoved(rootIntent);
    }
    
    @Override
    public void onDestroy() {
        if(isOpenDebugModel)
            Toast.makeText(VVServer.this,"VVServer-onDestroy",Toast.LENGTH_LONG).show();
        releaseWakeLock();
        //关闭时停止定时器
        if(!isStop){
            stopTimer();    
        }
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();  
        
        BackgroundMode.alarm(VVServer.this, 1);
        /*
        SharedPreferences sharedPreferencesRead = this.getSharedPreferences("TimeFile", MODE_PRIVATE);
        if(sharedPreferencesRead!=null){
            String strTime = sharedPreferencesRead.getString("Time","");
            if(!strTime.equals("")){
                wakeMainActivityTime = Long.parseLong(strTime);
                long curTime = System.currentTimeMillis();
                long leftTime = wakeMainActivityTime - curTime;//单位毫秒
                if( leftTime/1000 > 0 ){//如果设置的时间大于当前时间就去启动一个AlarmManager去拉起程序
                    BackgroundMode.alarm(VVServer.this, (int)(leftTime/1000));
                }
            }
        }else{
            WriteLog(VVServer.this,"VVServer：读取文件失败，文件不存在\n");
        }
        */
        
        //直接启动一个
//         if(isStop){
//             startTimer(false,new Date(wakeMainActivityTime),1000,1000);
//         }else{
//             stopTimer();    
//             startTimer(false,new Date(wakeMainActivityTime),1000,1000);
//         }
        //setForeground(); //未适配8.0的
        //setNotificationChannel("Vv小秘书");//适配8.0的
        
        //适配8.0并加入通知栏自定义布局的
        Notification notification = NotificationUtils.init(this,Meta.getResId(this, "drawable", "logo_32"),"com.limainfo.vv.Vv___");
        startForeground(1, notification);
        WriteLog(VVServer.this,"startForeground \n");
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
                .setContentTitle("Vv")
                /**设置通知的内容**/
                .setContentText("Vv小助手为您服务")
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
    
        
    private Notification notification;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    Intent intent;
    private void setNotificationChannel(String channel){
        String name = channel;//"通知渠道名称";//渠道名字
        String id = "my_package_channel_1"; // 渠道ID
        String description = "Vv小秘书为您服务，请勿关闭该通知"; // 渠道解释说明
        PendingIntent pendingIntent;//非紧急意图，可设置可不设置
        intent = new Intent(this, com.limainfo.vv.Vv___.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        
        if (notificationManager == null) {
            notificationManager =  (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        }
        //判断是否是8.0上设备
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = null;
            if (mChannel == null) {
                mChannel = new NotificationChannel(id, name, importance);
                mChannel.setDescription(description);
                mChannel.enableLights(true); //是否在桌面icon右上角展示小红点
                notificationManager.createNotificationChannel(mChannel);
            }
            notificationBuilder = new NotificationCompat.Builder(this);


            notificationBuilder
                    .setSmallIcon(Meta.getResId(this, "drawable", "logo_32"))
                    .setContentTitle("Vv小秘书")
                    .setContentText("Vv小秘书正在后台运行")
                    .setContentIntent(pendingIntent)
                    .setChannelId(id)
                    .setAutoCancel(true);
        }else{
            notificationBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(Meta.getResId(this, "drawable", "logo_32"))
                    .setContentTitle("Vv小秘书")
                    .setContentText("Vv小秘书正在后台运行")
                    .setAutoCancel(true);
        }
        notification = notificationBuilder.build();
        startForeground(1, notification);
    }
    
    private PowerManager.WakeLock wakeLock;
    private void WakeScreen(){
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        releaseWakeLock();
        if (Build.VERSION.SDK_INT < 20) {
            if(pm.isScreenOn()){
                return;
            }
        }
        
        int level = PowerManager.SCREEN_DIM_WAKE_LOCK |
                    PowerManager.ACQUIRE_CAUSES_WAKEUP;
        wakeLock = pm.newWakeLock(level, "Locationtion");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(1000);
    }
       
    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }
    
}
