package com.grupom2.wastemate.bluetooth;

import android.content.Context;

import com.grupom2.wastemate.util.SharedPreferencesManager;

public class BluetoothPreferencesManager
{
    private static final String PREFS_NAME = "BluetoothPrefs";
    private static final String KEY_LAST_CONNECTED_DEVICE = "LastConnectedDevice";
    private SharedPreferencesManager sharedPreferencesManager;

    public BluetoothPreferencesManager(Context context) {
        sharedPreferencesManager = SharedPreferencesManager.getInstance(context, PREFS_NAME);
    }

    public void saveLastConnectedDevice(String deviceAddress) {
        sharedPreferencesManager.saveString(KEY_LAST_CONNECTED_DEVICE, deviceAddress);
    }

    public String loadLastConnectedDevice() {
        return sharedPreferencesManager.loadString(KEY_LAST_CONNECTED_DEVICE, null);
    }

    public void removeLastConnectedDevice()
    {
        sharedPreferencesManager.removeKey(KEY_LAST_CONNECTED_DEVICE);
    }
}

