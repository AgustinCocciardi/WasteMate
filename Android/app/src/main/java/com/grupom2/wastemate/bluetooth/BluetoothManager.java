package com.grupom2.wastemate.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.google.gson.Gson;
import com.grupom2.wastemate.R;
import com.grupom2.wastemate.constant.Actions;
import com.grupom2.wastemate.constant.Constants;
import com.grupom2.wastemate.model.BluetoothDeviceData;
import com.grupom2.wastemate.util.BroadcastUtil;

import java.util.Set;
import java.util.UUID;

public class BluetoothManager
{
    private final BluetoothConnection bluetoothConnection;
    private final BluetoothPreferencesManager prefsManager;
    private final UUID commonUuid = UUID.fromString(Constants.COMMON_UUID_STRING);
    private final Context context;
    private boolean isConnected;

    public BluetoothManager(Context context)
    {
        bluetoothConnection = new BluetoothConnection(context);
        prefsManager = new BluetoothPreferencesManager(context);
        this.context = context;
        loadLastConnectedDevice();
    }

    public void connectToDevice(String deviceAddress)
    {
        if (isConnected)
        {
            disconnect();
        }
        bluetoothConnection.connectToDevice(deviceAddress, commonUuid);
        isConnected = true;
    }

    public void write(Object data)
    {
        Gson gson = new Gson();
        bluetoothConnection.write(gson.toJson(data));
    }

    public void disconnect()
    {
        if (isConnected)
        {
            bluetoothConnection.disconnect();
            isConnected = false;
        }
    }

    public void disconnectAndForget()
    {
        disconnect();
        BroadcastUtil.sendLocalBroadcast(context, Actions.LOCAL_ACTION_NO_DEVICE_CONNECTED, null);
    }

    public void saveLastConnectedDevice(String deviceAddress)
    {
        prefsManager.saveLastConnectedDevice(deviceAddress);
    }

    public void removeLastConnectedDevice()
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
            BroadcastUtil.sendLocalBroadcast(context, Actions.LOCAL_ACTION_NO_DEVICE_CONNECTED, null);
        }
    }

    public boolean isConnected()
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

    public Set<BluetoothDevice> getBondedDevices()
    {
        return bluetoothConnection.getBondedDevices();
    }

    public void startDiscovery()
    {
        bluetoothConnection.startDiscovery();
    }

    public String getConnectedDeviceAddress()
    {
        return bluetoothConnection.getDeviceAddress();
    }

    public boolean isEnabled()
    {
        return bluetoothConnection.isEnabled();
    }

    public BluetoothDeviceData getDeviceData()
    {
        return bluetoothConnection.getDeviceData();
    }

    public void setCalibrating()
    {
        bluetoothConnection.startCalibration();
    }

    public boolean isLastDeviceConnected(BluetoothDevice device)
    {
        return bluetoothConnection.isLastDeviceConnected(device);
    }
}


