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

import de.appplant.cordova.plugin.background.ForegroundService.ForegroundBinder;

import static android.content.Context.BIND_AUTO_CREATE;

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
    private boolean isOpenDebugModel = false;
  
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.mActivity = cordova.getActivity();
        this.mWebView = webView;
        if(isOpenDebugModel)
            Toast.makeText(cordova.getActivity(), "initialize", Toast.LENGTH_LONG).show();
        
    }
    
    @Override
    protected void pluginInitialize() {
        BackgroundExt.addWindowFlags(cordova.getActivity());
        if(isOpenDebugModel)
            Toast.makeText(cordova.getActivity(), "pluginInitialize", Toast.LENGTH_LONG).show();
        
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
        
        if(action.equalsIgnoreCase("GotoAutoStartManagerPage")){
            jumpStartInterface();
            callback.success();
            return true;
        }
        
        if(action.equalsIgnoreCase("StartJobServer")){
            StartJobServer();
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
            Toast.makeText(cordova.getActivity(),cordova.getActivity().getClass().getName(), Toast.LENGTH_LONG).show();
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
          
        if(action.equals("StartIPC")){
            if(MyJobService.isServiceWork(cordova.getActivity(),de.appplant.cordova.plugin.background.LocalCastielService)){
                Intent intent = new Intent(cordova.getActivity(), LocalCastielService.class);
                cordova.getActivity().startService(intent);
            }
            
            if(MyJobService.isServiceWork(cordova.getActivity(),de.appplant.cordova.plugin.background.RemoteCastielService)){
                Intent intent1 = new Intent(cordova.getActivity(), RemoteCastielService.class);
                cordova.getActivity().startService(intent1);
            }
           
            return true;
        }
       
        if (action.equals("BringToFrontBySetTime")) {     
            //获取到的秒数
            long time = Integer.parseInt(args.getString(0))*1000;      
            //当前时间的总秒数（相对于2010年的）
            long curTime = VVServer.getCurrentTime2Stamp();
            //设定的时间
            long setTime = curTime + time;
            
//             //存数据：
//             SharedPreferences alermTime = cordova.getActivity().getSharedPreferences("alermTime", 0);
//             alermTime.edit().putString("time",  String.valueOf(setTime)).commit();
                
            VVServer.initPropertiesFile(cordova.getActivity());
            VVServer.prop.put("time",String.valueOf(setTime));
            VVServer.prop.put("class",cordova.getActivity().getClass().getName());
            VVServer.saveConfig(cordova.getActivity(), "/data/data/" + cordova.getActivity().getPackageName()+ "/files/config.properties", VVServer.prop);
            return true;
        }
        
        if(action.equals("moveTaskToBack")){
            cordova.getActivity().moveTaskToBack(true);
            return true;
        }
        
        BackgroundExt.execute(this, action, callback);
        return true;
    }

    
    public void StartJobServer(){
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
