package com.evollu.react.fcm;


import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;

import org.json.JSONArray;

import java.net.URLDecoder;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/*
    Referances:
    https://medium.com/@essamtarik2000/introduction-to-react-natives-headless-js-90e87dee0ab4
    https://github.com/mauron85/background-geolocation-android/blob/fb4f7a0d3175903eb9f6a3faf1332c46964849de/src/main/java/com/marianhello/bgloc/service/LocationServiceImpl.java#L408
    https://developerlife.com/2017/07/10/android-o-n-and-below-component-lifecycles-and-background-tasks/


 */
public class NotificationForeGroundServiceHJS extends Service {
    private boolean mIsInForeground = false;
	private int notifTime = 0;

    @Override
    public void onCreate() {
        // Log.d("MessagingService", "MessagingService ------------------- Y -1");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
			int notifStartedAt = intent.getIntExtra("notifTm", 0);
			// Log.d("MessagingService", "MessagingService ------------------- Y 0 : " + notifStartedAt +" : "+notifTime);
            if (intent.getBooleanExtra("stop", false)) {
                // Log.d("MessagingService", "MessagingService stop ------------------- Y 00");
                if (mIsInForeground && notifStartedAt!=0 && notifStartedAt == notifTime) {
					// Log.d("MessagingService", "MessagingService stop ------------------- 00Y 00");
                    stopForeground(true); // Added in API level 5 (This only removes the notification but does not stop the service)
                }
            } 
			else {
                // Log.d("MessagingService", "MessagingService ------------------- Y 000");
                Bundle bundle = intent.getBundleExtra("data");

                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //Added by Chandrajyoti
                    //When App is in killed state in above Android O(API 26), without creating channel notification shall not awake the application
                    //Also This creates the channel for local notification creation also above Android O(API 26) without this app will not show notification
                    NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                    NotificationChannel mChannel = new NotificationChannel("SilentChannel", "Default", NotificationManager.IMPORTANCE_HIGH); //This is a Silent Channel so no sound shall be set
                    mChannel.setDescription("Default Notifier");
                    mChannel.setShowBadge(false);
					mChannel.setSound(null,null); //This is a Silent Channel so no sound shall be set
                    mChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                    if (mNotificationManager != null) {
                        mNotificationManager.createNotificationChannel(mChannel);
                    }
                }

                String packageName = this.getPackageName();
                Intent launchIntent = this.getPackageManager().getLaunchIntentForPackage(packageName);
                String intentClassName = launchIntent != null ? launchIntent.getComponent().getClassName() : null;

                if (intentClassName == null) {
                    return START_NOT_STICKY;
                }

                Resources res = this.getResources();

                // Log.d("MessagingService", "MessagingService ------------------- Y  1");
                NotificationCompat.Builder notification = new NotificationCompat.Builder(this, "SilentChannel") //This is a Silent Channel so no sound shall be set
                        .setContentTitle("CleNet")
                        .setVibrate(null)
						.setSound(null); //This is a Silent Channel so no sound shall be set

				if(bundle.getString("channel", "").equals("compact")){
					notification.setContentText("Optimising Data...");
				}
				else{
					notification.setContentText("Retrieving Messages...");
				}

                // Log.d("MessagingService", "MessagingService ------------------- Y  3");

                if (bundle.containsKey("ongoing") && bundle.getBoolean("ongoing")) {
                    notification.setOngoing(bundle.getBoolean("ongoing"));
                }
                //priority
                switch (bundle.getString("priority", "")) {
                    case "min":
                        notification.setPriority(NotificationCompat.PRIORITY_MIN);
                        break;
                    case "high":
                        notification.setPriority(NotificationCompat.PRIORITY_HIGH);
                        break;
                    case "max":
                        notification.setPriority(NotificationCompat.PRIORITY_MAX);
                        break;
                    default:
                        notification.setPriority(NotificationCompat.PRIORITY_DEFAULT);
                }

                //icon
                String smallIcon = bundle.getString("icon", "ic_launcher");
                int smallIconResId = res.getIdentifier(smallIcon, "mipmap", packageName);
                if (smallIconResId == 0) {
                    smallIconResId = res.getIdentifier(smallIcon, "drawable", packageName);
                }
                if (smallIconResId != 0) {
                    notification.setSmallIcon(smallIconResId);
                }

                // Log.d("MessagingService", "MessagingService ------------------- Y  8");
                Intent newintent = new Intent();
                newintent.setClassName(this, intentClassName);
                newintent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                newintent.putExtras(bundle);

                String clickAction = bundle.getString("click_action");
                if (clickAction != null) clickAction = URLDecoder.decode(clickAction, "UTF-8");

                newintent.setAction(clickAction);

                int notificationID = bundle.containsKey("id") ? bundle.getString("id", "").hashCode() : (int) System.currentTimeMillis();

                PendingIntent pendingIntent = PendingIntent.getActivity(this, notificationID, newintent, PendingIntent.FLAG_UPDATE_CURRENT);

                notification.setContentIntent(pendingIntent);

                if (bundle.containsKey("android_actions")) {
                    String androidActions = bundle.getString("android_actions");
                    androidActions = URLDecoder.decode(androidActions, "UTF-8");

                    WritableArray actions = ReactNativeJson.convertJsonToArray(new JSONArray(androidActions));
                    for (int a = 0; a < actions.size(); a++) {
                        ReadableMap action = actions.getMap(a);
                        String actionTitle = action.getString("title");
                        String actionId = action.getString("id");
                        Intent actionIntent = new Intent();
                        actionIntent.setClassName(this, intentClassName);
                        actionIntent.setAction("com.evollu.react.fcm." + actionId + "_ACTION");
                        actionIntent.putExtras(bundle);
                        actionIntent.putExtra("_actionIdentifier", actionId);
                        actionIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        PendingIntent pendingActionIntent = PendingIntent.getActivity(this, notificationID, actionIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT);

                        notification.addAction(0, actionTitle, pendingActionIntent);
                    }
                }

                Notification info = notification.build();
                // Log.d("MessagingService", "MessagingService ------------------- Y  9");
 
				/*Note :
                    Cancelling foreground notfication with notficationmanager.cancel(notificationId) or notficationmanager.cancelAll() shall not work.
					Only calling stopForeground(true) or stopForeground(<foreground notification id>) shall work
				*/
                startForeground(1, info); // Added in API level 5 , 1st Argument(foreground notification id) should not be zero
                mIsInForeground = true;
				notifTime = (int) System.currentTimeMillis();
				bundle.putInt("notifTime", notifTime);
				HeadlessJsTaskService.acquireWakeLockNow(this.getApplicationContext());
				
                try {
                    Intent headlessIntent = new Intent(this.getApplicationContext(), HeadlessService.class);
                    headlessIntent.putExtras(bundle);
					
                    this.getApplicationContext().startService(headlessIntent);
                } catch (IllegalStateException ex) {
                    Log.e("MessagingService", "Background messages will only work if the message priority is set to 'high'", ex);
                }
            }
        } catch (Exception e) {
            Log.e("ReactNativeJS", "failed to send local notification", e);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}