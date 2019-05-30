package com.evollu.react.fcm;

import android.content.Intent;
import android.os.Bundle;

import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;
import com.google.firebase.messaging.RemoteMessage;
import com.facebook.react.bridge.Arguments;

import javax.annotation.Nullable;

import android.util.Log;

public class HeadlessService extends HeadlessJsTaskService {
    @Override
    protected @Nullable HeadlessJsTaskConfig getTaskConfig(Intent intent) {
        // Log.d("MessagingService", "MessagingService ------------------- Z  1");
        return new HeadlessJsTaskConfig(
			"CNBackgroundMessage", //JS task to run
			Arguments.fromBundle(intent.getExtras()), //data to be passed to JS task
			40000, // timeout for the task in ms
			false  // optional: defines whether or not  the task is allowed in foreground. Default is false
        );
    }
}
