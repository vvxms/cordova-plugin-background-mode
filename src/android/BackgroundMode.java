/*
    Copyright 2013-2017 appPlant GmbH
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at
     http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */

package de.appplant.cordova.plugin.background;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPreferences;

import java.util.Date;
import android.widget.Toast;
import android.content.Context;
import android.content.IntentFilter;
import android.app.PendingIntent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.util.Log;
import android.content.SharedPreferences;
import android.view.WindowManager;
import android.text.TextUtils;

import android.app.NotificationManager;
import android.os.PowerManager;

import de.appplant.cordova.plugin.background.ForegroundService.ForegroundBinder;
import com.tencent.bugly.crashreport.CrashReport;
import static android.content.Context.BIND_AUTO_CREATE;
import static android.content.Context.MODE_PRIVATE;

public class BackgroundMode extends CordovaPlugin {

    // Event types for callbacks
    private enum Event {
        ACTIVATE, DEACTIVATE, FAILURE
    }

    // Plugin namespace
    private static final String JS_NAMESPACE =
            "cordova.plugins.backgroundMode";

    // Flag indicates if the app is in background or foreground
    public static boolean inBackground = false;

    // Flag indicates if the plugin is enabled or disabled
    public static boolean isDisabled = true;

    // Flag indicates if the service is bind
    public static boolean isBind = false;

    // Default settings for the notification
    private static JSONObject defaultSettings = new JSONObject();

    // Service that keeps the app awake
    private ForegroundService service;
    
