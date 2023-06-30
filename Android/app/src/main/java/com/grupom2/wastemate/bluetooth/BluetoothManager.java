package com.grupom2.wastemate.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.grupom2.wastemate.constant.Actions;
import com.grupom2.wastemate.util.BroadcastUtil;

import java.util.UUID;

public class BluetoothManager
{
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothConnection bluetoothConnection;
    private BluetoothPreferencesManager prefsManager;
    private UUID YOUR_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //region Auto Reconnect
    private boolean autoReconnect = true;
    private boolean isDisconnectExplicit = false;
    private static final long RECONNECT_DELAY = 3000; // 3 seconds
    private Context context;
    private Handler handler;
    //endregion


    public BluetoothManager(Context context)
    {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothConnection = new BluetoothConnection(context);
        prefsManager = new BluetoothPreferencesManager(context);
        this.context = context;
        loadLastConnectedDevice();
    }

    public void connectToDevice(String deviceAddress)
    {
        disconnect();
        autoReconnect = true;
        bluetoothConnection.connectToDevice(deviceAddress, YOUR_UUID);
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
}


