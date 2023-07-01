package com.grupom2.wastemate.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.grupom2.wastemate.receiver.FilteredBroadcastReceiver;

import java.io.Serializable;

public class BroadcastUtil
{
    public static void sendLocalBroadcast(Context context, String action, Serializable data)
    {
        Intent intent = new Intent(action);
        Bundle bundle = new Bundle();
        if (data != null)
        {
            bundle.putSerializable("data", data);
            intent.putExtras(bundle);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static <T extends Serializable> T getData(Intent intent, Class<T> type)
    {
        Bundle extras = intent.getExtras();
        Serializable data = null;
        if (extras != null)
        {
            data = extras.getSerializable("data");
        }
        return (type.isInstance(data) ? type.cast(data) : null);
    }

    public static void registerLocalReceiver(Context context, BroadcastReceiver receiver, String... actions)
    {
        IntentFilter intentFilter = new IntentFilter();
        for (String action : actions)
        {
            intentFilter.addAction(action);
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, intentFilter);
    }

    public static void unregisterLocalReceiver(Context context, BroadcastReceiver receiver)
    {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
    }

    public static void registerReceiver(Context context, BroadcastReceiver receiver, String... actions)
    {
        context.registerReceiver(receiver, getIntentFilterForActions(actions));
    }

    public static void registerReceiver(Context context, FilteredBroadcastReceiver receiver)
    {
        context.registerReceiver(receiver, receiver.getIntentFilter());
    }

    public static void unregisterReceiver(Context context, BroadcastReceiver receiver)
    {
        context.unregisterReceiver(receiver);
    }

    public static IntentFilter getIntentFilterForActions(String... actions)
    {
        IntentFilter intentFilter = new IntentFilter();
        for (String action : actions)
        {
            intentFilter.addAction(action);
        }
        return intentFilter;
    }
}
