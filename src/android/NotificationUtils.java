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
    public static NotificationChannel mNotificationChannel = null;
    public static NotificationCompat.Builder mNotificationBuilder = null;
    private static Notification mNotification = null;
    public static PendingIntent mPendingIntent = null;//非紧急意图，可设置可不设置
    public static Intent mintent = null;

    public static RemoteViews bigContentView = null;
    public static boolean mIsSetContentView = true;


    public static String channelNameDefault = "Vv_channel_default";//"通知渠道名称";//渠道名字
    public static String channelIdDefault = "Vv_channelId"; // 渠道ID
    public static String descriptionDefault = "Vv小秘书通知渠道_默认"; // 渠道解释说明

    public static String channelNameOne = "Vv小秘书";//"通知渠道名称";//渠道名字
    public static String channelIdOne = "Vv_channelId_Default"; // 渠道ID
    public static String description = "Vv小秘书通知渠道"; // 渠道解释说明

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance_default = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel cNotificationChannel = new NotificationChannel(channelIdDefault, channelNameDefault, importance_default);
            cNotificationChannel.setDescription(descriptionDefault);
            cNotificationChannel.enableLights(false); //是否在桌面icon右上角展示小红点
            mNotificationManager.createNotificationChannel(cNotificationChannel);

            int importance = NotificationManager.IMPORTANCE_HIGH;
            if (mNotificationChannel == null) {
                mNotificationChannel = new NotificationChannel(channelIdOne, channelNameOne, importance);
                mNotificationChannel.setDescription(description);
                mNotificationChannel.enableLights(true); //是否在桌面icon右上角展示小红点
                mNotificationManager.createNotificationChannel(mNotificationChannel);
            }
            mNotificationBuilder = new NotificationCompat.Builder(context);
            mNotificationBuilder
                    .setSmallIcon(icon)
                    .setContentTitle("Vv小秘书")
                    .setContentText("Vv小秘书正在后台运行")
                    .setContentIntent(mPendingIntent)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setChannelId(channelIdDefault)//关键!!!!!!!!!!!
                    .setAutoCancel(false);
            
            VVServer.WriteLog(context, "Android O chanel" + "\n");
        }else{
            mNotificationBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(icon)
                    .setContentTitle("Vv小秘书")
                    .setContentText("Vv小秘书正在后台运行")
                    .setPriority(Notification.PRIORITY_MAX)//在通知栏顶部显示
                    .setAutoCancel(false);
            VVServer.WriteLog(context, "Normal Android Sdk chanel" + "\n");
        }
        mNotification = mNotificationBuilder.build();
        
        mNotification.flags =  Notification.FLAG_ONGOING_EVENT;
                
        RemoteViews contentView = new RemoteViews(context.getPackageName(), Meta.getResId(context, "layout", "content_view"));
        contentView.setTextViewText(Meta.getResId(context, "id", "textview"), "Vv小助手");
        mNotification.contentView = contentView;

        //自定义bigContentView
        if (Build.VERSION.SDK_INT >= 16 && mIsSetContentView) {
            bigContentView = new RemoteViews(context.getPackageName(), Meta.getResId(context, "layout", "remote_layout"));
//                    - setTextViewText(viewId, text)                     设置文本
//                    - setTextColor(viewId, color)                       设置文本颜色
//                    - setTextViewTextSize(viewId, units, size)          设置文本大小
//                    - setImageViewBitmap(viewId, bitmap)                设置图片
//                    - setImageViewResource(viewId, srcId)               根据图片资源设置图片
//                    - setViewPadding(viewId, left, top, right, bottom)  设置Padding间距
//                    - setOnClickPendingIntent(viewId, mPendingIntent)    设置点击事件
            bigContentView.setOnClickPendingIntent(Meta.getResId(context, "id", "button"), mPendingIntent);
            mNotification.bigContentView = bigContentView;
        }
        //mNotificationManager.notify(0,mNotification);
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
            mNotificationChannel = mNotificationManager.getNotificationChannel(channelIdOne);
            mNotificationChannel.setImportance(importance);
            if(mNotificationChannel.getImportance() == NotificationManager.IMPORTANCE_NONE){
                Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE,context.getPackageName());
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, mNotificationChannel.getId());
                context.startActivity(intent);//跳转到打开通知界面
                Toast.makeText(context,"请手动将通知打开!",Toast.LENGTH_SHORT).show();
            }
            mNotificationBuilder = new NotificationCompat.Builder(context,channelIdOne);
        }else {//8.0以下系统
            mNotificationBuilder = new NotificationCompat.Builder(context);
        }

        if(mNotificationBuilder!=null){
            mNotificationBuilder.setContentIntent(pendingIntent);
        }
        if(title!=null){
            mNotificationBuilder.setContentTitle(title);
        }
        if(content!=null){
            mNotificationBuilder.setContentText(content);
        }
        if(innerContent!=null){
            mNotificationBuilder.setSubText(innerContent);
        }
        if(icon == -1){
            return;
        }
        mNotificationBuilder
                .setSmallIcon(icon)
                .setPriority(Notification.PRIORITY_MAX)
                .setAutoCancel(true)//设置可取消
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE);//设置默认的呼吸灯和震动;
        Notification notification = mNotificationBuilder.build();
        mNotificationManager.notify(notificationId,notification);
    }
}
