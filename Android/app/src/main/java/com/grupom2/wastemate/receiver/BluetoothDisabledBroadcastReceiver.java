package com.grupom2.wastemate.receiver;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;

import com.grupom2.wastemate.util.NavigationUtil;

public class BluetoothDisabledBroadcastReceiver extends FilteredBroadcastReceiver
{

    public BluetoothDisabledBroadcastReceiver()
    {
        setFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();
        if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
        {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (state == BluetoothAdapter.STATE_OFF)
            {
                NavigationUtil.navigateToBluetoothRequiredActivity(context);
            }
        }
    }
}

