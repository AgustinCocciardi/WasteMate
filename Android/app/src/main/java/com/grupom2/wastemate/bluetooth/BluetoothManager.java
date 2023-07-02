package com.grupom2.wastemate.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresPermission;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.grupom2.wastemate.R;
import com.grupom2.wastemate.constant.Actions;
import com.grupom2.wastemate.constant.Constants;
import com.grupom2.wastemate.util.BroadcastUtil;

import java.util.Set;
import java.util.UUID;

public class BluetoothManager
{
    private final BluetoothConnection bluetoothConnection;
    private final BluetoothPreferencesManager prefsManager;
    private final UUID commonUuid = UUID.fromString(Constants.COMMON_UUID_STRING);

    //region Auto Reconnect
    private boolean autoReconnect = true;
    private boolean isDisconnectExplicit = false;
    private static final long RECONNECT_DELAY = 3000; // 3 seconds
    private Context context;
    private Handler handler;

    private BroadcastReceiver deviceConnectedBroadcastReceiver;//TODO VER SI ANDA
    //endregion

    public BluetoothManager(Context context)
    {
        bluetoothConnection = new BluetoothConnection(context);
        prefsManager = new BluetoothPreferencesManager(context);
        deviceConnectedBroadcastReceiver = new DeviceConnectedBroadcastReceiver();
        this.context = context;
        BroadcastUtil.registerReceiver(context, deviceConnectedBroadcastReceiver, Actions.ACTION_ACK); //TODO VER SI ANDA
        loadLastConnectedDevice();
    }

    public void connectToDevice(String deviceAddress)
    {
        disconnect();
        autoReconnect = true;
        bluetoothConnection.connectToDevice(deviceAddress, commonUuid);
        startConnectionStatusMonitoring();
    }

    public void write(Object data)
    {
        Gson gson = new Gson();
        bluetoothConnection.write(gson.toJson(data));
    }

    public void disconnect()
    {
        stopConnectionStatusMonitoring();
        BroadcastUtil.unregisterReceiver(context, deviceConnectedBroadcastReceiver);//TODO VER SI ANDA
        bluetoothConnection.disconnect();
        isDisconnectExplicit = true;
    }

    public void disconnectAndForget()
    {
        disconnect();
        BroadcastUtil.sendLocalBroadcast(context, Actions.ACTION_NO_DEVICE_CONNECTED, null);
        removeLastConnectedDevice();
    }

    private void saveLastConnectedDevice(String deviceAddress)
    {
        prefsManager.saveLastConnectedDevice(deviceAddress);
    }

    private void removeLastConnectedDevice()
    {
        prefsManager.removeLastConnectedDevice();
    }

    private void loadLastConnectedDevice()
    {
        String deviceAddress = prefsManager.loadLastConnectedDevice();
        if (deviceAddress != null)
        {
            connectToDevice(deviceAddress);
        }
        else
        {
            BroadcastUtil.sendLocalBroadcast(context, Actions.ACTION_NO_DEVICE_CONNECTED, null);
        }
    }

    private void handleConnectionError()
    {
        if (autoReconnect && !isDisconnectExplicit)
        {
            reconnect();
        }
    }

    private void reconnect()
    {
        String deviceAddress = prefsManager.loadLastConnectedDevice();
        if (deviceAddress != null)
        {
            connectToDevice(deviceAddress); // Replace YOUR_UUID with your desired UUID
        }
    }

    private void startConnectionStatusMonitoring()
    {
        handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                if (isConnected())
                {
                    // Connection is active, continue monitoring
                    handler.postDelayed(this, RECONNECT_DELAY);
                }
                else
                {
                    // Connection is dropped, attempt reconnection
                    handleConnectionError();
                }
            }
        }, RECONNECT_DELAY);
    }

    private void stopConnectionStatusMonitoring()
    {
        if (handler != null)
        {
            handler.removeCallbacksAndMessages(null);
        }
    }

    private boolean isConnected()
    {
        return bluetoothConnection.isConnected();
    }

    public BluetoothConnection getBluetoothConnection()
    {

        return bluetoothConnection;
    }

    public String getConnectedDeviceName()
    {
        String deviceName;
        if (bluetoothConnection.isConnected())
        {
            deviceName = bluetoothConnection.getDeviceName();
        }
        else
        {
            deviceName = context.getResources().getString(R.string.status_not_connected);
        }
        return deviceName;
    }

    @RequiresPermission(anyOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH})
    public Set<BluetoothDevice> getBondedDevices()
    {
        return bluetoothConnection.getBondedDevices();
    }

    @RequiresPermission(anyOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN})
    public void startDiscovery()
    {
        bluetoothConnection.startDiscovery();
    }

    public void refresh()
    {
        bluetoothConnection.refreshAdapter();
    }

    public String getConnectedDeviceAddress()
    {
        return bluetoothConnection.getDeviceAddress();
    }

    private class DeviceConnectedBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            saveLastConnectedDevice(bluetoothConnection.getDeviceAddress()); //TODO: VALIDAR SI ANDA.
        }
    }
}


