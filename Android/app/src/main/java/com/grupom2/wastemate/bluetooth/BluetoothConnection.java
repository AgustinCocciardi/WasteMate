package com.grupom2.wastemate.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.grupom2.wastemate.constant.Actions;
import com.grupom2.wastemate.constant.Constants;
import com.grupom2.wastemate.model.BluetoothMessage;
import com.grupom2.wastemate.model.BluetoothMessageResponse;
import com.grupom2.wastemate.util.BroadcastUtil;
import com.grupom2.wastemate.util.NavigationUtil;
import com.grupom2.wastemate.util.PermissionHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

public class BluetoothConnection
{
    private final Object lock = new Object();
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;
    private ReadThread readThread;
    private ConnectThread connectThread;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private Context context;

    public BluetoothConnection(Context context)
    {
        this.context = context;
    }

    public void connectToDevice(String deviceAddress, UUID uuid)
    {
        synchronized (lock)
        {
            disconnect();
            connectThread = new ConnectThread(deviceAddress, uuid);
            connectThread.start();
        }
    }

    public void write(String data)
    {
        synchronized (lock)
        {
            byte[] msgBuffer = data.getBytes();

            if (bluetoothSocket != null && bluetoothSocket.isConnected())
            {
                Thread writeThread = new Thread(() ->
                {
                    try
                    {
                        OutputStream outputStream = bluetoothSocket.getOutputStream();
                        outputStream.write(msgBuffer);
                        outputStream.flush();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        //TODO: MANAGE EXCEPTION
                    }
                });

                writeThread.start();
            }
        }
    }

    public void disconnect()
    {
        synchronized (lock)
        {
            if (bluetoothSocket != null)
            {
                try
                {
                    bluetoothSocket.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    //TODO: MANAGE EXCEPTION
                }
                bluetoothSocket = null;
            }
            if (connectThread != null)
            {
                connectThread.cancel();
                try
                {
                    connectThread.join();
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException(e);
                    //TODO: Manage exception
                }
                connectThread = null;
            }

            if (readThread != null)
            {
                readThread.cancel();
                try
                {
                    readThread.join();
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException(e);
                    //TODO: MANAGE EXCEPTION
                }
                readThread = null;
            }

        }
    }

    public boolean isConnected()
    {
        synchronized (lock)
        {
            return bluetoothSocket != null && bluetoothSocket.isConnected();
        }
    }

    private boolean read(BluetoothMessageResponse response)
    {
        boolean read;
        synchronized (lock)
        {
            try
            {
                InputStream inputStream = bluetoothSocket.getInputStream();
                if (inputStream.available() > 0)
                {
                    byte[] buffer = new byte[256];
                    int bytes = inputStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    response = BluetoothMessageResponse.fromJson(readMessage);
                    read = true;
                }
                else
                {
                    read = false;
                }
            }
            catch (IOException e)
            {
                read = false;
            }
        }
        return read;
    }

    public String getDeviceAddress()
    {
        return bluetoothDevice != null ? bluetoothDevice.getAddress() : null;
    }

    public BluetoothAdapter getAdapter()
    {
        return bluetoothAdapter;
    }

    private class ConnectThread extends Thread
    {
        ConnectionHandler handler;
        private String deviceAddress;
        private UUID uuid;

        public ConnectThread(String deviceAddress, UUID uuid)
        {
            this.deviceAddress = deviceAddress;
            this.uuid = uuid;
        }

        @Override
        public void run()
        {
            synchronized (lock)
            {
                try
                {
                    if (PermissionHelper.isPermissionGranted(context, Manifest.permission.BLUETOOTH_CONNECT))
                    {
                        bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
                        //TODO: VALIDAR SI LA VERIFICACION DE PERMISOS ANDA. SI ANDA, SUPRIMIR WARNING.
                        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                        bluetoothSocket.connect();
                        write(new BluetoothMessage(Constants.CODE_CONNECTION_REQUESTED).Serialize());
                        handler = new ConnectionHandler();
                        handler.sendEmptyMessage(0);
                    }
                    else
                    {
                        NavigationUtil.navigateToMissingPermissionsActivity(context, new ArrayList<String>()
                        {{
                            add(Manifest.permission.BLUETOOTH_CONNECT);
                        }});
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    //TODO: MANAGE EXCEPTION  - CONNECION FAILED.
                }
            }
        }

        public void cancel()
        {
            handler.cancel();
        }
    }

    private class ConnectionHandler extends Handler
    {
        private static final int MAX_CONNECTION_ATTEMPTS = 5;
        private static final int CONNECTION_INTERVAL = 1000;
        private BluetoothMessageResponse response;

        private boolean ackReceived;
        private int connectionAttempts;
        private boolean isCanceled;

        public ConnectionHandler()
        {
            super(Looper.getMainLooper());
            isCanceled = false;
        }

        @Override
        public void handleMessage(android.os.Message msg)
        {
            //TODO: improve this.
            Gson gson = new Gson();
            if (isCanceled)
            {
                BroadcastUtil.sendLocalBroadcast(context, Actions.ACTION_CONNECTION_CANCELED, null);
            }
            if (ackReceived)
            {
                BroadcastUtil.sendLocalBroadcast(context, Actions.ACTION_ACK, gson.toJson(response));
                removeCallbacksAndMessages(null);
                return;
            }
            if (connectionAttempts >= MAX_CONNECTION_ATTEMPTS)
            {
                BroadcastUtil.sendLocalBroadcast(context, Actions.ACTION_UNSUPPORTED_DEVICE, gson.toJson(response));
                disconnect();
                removeCallbacksAndMessages(null);
                return;
            }

            if (isConnected())
            {
                try
                {
                    if (bluetoothSocket.getInputStream().available() > 0)
                    {
                        boolean read = read(response);
                        if (read && response != null && Objects.equals(response.getCode(), Constants.CODE_ACK))
                        {
                            ackReceived = true;
                            readThread = new ReadThread();
                            readThread.start();
                        }
                    }
                }
                catch (IOException e)
                {
                    //Se ignora la excepción. Se reintenta establecer la conexión.
                }
            }

            if (!ackReceived)
            {
                connectionAttempts++;
                sendEmptyMessageDelayed(0, CONNECTION_INTERVAL);
            }
        }


        public void cancel()
        {
            isCanceled = true;
        }
    }

    private class ReadThread extends Thread
    {
        private InputStream inputStream;
        private boolean isRunning;

        public ReadThread()
        {
            try
            {
                inputStream = bluetoothSocket.getInputStream();
                isRunning = true;
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        public void run()
        {
            while (isRunning)
            {
                BluetoothMessageResponse response = null;
                boolean read = read(response);
                if (read)
                {
                    handleMessage(response);
                }
                else
                {
                    //TODO: HANDLE READING ERROR?
                }
            }
        }

        public void cancel()
        {
            isRunning = false;
        }

        private void handleMessage(BluetoothMessageResponse message)
        {
            String action = message.getCode();
            if (action != null && !action.isEmpty())
            {
                BroadcastUtil.sendLocalBroadcast(context, action, message);
            }
        }
    }
}
