package com.evollu.react.fcm;

import java.util.Map;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactContext;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import android.support.v4.content.ContextCompat; //Added by Chandrajyoti

// public class MessagingService extends FirebaseMessagingService { //Commented By Chandrajyoti
public class MessagingServiceCleNet extends FirebaseMessagingService {//MessagingService changed to MessagingServiceCleNet, so that the broadcast shall be only to CleNet even if some other app uses this fcm library

    private static final String TAG = "MessagingService";

    // public MessagingServiceCleNet() { //Added by chandrajyoti (not required) as per https://windows-hexerror.linestarve.com/q/so43571622-sending-push-notification-with-firebase-FATAL-EXCEPTION-android
    // super(); //This has to be empty else error shall be thrown while build
    // }

    @Override
    public void onNewToken(String token) { //Added by Chandrajyoti as FirebaseInstanceIdService (in InstanceIdServiceCleNet.java) depricated
        // Log.d(TAG, "onNewToken event received " + token);
        Intent i = new Intent("com.evollu.react.fcm.FCMRefreshTokenCleNet"); //FCMRefreshToken changed to FCMRefreshTokenCleNet, so that the broadcast shall be only to CleNet even if some other app uses this fcm library
        Bundle bundle = new Bundle();
        bundle.putString("token", token);
        i.putExtras(bundle);

        final Intent message = i;

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                // Construct and load our normal React JS code bundle
                ReactInstanceManager mReactInstanceManager = ((ReactApplication) getApplication()).getReactNativeHost().getReactInstanceManager();
                ReactContext context = mReactInstanceManager.getCurrentReactContext();
                // If it's constructed, send a notification
                if (context != null) {
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(message);
                } else {
                    // Otherwise wait for construction, then send the notification
                    mReactInstanceManager.addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
                        public void onReactContextInitialized(ReactContext context) {
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(message);
                        }
                    });
                    if (!mReactInstanceManager.hasStartedCreatingInitialContext()) {
                        // Construct it in the background
                        mReactInstanceManager.createReactContextInBackground();
                    }
                }
            }
        });
    }


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // super.onMessageReceived(remoteMessage); //Added & commented by Chandrajyoti
        // Log.d(TAG, "Remote message received");
        // Intent i = new Intent("com.evollu.react.fcm.ReceiveNotification");//Commented By Chandrajyoti
        // Intent i = new Intent("com.evollu.react.fcm.ReceiveNotificationCleNet"); //Commented By Chandrajyoti //ReceiveNotification changed to ReceiveNotificationCleNet, so that the broadcast shall be only to CleNet even if some other app uses this fcm library
        // i.putExtra("data", remoteMessage);//Commented By Chandrajyoti
        handleBadge(remoteMessage);
        if (FIRLocalMessagingHelper.isAppInForeground(this.getApplication())) { //Added by Chandrajyoti
            // Log.d(TAG, "MessagingService ------------------- x 0");
            buildLocalNotification(remoteMessage);
        } else { //Added by Chandrajyoti
            Intent serviceIntent = new Intent(this.getApplication(), NotificationForeGroundServiceHJS.class);
            try {
                // Log.d(TAG, "MessagingService ------------------- x1");
                Map<String, String> data = remoteMessage.getData();
                String customNotification = data.get("custom_notification");
                Bundle bundle = BundleJSONConverter.convertToBundle(new JSONObject(customNotification));
                serviceIntent.putExtra("data", bundle);
                ContextCompat.startForegroundService(this.getApplication(), serviceIntent);
                // Log.d(TAG, "MessagingService ------------------- x2");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // final Intent message = i; //Commented By Chandrajyoti

        // Log.d(TAG, "MessagingService ------------------- 1");
        /* Commented by Chandrajyoti
        // We need to run this on the main thread, as the React code assumes that is true.
        // Namely, DevServerHelper constructs a Handler() without a Looper, which triggers:
        // "Can't create handler inside thread that has not called Looper.prepare()"
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                // Construct and load our normal React JS code bundle
                ReactInstanceManager mReactInstanceManager = ((ReactApplication) getApplication()).getReactNativeHost().getReactInstanceManager();
                ReactContext context = mReactInstanceManager.getCurrentReactContext();
                // If it's constructed, send a notification
                if (context != null) {
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(message);
										Log.d(TAG, "MessagingService ------------------- 2");
                } else {
                    // Otherwise wait for construction, then send the notification
                    mReactInstanceManager.addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
                        public void onReactContextInitialized(ReactContext context) {
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(message);
														Log.d(TAG, "MessagingService ------------------- 3");
                        }
                    });
                    if (!mReactInstanceManager.hasStartedCreatingInitialContext()) {
                        // Construct it in the background
                        mReactInstanceManager.createReactContextInBackground();
												Log.d(TAG, "MessagingService ------------------- 4");
                    }
                }
            }
        });
	    */
    }

    public void handleBadge(RemoteMessage remoteMessage) {
        BadgeHelper badgeHelper = new BadgeHelper(this);
        if (remoteMessage.getData() == null) {
            return;
        }

        Map data = remoteMessage.getData();
        if (data.get("badge") == null) {
            return;
        }

        try {
            int badgeCount = Integer.parseInt((String) data.get("badge"));
            badgeHelper.setBadgeCount(badgeCount);
        } catch (Exception e) {
            Log.e(TAG, "Badge count needs to be an integer", e);
        }
    }

    public void buildLocalNotification(RemoteMessage remoteMessage) {
        // Log.d(TAG, "MessagingService ------------------- 5");
        if (remoteMessage.getData() == null) {
            return;
        }
        Map<String, String> data = remoteMessage.getData();
        String customNotification = data.get("custom_notification");
        if (customNotification != null) {
            try {
                Bundle bundle = BundleJSONConverter.convertToBundle(new JSONObject(customNotification));
                // bundle.putBoolean("isNewCnMsg", true);
                FIRLocalMessagingHelper helper = new FIRLocalMessagingHelper(this.getApplication());
                helper.sendNotification(bundle);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }
}
