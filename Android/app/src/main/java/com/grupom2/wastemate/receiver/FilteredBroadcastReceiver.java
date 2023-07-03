package com.grupom2.wastemate.receiver;

import android.content.IntentFilter;

import com.grupom2.wastemate.util.BroadcastUtil;

public abstract class FilteredBroadcastReceiver extends SafeBroadcastReceiver
{
    protected IntentFilter intentFilter;

    protected void setFilter(String... actions)
    {
        intentFilter = BroadcastUtil.getIntentFilterForActions(actions);
    }

    public IntentFilter getIntentFilter()
    {
        return intentFilter;
    }
}

