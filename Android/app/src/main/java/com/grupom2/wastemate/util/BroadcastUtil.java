package com.grupom2.wastemate.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.grupom2.wastemate.receiver.FilteredBroadcastReceiver;
import com.grupom2.wastemate.receiver.SafeBroadcastReceiver;

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

    @Nullable
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

    public static void registerLocalReceiver(Context context, SafeBroadcastReceiver receiver, String... actions)
    {
        IntentFilter intentFilter = new IntentFilter();
        for (String action : actions)
        {
            intentFilter.addAction(action);
        }
        receiver.registerLocal(context, intentFilter);
    }

    public static void unregisterLocalReceiver(Context context, SafeBroadcastReceiver receiver)
    {
        receiver.unregisterLocal(context);
    }

    public static void registerReceiver(Context context, SafeBroadcastReceiver receiver, String... actions)
    {
        receiver.register(context, getIntentFilterForActions(actions));
    }

    public static void registerReceiver(Context context, FilteredBroadcastReceiver receiver)
    {
        receiver.register(context, receiver.getIntentFilter());
    }

    public static void unregisterReceiver(Context context, SafeBroadcastReceiver receiver)
    {
        receiver.unregister(context);
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
