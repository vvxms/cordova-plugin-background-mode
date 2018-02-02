package de.appplant.cordova.plugin.background;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;




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


public class LocalCastielService extends Service {

    MyBinder myBinder;
    private PendingIntent pintent;
    MyServiceConnection myServiceConnection;
    private int i = 0;
    private String errorStr = "";
    Class<?> mClass;
    private String testLog = "";
    
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("LocalCastielService", "onCreate");
        if (myBinder == null) {
            myBinder = new MyBinder();
        }
        myServiceConnection = new MyServiceConnection();  
    }
    
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
                    Toast.makeText(VVServer.this,"LocalCastielService"+testLog,Toast.LENGTH_SHORT).show();
//                     Intent notificationIntent;
//                     if(mClass!=null){
//                         notificationIntent = new Intent(LocalCastielService.this, mClass);
//                     }else{
//                             Toast.makeText(LocalCastielService.this,"LocalCastielService-无法获取activity类名",Toast.LENGTH_SHORT).show();
//                             return;
//                     }
//                     notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP |Intent.FLAG_ACTIVITY_NEW_TASK);
//                     PendingIntent pendingIntent = PendingIntent.getActivity(LocalCastielService.this, 0, notificationIntent, 0);
//                     try 
//                     {
//                       pendingIntent.send();
//                     }
//                     catch (PendingIntent.CanceledException e) 
//                     {
//                       e.printStackTrace();
//                     }
            LocalCastielService.this.startService(new Intent(LocalCastielService.this, VVServer.class));
            
//             Toast.makeText(LocalCastielService.this, "LocalCastielService: "+String.valueOf(msg.what)+ errorStr, Toast.LENGTH_SHORT).show();
            return true;
        }
    });

    /**
     * 显示一个普通的通知
     *
     * @param context 上下文
     */
    public void showNotification(Context context,int startId) {
        Log.e("LocalCastielService", "显示一个普通的通知");
        Notification notification = new NotificationCompat.Builder(context)
                /**通知首次出现在通知栏，带上升动画效果的**/
                .setTicker("保活服务1")
                /**设置通知的标题**/
                .setContentTitle("保活服务")
                /**设置通知的内容**/
                .setContentText("点击跳转到MainActivity")
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
        Log.e("LocalCastielService","notifyId"+String.valueOf(startId));
        startForeground(startId, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.bindService(new Intent(this, RemoteCastielService.class), myServiceConnection, Context.BIND_IMPORTANT);
        Log.e("LocalCastielService", "绑定RemoteCastielService服务");
        showNotification(this,startId );
         
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(!MyJobService.isServiceWork(LocalCastielService.this,"de.appplant.cordova.plugin.background.VVServer")){                                  
                    Message message = new Message(); 
                    message.what = i;
                    handler.sendMessage(message); 
                }
            }
        }, 0, 900000);//15分钟检测一次
        
//         Timer timer1 = new Timer();
//         timer1.schedule(new TimerTask() {
//             @Override
//             public void run() {
//                   //读数据
//                 if(prop==null){     
//                     initPropertiesFile(LocalCastielService.this);
//                 }

//                 try {
//                     mClass = Class.forName(prop.get("class").toString());
//                     if(mClass != null){
//                         testLog = prop.get("class").toString();
//                     }else{
//                         testLog = "获取包名失败";
//                     }
//                 } catch (ClassNotFoundException e) 
//                 {    
//                     testLog = e.toString();
//                     e.printStackTrace();
//                 }              
      
//                 Message message = new Message();
//                 message.what = 1;  
//                 handler.sendMessage(message);
//             }
//         }, 10000, 10000);
        
        return START_STICKY;
    }
  
    public  class MyServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            Log.e("LocalCastielService", "远程服务连接成功");
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.e("LocalCastielService", "远程服务Remote被干掉");
            // 连接出现了异常断开了，RemoteService被杀掉了
//             Toast.makeText(LocalCastielService.this, "远程服务Remote被干掉", Toast.LENGTH_LONG).show();
            // 启动RemoteCastielService
            LocalCastielService.this.startService(new Intent(LocalCastielService.this, RemoteCastielService.class));
            LocalCastielService.this.bindService(new Intent(LocalCastielService.this, RemoteCastielService.class),
                    myServiceConnection, Context.BIND_IMPORTANT);
        }

    }

    static class MyBinder extends CastielProgressConnection.Stub {
        @Override
        public String getProName() throws RemoteException {
            return "Local";
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return myBinder;
    }

    @Override
    public void onDestroy() {
        this.unbindService(myServiceConnection);
        super.onDestroy();
    }
    
    
        
    public static Properties prop;
    public static void initPropertiesFile(Context context) {
        prop = loadConfig(context, "/data/data/" + context.getPackageName()+ "/config.properties");
        Toast.makeText(context,"LocalCastielService路径" + "/data/data/" + context.getPackageName()+ "/config.properties",Toast.LENGTH_LONG).show();
        if (prop == null) {
            // 配置文件不存在的时候创建配置文件 初始化配置信息
            Toast.makeText(context,"LocalCastielService配置文件新建了",Toast.LENGTH_LONG).show();
       
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