    // Used to (un)bind the service to with the activity
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ForegroundBinder binder = (ForegroundBinder) service;
            BackgroundMode.this.service = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            fireEvent(Event.FAILURE, "'service disconnected'");
        }
    };

    
    public static Activity mActivity;
    public static CordovaWebView mWebView;
    private int tempNotificationId = 2;//发送的通知的Id
    private static boolean isOpenDebugModel = false;
  
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.mActivity = cordova.getActivity();
        this.mWebView = webView;
        CrashReport.initCrashReport(this.cordova.getActivity().getApplicationContext());
        if(isOpenDebugModel)
            VVServer.WriteLog(cordova.getActivity(), " initialize");
        
    }
    
    @Override
    protected void pluginInitialize() {
        BackgroundExt.addWindowFlags(cordova.getActivity());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VVServer.WriteLog(cordova.getActivity(), " Android 8.0 startForegroundService");
            cordova.getActivity().startForegroundService(new Intent(cordova.getActivity(),VVServer.class));
        }else {
            VVServer.WriteLog(cordova.getActivity(), " 低版本启动服务");
            cordova.getActivity().startService(new Intent(cordova.getActivity(), VVServer.class));//程序启动的时候就启动vvservice服务    
        }
                
        if(!MyJobService.isServiceWork(cordova.getActivity(),"de.appplant.cordova.plugin.background.LocalCastielService")){        
            Intent intent = new Intent(cordova.getActivity(), LocalCastielService.class);
            cordova.getActivity().startService(intent);
        }
        if(!MyJobService.isServiceWork(cordova.getActivity(),"de.appplant.cordova.plugin.background.RemoteCastielService")){
            Intent intent1 = new Intent(cordova.getActivity(), RemoteCastielService.class);
            cordova.getActivity().startService(intent1);
        }     
        if(isOpenDebugModel)
            VVServer.WriteLog(cordova.getActivity(), " pluginInitialize");
    }

    // codebeat:disable[ABC]

    /**
     * Executes the request.
     *
     * @param action   The action to execute.
     * @param args     The exec() arguments.
     * @param callback The callback context used when
     *                 calling back into JavaScript.
     *
     * @return Returning false results in a "MethodNotFound" error.
     *
     * @throws JSONException
     */
    @Override
    public boolean execute (String action, JSONArray args,
                            CallbackContext callback) throws JSONException {

        if (action.equalsIgnoreCase("configure")) {
            configure(args.getJSONObject(0), args.getBoolean(1));
            callback.success();
            return true;
        }

        if (action.equalsIgnoreCase("enable")) {
            enableMode();
            callback.success();
            return true;
        }

        if (action.equalsIgnoreCase("disable")) {
            disableMode();
            callback.success();
            return true;
        }
        
           
        if(action.equalsIgnoreCase("ignoreBatteryOption")){
            isIgnoreBatteryOption(mActivity);//是否忽略电池优化
            return true;
        }
        
                   
        if(action.equalsIgnoreCase("launchAppMarketDetail")){
            //跳转到应用商店详情界面
            launchAppDetail(mActivity.getPackageName(),"");
            return true;
        }
        
                   
        if(action.equalsIgnoreCase("launchAppMarketSearch")){
            //跳转到应用商店搜索界面
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("market://search?q="+ mActivity.getPackageName()));
                mActivity.startActivity(i);
            } catch (Exception e) {
                Toast.makeText(mActivity, "您的手机没有安装Android应用市场", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
            return true;
        }
        
        //跳转到自启动界面
        if(action.equalsIgnoreCase("GotoAutoStartManagerPage")){
            jumpStartInterface();
            callback.success();
            return true;
        }
        
        if(action.equalsIgnoreCase("StartJobServer")){
            //暂时不需要jobserver
            callback.success();
            return true;
        }
        
        if(action.equalsIgnoreCase("StartOnPixelActivityWhenScreenOff")){
            registerScnOnAndOffBroadcast();
            callback.success();
            return true;
        }
        
        if(action.equalsIgnoreCase("StartVVSerivce")){
            Activity context = cordova.getActivity();
            Intent intent = new Intent(context, VVServer.class);
            context.startService(intent);
            callback.success();
            return true;
        }
        
        
        if (action.equals("BringToFront")) {
            if(isOpenDebugModel){
                Toast.makeText(cordova.getActivity(),cordova.getActivity().getClass().getName(), Toast.LENGTH_LONG).show();
            }
            Intent notificationIntent = new Intent(cordova.getActivity(), cordova.getActivity().getClass());
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(cordova.getActivity(), 0, notificationIntent, 0);
            try 
            {
              pendingIntent.send();
            }
            catch (PendingIntent.CanceledException e) 
            {
              e.printStackTrace();
            }
            return true;
        }
          
          
        if(action.equalsIgnoreCase("GetLog")){              
            SharedPreferences sharedPreferences = cordova.getActivity().getSharedPreferences("TimeFile", MODE_PRIVATE);
            if (sharedPreferences != null) {
                String log = sharedPreferences.getString("Log","");   
                callback.success(log);
            }        
            return true;
        } 
        
        if(action.equals("StartIPC")){
            if(!MyJobService.isServiceWork(cordova.getActivity(),"de.appplant.cordova.plugin.background.LocalCastielService")){
                Intent intent = new Intent(cordova.getActivity(), LocalCastielService.class);
                cordova.getActivity().startService(intent);
            }
            if(!MyJobService.isServiceWork(cordova.getActivity(),"de.appplant.cordova.plugin.background.RemoteCastielService")){
                Intent intent1 = new Intent(cordova.getActivity(), RemoteCastielService.class);
                cordova.getActivity().startService(intent1);
            }
            if(isOpenDebugModel){
                Toast.makeText(cordova.getActivity(),"StartIPC", Toast.LENGTH_LONG).show();
            }
            return true;
        }
       
        if (action.equals("BringToFrontBySetTime")) {
            if(args.getString(0).equals("")){
                VVServer.WriteLog(cordova.getActivity(), " 时间点设置为空!\n");
                return true;
            }
            //获取到的秒数
            long time = Integer.parseInt(args.getString(0))*1000; 
            VVServer.WriteLog(cordova.getActivity(), " 设定闹钟，设定的秒数:" + args.getString(0)+"\n");
            //使用JobService启动一个一次性任务
            //StartJobServer(Integer.parseInt( args.getString(0) ) );//有bug暂时不用
            //使用AlarmManager启动一个一次性任务
            alarm(cordova.getActivity(),Integer.parseInt( args.getString(0) ) );
            
            //当前时间的总秒数
            long curTime = System.currentTimeMillis();
            //设定的时间
            long setTime = curTime + time;
            SharedPreferences sharedPreferences = cordova.getActivity().getSharedPreferences("TimeFile", MODE_PRIVATE);
            if(sharedPreferences!=null){
                sharedPreferences.edit().putString("Time",String.valueOf(setTime)).commit();
            }   
            VVServer.wakeMainActivityTime  = setTime;
            if(isOpenDebugModel)           
            { 
                VVServer.WriteLog(cordova.getActivity(), "设定的秒数(毫秒)  " + String.valueOf(time) + "\n存储的时间 " + new Date(setTime).toString()+"\n");
            }
            return true;
        }
        
        if(action.equals("joinQQChatPage")){//加入到一个QQ聊天界面
            try {
                //跳转到添加好友，如果qq号是好友了，直接聊天
                String url = "mqqwpa://im/chat?chat_type=wpa&uin=" + args.getString(0);//uin是发送过去的qq号码
                mActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(mActivity,"请检查是否安装QQ",Toast.LENGTH_SHORT).show();
            }      
            return true;
        }
        
        if(action.equals("joinQQGroupPage")){
            Intent intent = new Intent();
            intent.setData(Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3D" + args.getString(0)));
            // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                mActivity.startActivity(intent);
                return true;
            } catch (Exception e) {
                // 未安装手Q或安装的版本不支持
                return false;
            }
        }
        
        if(action.equals("sendNotification")){
            VVServer.WriteLog(cordova.getActivity(), " 发送通知Start");
            Intent mintent = null;
            try {
                mintent = new Intent(mActivity, Class.forName("com.limainfo.vv.Vv___"));
            } catch (ClassNotFoundException e) {
                VVServer.WriteLog(cordova.getActivity(), " 发送通知错误"+e.toString());
                e.printStackTrace();
                return true;
            }
            mintent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP| Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent mPendingIntent = PendingIntent.getActivity(cordova.getActivity(), 0, mintent, 0);
            NotificationUtils.sendNotification(cordova.getActivity(), NotificationManager.IMPORTANCE_MAX, Meta.getResId(cordova.getActivity(), "drawable", "del_32px"),args.getString(0),args.getString(1),args.getString(2),tempNotificationId,mPendingIntent);
            tempNotificationId++;            
            VVServer.WriteLog(cordova.getActivity(), " 发送通知End");
            return true;
        }
                
        if(action.equals("setNotificationText")){
            VVServer.WriteLog(cordova.getActivity(), " 更改内容Start");
            if(args.getString(0).equals("") && !args.getString(1).equals("")){
                VVServer.WriteLog(cordova.getActivity(), " 更改内容Start--1");
                NotificationUtils.upDataNotificationText(cordova.getActivity(),null,args.getString(1));
            }else if(!args.getString(0).equals("") && args.getString(1).equals("")){
                VVServer.WriteLog(cordova.getActivity(), " 更改内容Start--2");
                NotificationUtils.upDataNotificationText(cordova.getActivity(),args.getString(0),null);
            }else if(!args.getString(0).equals("") && !args.getString(1).equals("")){
                VVServer.WriteLog(cordova.getActivity(), " 更改内容Start--3");
                NotificationUtils.upDataNotificationText(cordova.getActivity(),args.getString(0),args.getString(1));
            }
            VVServer.WriteLog(cordova.getActivity(), " 更改内容END");
            return true;
        }
        
        if(action.equals("setNotificationButtonClickIntent")){
            VVServer.WriteLog(cordova.getActivity(), " 更改按钮意图Start");
            try {
                Intent mintent = new Intent(mActivity, Class.forName("com.limainfo.vv.Vv___"));
                mintent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP| Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent mPendingIntent = PendingIntent.getActivity(cordova.getActivity(), 0, mintent, 0);
                NotificationUtils.setButtonIntent(cordova.getActivity(),mPendingIntent);
            } catch (ClassNotFoundException e) {
                VVServer.WriteLog(cordova.getActivity(), " 发送通知错误"+e.toString());
                e.printStackTrace();
            }      
            VVServer.WriteLog(cordova.getActivity(), " 更改按钮意图End");
            return true;
        }
        
        if(action.equals("moveTaskToBack")){
            cordova.getActivity().moveTaskToBack(true);
            return true;
        }
        
        if(action.equals("TestBugly")){
            Toast.makeText(cordova.getActivity(),"测试bugly",Toast.LENGTH_LONG).show();
            String message = "测试bugly";
            this.test(message, callback);
            return true;
        }
        
        BackgroundExt.execute(this, action, callback);
        return true;
    }
    
   private void test(String message, CallbackContext callbackContext) {
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException("This is a crash");
            }
        });
        callbackContext.success(message);
    }
    
        
    /**
     * 启动到应用商店app详情界面
     *
     * @param appPkg    目标App的包名
     * @param marketPkg 应用商店包名 ,如果为 "" 则由系统弹出应用商店列表供用户选择,否则调转到目标市场的应用详情界面，某些应用商店可能会失败
     */
    public void launchAppDetail(String appPkg, String marketPkg) {
        try {
            if (TextUtils.isEmpty(appPkg)) return;

            Uri uri = Uri.parse("market://details?id=" + appPkg);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if (!TextUtils.isEmpty(marketPkg)) {
                intent.setPackage(marketPkg);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            cordova.getActivity().startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
      
    public void alarm(Context context,int time){
        AlarmManager am = (AlarmManager) context.getSystemService(context.ALARM_SERVICE);
        Intent intent = new Intent(context, VVServer.class);
        intent.setAction(VVServer.ACTION_ALARM);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if(Build.VERSION.SDK_INT < 19){
            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + time*1000, pendingIntent);
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(System.currentTimeMillis() + time*1000, pendingIntent), pendingIntent);
            }else
            {
                am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + time*1000, pendingIntent);
            }
        }
    }
    
    public static int curJobInfoId = 1;
    public void StartJobServer(int time){
     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
              JobScheduler jobScheduler = (JobScheduler) cordova.getActivity().getSystemService("jobscheduler");
              JobInfo jobInfo = new JobInfo.Builder(curJobInfoId, new ComponentName(cordova.getActivity().getPackageName(), MyJobService.class.getName()))
                      .setMinimumLatency((time-5)*1000)
                      .setOverrideDeadline(time*1000)
                      .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                      .setPersisted(true)
                      .build();
      
         curJobInfoId++;
         int returnCode = jobScheduler.schedule(jobInfo);
         if(returnCode < 0){
             // do something when schedule goes wrong
             VVServer.WriteLog(cordova.getActivity(), " jobserver任务启动失败，错误码:" + returnCode +"\n");
         }else{
            VVServer.WriteLog(cordova.getActivity(), " jobserver任务启动成功\n");
         }
      }
    }
    
    public void StartJobServerBackUp(){
     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
              JobScheduler jobScheduler = (JobScheduler) cordova.getActivity().getSystemService("jobscheduler");
              JobInfo jobInfo = new JobInfo.Builder(1, new ComponentName(cordova.getActivity().getPackageName(), MyJobService.class.getName()))
                      .setPeriodic(10000)
                      .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                      .setPersisted(true)
                      .build();
              jobScheduler.schedule(jobInfo);
      }
    }
    
    public static OnePixelReceiver mOnepxReceiver;
    //注册监听屏幕的广播
    public void registerScnOnAndOffBroadcast(){
        mOnepxReceiver = new OnePixelReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.USER_PRESENT");
        cordova.getActivity().registerReceiver(mOnepxReceiver, intentFilter);
    }
    
          /**
     * Get Mobile Type
     *
     * @return
     */
    private static String getMobileType() {
        return Build.MANUFACTURER;
    }
    
    public void jumpStartInterface(){
        Intent intent = new Intent();
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Log.e("HLQ_Struggle", "******************当前手机型号为：" + getMobileType());
            ComponentName componentName = null;
            if (getMobileType().equals("Xiaomi")) { // 红米Note4测试通过
                componentName = new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
            } else if (getMobileType().equals("Letv")) { // 乐视2测试通过
                intent.setAction("com.letv.android.permissionautoboot");
            } else if (getMobileType().equals("samsung")) { // 三星Note5测试通过
                componentName = new ComponentName("com.samsung.android.sm_cn", "com.samsung.android.sm.ui.ram.AutoRunActivity");
            } else if (getMobileType().equals("HUAWEI")) { // 华为测试通过
                componentName = new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity");
            } else if (getMobileType().equals("vivo")) { // VIVO测试通过
                componentName = ComponentName.unflattenFromString("com.iqoo.secure/.MainActivity");//这个可以跳转到i管家
//                componentName = ComponentName.unflattenFromString("com.iqoo.secure/.safeguard.PurviewTabActivity");
            } else if (getMobileType().equals("Meizu")) { //万恶的魅族
//                 componentName = ComponentName.unflattenFromString("com.meizu.safe/.permission.PermissionMainActivity");
                componentName = ComponentName.unflattenFromString("com.meizu.safe/.permission.SmartBGActivity");
            } else if (getMobileType().equals("OPPO")) { // OPPO R8205测试通过
                componentName = ComponentName.unflattenFromString("com.oppo.safe/.permission.startup.StartupAppListActivity");
            } else if (getMobileType().equals("ulong")) { // 360手机 未测试
                componentName = new ComponentName("com.yulong.android.coolsafe", ".ui.activity.autorun.AutoRunListActivity");
            } else if(getMobileType().equals("nubia")){//中兴nubia z11Minis测试成功
                componentName = new ComponentName("cn.nubia.security2", "cn.nubia.security.appmanage.selfstart.ui.SelfStartActivity");
            }else if(getMobileType().equals("ZUK")){//联想zuk z2 pro测试通过
                componentName = new ComponentName("com.zui.safecenter", "com.lenovo.safecenter.MainTab.LeSafeMainActivity");
            }else {
                // 以上只是市面上主流机型，由于公司你懂的，所以很不容易才凑齐以上设备
                // 针对于其他设备，我们只能调整当前系统app查看详情界面
                // 在此根据用户手机当前版本跳转系统设置界面
                if (Build.VERSION.SDK_INT >= 9) {
                    intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                    intent.setData(Uri.fromParts("package", cordova.getActivity().getPackageName(), null));
                } else if (Build.VERSION.SDK_INT <= 8) {
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
                    intent.putExtra("com.android.settings.ApplicationPkgName", cordova.getActivity().getPackageName());
                }
            }
            intent.setComponent(componentName);
            cordova.getActivity().startActivity(intent);
        } catch (Exception e) {//抛出异常就直接打开设置页面
            intent = new Intent(Settings.ACTION_SETTINGS);
            cordova.getActivity().startActivity(intent);
        }
  }
    
    
    // codebeat:enable[ABC]

    /**
     * Called when the system is about to start resuming a previous activity.
     *
     * @param multitasking Flag indicating if multitasking is turned on for app.
     */
    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        inBackground = true;
        startService();
    }

    /**
     * Called when the activity will start interacting with the user.
     *
     * @param multitasking Flag indicating if multitasking is turned on for app.
     */
    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        inBackground = false;
        stopService();
    }

    /**
     * 针对N以上的Doze模式
     *
     * @param activity
     */
    public static void isIgnoreBatteryOption(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent();
                String packageName = activity.getPackageName();
                VVServer.WriteLog(activity, "包名: "+packageName);
                PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    //               intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    activity.startActivity(intent);
//                     activity.startActivityForResult(intent, REQUEST_IGNORE_BATTERY_CODE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
//     private static int REQUEST_IGNORE_BATTERY_CODE = 9527;
//     @Override
//     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//         if (resultCode == Activity.RESULT_OK) {
//             if (requestCode == REQUEST_IGNORE_BATTERY_CODE){
//                 //TODO something
//             }
//         }else if (resultCode == Activity.RESULT_CANCELED){
//             if (requestCode == REQUEST_IGNORE_BATTERY_CODE){
//                 Toast.makeText(cordova.getActivity(),"请开启忽略电池优化",Toast.LENGTH_SHORT).show();
//             }
//         }
//     }
    
    /**
     * Called when the activity will be destroyed.
     */
    @Override
    public void onDestroy() {
        if(isOpenDebugModel)
            Toast.makeText(cordova.getActivity(), "onDestroy" + VVServer.wakeMainActivityTime, Toast.LENGTH_LONG).show();
        stopService();
        super.onDestroy();
        //android.os.Process.killProcess(android.os.Process.myPid()); //为什么要这样写？
    }

    /**
     * Enable the background mode.
     */
    private void enableMode() {
        isDisabled = false;

        if (inBackground) {
            startService();
        }
    }

    /**
     * Disable the background mode.
     */
    private void disableMode() {
        stopService();
        isDisabled = true;
    }

    /**
     * Update the default settings and configure the notification.
     *
     * @param settings The settings
     * @param update A truthy value means to update the running service.
     */
    private void configure(JSONObject settings, boolean update) {
        if (update) {
            updateNotification(settings);
        } else {
            setDefaultSettings(settings);
        }
    }

    /**
     * Update the default settings for the notification.
     *
     * @param settings The new default settings
     */
    private void setDefaultSettings(JSONObject settings) {
        defaultSettings = settings;
    }

    /**
     * The settings for the new/updated notification.
     *
     * @return
     *      updateSettings if set or default settings
     */
    protected static JSONObject getSettings() {
        return defaultSettings;
    }

    /**
     * Update the notification.
     *
     * @param settings The config settings
     */
    private void updateNotification(JSONObject settings) {
        if (isBind) {
            service.updateNotification(settings);
        }
    }

    /**
     * Bind the activity to a background service and put them into foreground
     * state.
     */
    private void startService() {
        Activity context = cordova.getActivity();

        if (isDisabled || isBind)
            return;

        Intent intent = new Intent(context, ForegroundService.class);

        try {
            context.bindService(intent, connection, BIND_AUTO_CREATE);
            fireEvent(Event.ACTIVATE, null);
            context.startService(intent);
        } catch (Exception e) {
            fireEvent(Event.FAILURE, String.format("'%s'", e.getMessage()));
        }

        isBind = true;
    }
    
    /**
     * Bind the activity to a background service and put them into foreground
     * state.
     */
    private void stopService() {
        Activity context = cordova.getActivity();
        Intent intent    = new Intent(context, ForegroundService.class);

        if (!isBind)
            return;

        fireEvent(Event.DEACTIVATE, null);
        context.unbindService(connection);
        context.stopService(intent);

        isBind = false;
    }

    /**
     * Fire vent with some parameters inside the web view.
     *
     * @param event The name of the event
     * @param params Optional arguments for the event
     */
    private void fireEvent (Event event, String params) {
        String eventName = event.name().toLowerCase();
        Boolean active   = event == Event.ACTIVATE;

        String str = String.format("%s._setActive(%b)",
                JS_NAMESPACE, active);

        
        str = String.format("%s;%s.on%s(%s)",
                str, JS_NAMESPACE, eventName, params);
        

        str = String.format("%s;%s.fireEvent('%s',%s);",
                str, JS_NAMESPACE, eventName, params);
        

        final String js = str;

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl("javascript:" + js);
            }
        });
    }

}
