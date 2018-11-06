package de.appplant.cordova.plugin.background;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * Created by loi on 2018/5/31.
 */

public class NotificationUtils {
    public static NotificationManager mNotificationManager = null;
    public static NotificationChannel mNotificationChannel = null;//重要的通知渠道
    public static NotificationChannel mNotificationChannelDefault = null;//普通的通知渠道
    public static NotificationCompat.Builder mNotificationBuilder = null;
    private static Notification mNotification = null;
    public static PendingIntent mPendingIntent = null;//非紧急意图，可设置可不设置
    public static Intent mintent = null;
    public static RemoteViews bigContentView = null;
    public static boolean mIsSetContentView = true;//弃用

    public static String channelNameDefault = "Vv小助手默认渠道";//"通知渠道名称";//渠道名字
    public static String channelIdDefault = "channelId_default"; // 渠道ID
    public static String descriptionDefault = "用于不紧急通知"; // 渠道解释说明

    public static String channelNameOne = "Vv小助手重要通知渠道";//"通知渠道名称";//渠道名字
    public static String channelIdOne = "channelId_important"; // 渠道ID
    public static String descriptionOne = "用于紧急通知"; // 渠道解释说明

    public static Notification init(Context context,int icon,String activityName){
        if (mNotificationManager == null) {
            mNotificationManager =  (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        //设置默认意图
        try {
            mintent = new Intent(context, Class.forName(activityName));
            mintent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP| Intent.FLAG_ACTIVITY_NEW_TASK);
            mPendingIntent = PendingIntent.getActivity(context, 0, mintent, 0);
        } catch (ClassNotFoundException e) {
            VVServer.WriteLog(context, e.toString() + "\n");
            e.printStackTrace();
        }

        bigContentView = new RemoteViews(context.getPackageName(), Meta.getResId(context, "layout", "remote_layout"));
        bigContentView.setOnClickPendingIntent(Meta.getResId(context, "id", "button"), mPendingIntent);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance_high = NotificationManager.IMPORTANCE_HIGH;
            if (mNotificationChannel == null) {
                mNotificationChannel = new NotificationChannel(channelIdOne, channelNameOne, importance_high);
                mNotificationChannel.setDescription(descriptionOne);
                mNotificationChannel.enableLights(true); //是否在桌面icon右上角展示小红点
                mNotificationManager.createNotificationChannel(mNotificationChannel);
            }

            int importance_default = NotificationManager.IMPORTANCE_DEFAULT;
            if(mNotificationChannelDefault == null){
                mNotificationChannelDefault = new NotificationChannel(channelIdDefault, channelNameDefault, importance_default);
                mNotificationChannelDefault.setDescription(descriptionDefault);
                mNotificationChannelDefault.enableLights(true); //是否在桌面icon右上角展示小红点
                mNotificationManager.createNotificationChannel(mNotificationChannelDefault);
            }

            mNotificationBuilder = new NotificationCompat.Builder(context);
            mNotificationBuilder
                    .setSmallIcon(icon)
                    .setContentTitle("Vv小秘书_测试")
                    .setContentText("Vv小秘书正在后台运行")
                    .setContent(bigContentView)
                    .setContentIntent(mPendingIntent)
                    .setPriority(Notification.PRIORITY_MAX)//设置优先级,级别高的排在前面
                    .setChannelId(channelIdOne)//关键!!!!!!!!!!!
                    .setAutoCancel(false);
        }else{
            mNotificationBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(icon)
                    .setContentTitle("Vv小秘书_测试")
                    .setContentText("Vv小秘书正在后台运行")
                    .setContent(bigContentView)
                    .setContentIntent(mPendingIntent)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setAutoCancel(false);
        }
        mNotification = mNotificationBuilder.build();
        
        mNotification.flags =  Notification.FLAG_ONGOING_EVENT; 
        mNotification.defaults|=Notification.DEFAULT_SOUND;
        return mNotification;
    }


    /**
     * 设置文字
     * @param args1 首行文字
     * @param args2 次行文字
     */
    public static void upDataNotificationText(Context context,String  args1,String args2){
        VVServer.WriteLog(context, " upDataNotificationText---0");
        if(bigContentView!=null && mNotificationManager!=null){
            VVServer.WriteLog(context, " upDataNotificationText---1");
            if(args1!=null)
            {
                bigContentView.setTextViewText(Meta.getResId(context, "id", "textView_1"), args1);
                VVServer.WriteLog(context, " upDataNotificationText---2");
            }
            if(args2!=null)
            {
                bigContentView.setTextViewText(Meta.getResId(context, "id", "textView_2"), args2);
                VVServer.WriteLog(context, " upDataNotificationText---3");
            }
            if(args1!=null||args2!=null)
            {
                mNotificationManager.notify(1, mNotification);
                VVServer.WriteLog(context, " upDataNotificationText---4");
            }
        }
        VVServer.WriteLog(context, " upDataNotificationText---5");
    }

    /**
     *  更改
     * @param id 要更改的图标的Id
     * @param resId 资源文件的Id
     */
    public static void upDataNotificationIcon(int  id,int resId){
        if(bigContentView!=null&&mNotificationManager!=null){
                bigContentView.setImageViewResource(id, resId);
                mNotificationManager.notify(1, mNotification);
        }
    }

    /**
     *  设置通知栏按钮的意图
     * @param pendingIntent 意图
     */
    public static void setButtonIntent(Context context,PendingIntent pendingIntent){
        bigContentView.setOnClickPendingIntent(Meta.getResId(context, "id", "button"), pendingIntent);
        mNotificationManager.notify(1, mNotification);
    }

    /**
     * 发送一个通知
     * @param context
     * @param icon 图标 不可为空！！！
     * @param title 标题
     * @param content 内容
     * @param innerContent 子内容
     * @param notificationId 通知的ID
     * @param pendingIntent 点击这个通知的意图
     */
    public static void sendNotification(Context context,int importance,int icon, String title, String content,String innerContent, int notificationId,PendingIntent pendingIntent){
if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){//8.0及以上系统
            mNotificationBuilder = new NotificationCompat.Builder(context,channelIdDefault);
        }else {//8.0以下系统
            mNotificationBuilder = new NotificationCompat.Builder(context);
        }

        if(mNotificationBuilder!=null){
            mNotificationBuilder.setContentIntent(pendingIntent);
        }
        if(title!=null){
            mNotificationBuilder.setContentTitle(title);//必须设置
        }
        if(content!=null){
            mNotificationBuilder.setContentText(content);////必须设置
        }
        if(innerContent!=null){
            mNotificationBuilder.setSubText(innerContent);
        }
        if(icon == -1){
            return;
        }
        if(icon != -1){
            mNotificationBuilder.setSmallIcon(icon);//必须
        }
        mNotificationBuilder
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setAutoCancel(true)//设置可取消
                //设置通知时间
                .setWhen(System.currentTimeMillis())
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND);//设置默认的呼吸灯和震动;
        mNotificationManager.notify(notificationId,mNotificationBuilder.build());
    }
}
