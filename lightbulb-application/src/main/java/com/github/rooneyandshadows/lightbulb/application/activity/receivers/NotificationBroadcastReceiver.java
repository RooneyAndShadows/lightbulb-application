package com.github.rooneyandshadows.lightbulb.application.activity.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.github.rooneyandshadows.lightbulb.application.activity.BaseActivity;

public class NotificationBroadcastReceiver extends BroadcastReceiver {
    private BaseActivity activity;

    public void setActivity(BaseActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String receivedString = intent.getAction();
        if (receivedString.equals("NOTIFICATION_RECEIVED_ACTION"))
            activity.notificationReceived();
    }
}