package com.grupom2.wastemate.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public abstract class SafeBroadcastReceiver extends BroadcastReceiver
{
    public boolean isRegistered;

    public Intent register(Context context, IntentFilter filter)
    {
        try
        {
            return !isRegistered
                    ? context.registerReceiver(this, filter)
                    : null;
        }
        finally
        {
            isRegistered = true;
        }
    }

    public void registerLocal(Context context, IntentFilter filter)
    {
        try
        {
            if (!isRegistered)
            {
                LocalBroadcastManager.getInstance(context).registerReceiver(this, filter);
            }
        }
        finally
        {
            isRegistered = true;
        }
    }

    public boolean unregister(Context context)
    {
        return isRegistered && unregisterInternal(context);
    }

    private boolean unregisterInternal(Context context)
    {
        context.unregisterReceiver(this);
        isRegistered = false;
        return true;
    }

    private boolean unregisterLocalInternal(Context context)
    {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
        isRegistered = false;
        return true;
    }

    public boolean unregisterLocal(Context context)
    {
        return unregisterLocalInternal(context);
    }
}
