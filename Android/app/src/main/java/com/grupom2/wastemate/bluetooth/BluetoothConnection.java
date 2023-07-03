package com.grupom2.wastemate.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.grupom2.wastemate.R;
import com.grupom2.wastemate.constant.Actions;
import com.grupom2.wastemate.constant.Constants;
import com.grupom2.wastemate.model.BluetoothDeviceData;
import com.grupom2.wastemate.model.BluetoothMessage;
import com.grupom2.wastemate.model.BluetoothMessageResponse;
import com.grupom2.wastemate.util.BroadcastUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class BluetoothConnection
{
    private final Object lock = new Object();
    private BluetoothDevice bluetoothDevice;
    private BluetoothDeviceData deviceData;
    private BluetoothSocket bluetoothSocket;
    private SocketReader socketReader;
    private SocketConnectionStarter socketConnectionStarter;
    private final BluetoothAdapter bluetoothAdapter;
    private final Context context;

    public BluetoothConnection(Context context)
    {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void connectToDevice(String deviceAddress, UUID uuid)
    {
        synchronized (lock)
        {
            socketConnectionStarter = new SocketConnectionStarter(deviceAddress, uuid);
            socketConnectionStarter.start();
        }
    }

    public void write(String data)
    {
        synchronized (lock)
        {
            byte[] msgBuffer = data.getBytes();

            if (isConnected())
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
            if (socketConnectionStarter != null)
            {
                socketConnectionStarter.stop();
                socketConnectionStarter = null;
            }

            if (socketReader != null)
            {
                socketReader.stop();
                socketReader = null;
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

    private ArrayList<BluetoothMessageResponse> read(InputStream inputStream)
    {
        ArrayList<BluetoothMessageResponse> response = new ArrayList<>();
        try
        {
            synchronized (lock)
            {
                if (inputStream.available() > 0)
                {
                    byte[] buffer = new byte[256];
                    int bytes = inputStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);

                    String[] jsonParts = readMessage.split("\\}\\{"); // Split by the JSON object delimiter

                    String json = Arrays.stream(jsonParts).findFirst().orElse("");

                    if (!json.endsWith("}"))
                    {
                        json = json + "}";
                    }

                    response.add(BluetoothMessageResponse.fromJson(json));
                }
            }
        }
        catch (IOException e)
        {
            //TODO: HANDLE EXCEPTION
        }
        return response;
    }

    public String getDeviceAddress()
    {
        synchronized (lock)
        {
            return bluetoothDevice != null ? bluetoothDevice.getAddress() : null;
        }
    }

    public boolean isConnected(BluetoothDevice device)
    {
        return isConnected() && Objects.equals(device.getAddress(), bluetoothDevice.getAddress());
    }

    public String getDeviceName() throws SecurityException
    {
        if (bluetoothDevice != null)
        {
            String name = bluetoothDevice.getName();
            return name == null ? bluetoothDevice.getAddress() : name;
        }
        else
        {
            return context.getResources().getString(R.string.status_not_connected);
        }
    }

    public Set<BluetoothDevice> getBondedDevices() throws SecurityException
    {
        return bluetoothAdapter.getBondedDevices();
    }

    public void startDiscovery() throws SecurityException
    {
        bluetoothAdapter.startDiscovery();
    }

    public boolean isEnabled()
    {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    private class SocketConnectionStarter
    {
        private HandlerThread handlerThread;
        private ConnectionHandler handler;
        private final String deviceAddress;
        private final UUID uuid;

        public SocketConnectionStarter(String deviceAddress, UUID uuid)
        {
            this.deviceAddress = deviceAddress;
            this.uuid = uuid;
        }

        public void start()
        {
            handlerThread = new HandlerThread("SocketConnectionStarter");
            handlerThread.start();
            Handler socketConnectionHandler = new Handler(handlerThread.getLooper())
            {
                @Override
                public void handleMessage(Message msg) throws SecurityException
                {
                    try
                    {
                        bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
                        //TODO: VALIDAR SI LA VERIFICACION DE PERMISOS ANDA. SI ANDA, SUPRIMIR WARNING.
                        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                        bluetoothSocket.connect();
                        write(new BluetoothMessage(Constants.CODE_CONNECTION_REQUESTED).Serialize());
                        handler = new ConnectionHandler(handlerThread.getLooper());
                        handler.sendEmptyMessage(0);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        //TODO: MANAGE EXCEPTION  - CONNECTION FAILED.
                    }
                    catch (SecurityException e)
                    {
                        //TODO: HANDLE EXCEPTION

                    }
                }
            };

            // Post a message to the Handler to start the processing
            socketConnectionHandler.sendEmptyMessage(0);
        }

        public void stop()
        {
            if (handler != null)
            {
                handler.removeCallbacksAndMessages(null);

            }
            if (handlerThread != null)
            {
                handlerThread.quit();
            }
        }
    }

    private class ConnectionHandler extends Handler
    {
        private static final int MAX_CONNECTION_ATTEMPTS = 5;
        private static final int CONNECTION_INTERVAL = 1000;
        private boolean ackReceived;
        private int connectionAttempts;

        public ConnectionHandler(Looper looper)
        {
            super(looper);
        }

        @Override
        public void handleMessage(android.os.Message msg)
        {
            //TODO: improve this.
            if (ackReceived)
            {
                return;
            }
            if (connectionAttempts >= MAX_CONNECTION_ATTEMPTS)
            {
                BroadcastUtil.sendLocalBroadcast(context, Actions.ACTION_UNSUPPORTED_DEVICE, null);
                disconnect();
                removeCallbacksAndMessages(null);
                return;
            }

            if (isConnected())
            {
                try
                {
                    InputStream inputStream = bluetoothSocket.getInputStream();
                    if (inputStream.available() > 0)
                    {
                        ArrayList<BluetoothMessageResponse> read = read(inputStream);

                        if (!read.isEmpty())
                        {
                            BluetoothMessageResponse response = read.stream().filter(r -> Objects.equals(r.getCode(), Constants.CODE_ACK)).findFirst().orElse(null);
                            if (response != null)
                            {
                                ackReceived = true;
                                deviceData = new BluetoothDeviceData();
                                deviceData.setData(response.getData(), response.getCriticalPercentage(), response.getFullPercentage(), response.getCurrentPercentage(), response.getMaximumWeight());
                                socketReader = new SocketReader();
                                socketReader.start();
                                BroadcastUtil.sendLocalBroadcast(context, Actions.ACTION_ACK, deviceData);
                                removeCallbacksAndMessages(null);
                            }
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
    }

    private class SocketReader
    {
        private HandlerThread handlerThread;
        private Handler handler;

        public void start()
        {
            handlerThread = new HandlerThread("ReadingThread");
            handlerThread.start();
            handler = new ReadingHandler(handlerThread.getLooper());
            handler.sendEmptyMessage(0);
        }

        public void stop()
        {
            if (handler != null)
            {
                handler.removeCallbacksAndMessages(null);

            }
            if (handlerThread != null)
            {
                handlerThread.quit();
            }
        }

        private class ReadingHandler extends Handler
        {
            private static final int CONNECTION_INTERVAL = 3000;

            public ReadingHandler(Looper looper)
            {
                super(looper);
            }

            @Override
            public void handleMessage(android.os.Message msg)
            {
                try
                {
                    ArrayList<BluetoothMessageResponse> read = read(bluetoothSocket.getInputStream());
                    if (!read.isEmpty())
                    {
                        for (BluetoothMessageResponse response : read)
                        {
                            String action = response.getCode();
                            if (action != null && !action.isEmpty())
                            {
                                if (action.equals(Actions.ACTION_ACK) || action.equals(Actions.ACTION_UPDATE))
                                {
                                    deviceData.setData(response.getData(), response.getCriticalPercentage(), response.getFullPercentage(), response.getCurrentPercentage(), response.getMaximumWeight());
                                }
                                BroadcastUtil.sendLocalBroadcast(context, action, deviceData);
                                try
                                {
                                    Thread.sleep(200);
                                }
                                catch (InterruptedException e)
                                {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                    else
                    {
                        //TODO: HANDLE READING ERROR?
                    }
                }
                catch (IOException e)
                {
                    //TODO: HANDLE throw new RuntimeException(e);
                }

                sendEmptyMessageDelayed(0, CONNECTION_INTERVAL);
            }
        }
    }
}
