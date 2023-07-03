package com.grupom2.wastemate.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

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
    private BluetoothSocket bluetoothSocket;
    private SocketReader socketReader;
    private SocketConnectionStarter socketConnectionStarter;
    private final BluetoothAdapter bluetoothAdapter;
    private final Context context;
    private BluetoothDeviceData deviceData;

    private final Object socketLock = new Object();

    public BluetoothConnection(Context context)
    {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceData = new BluetoothDeviceData();
    }

    public void connectToDevice(String deviceAddress, UUID uuid)
    {
        synchronized (socketLock)
        {
            socketConnectionStarter = new SocketConnectionStarter(deviceAddress, uuid);
            socketConnectionStarter.start();
        }
    }

    public void write(String data)
    {
        byte[] msgBuffer = data.getBytes();
        synchronized (socketLock)
        {
            try
            {
                if (isConnectedUnsafe())
                {
                    OutputStream outputStream = bluetoothSocket.getOutputStream();

                    Thread writeThread = new Thread(() -> writeToStream(msgBuffer, outputStream));

                    writeThread.start();
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private static void writeToStream(byte[] msgBuffer, OutputStream outputStream)
    {
        try
        {
            outputStream.write(msgBuffer);
            outputStream.flush();
        }
        catch (IOException e)
        {
            Log.e("BluetoothConnection.writeToStream", "Error escribiendo en el Socket");
        }
    }

    public void disconnect()
    {
        synchronized (socketLock)
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

    private boolean isConnectedUnsafe()
    {
        return bluetoothSocket != null && bluetoothSocket.isConnected();
    }

    public boolean isConnected()
    {
        synchronized (socketLock)
        {
            return isConnectedUnsafe();
        }
    }

    private ArrayList<BluetoothMessageResponse> readUnsafe(InputStream inputStream)
    {
        ArrayList<BluetoothMessageResponse> response = new ArrayList<>();
        try
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
        catch (IOException e)
        {
            Log.e("BluetoothConnection.readUnsafe", "Error en la lectura del socket");
        }
        return response;
    }

    public String getDeviceAddress()
    {
        synchronized (socketLock)
        {
            return bluetoothDevice != null ? bluetoothDevice.getAddress() : null;
        }
    }

    public boolean isConnected(BluetoothDevice device)
    {
        synchronized (socketLock)
        {
            return isConnectedUnsafe() && Objects.equals(device.getAddress(), bluetoothDevice.getAddress());
        }
    }

    public String getDeviceName() throws SecurityException
    {
        synchronized (socketLock)
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

    public BluetoothDeviceData getDeviceData()
    {
        synchronized (socketLock)
        {
            return new BluetoothDeviceData(deviceData);
        }
    }

    public void startCalibration()
    {
        deviceData.setIsCalibrating(true);
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
                        synchronized (socketLock)
                        {
                            bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
                            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                            bluetoothSocket.connect();
                        }
                        write(new BluetoothMessage(Constants.CODE_CONNECTION_REQUESTED).Serialize());
                        handler = new ConnectionHandler(handlerThread.getLooper());
                        handler.sendEmptyMessage(0);
                    }
                    catch (IOException e)
                    {
                        BroadcastUtil.sendLocalBroadcast(context, Actions.ACTION_CONNECTION_CANCELED, null);
                        disconnect();
                    }
                }
            };

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

            synchronized (socketLock)
            {
                if (isConnectedUnsafe())
                {
                    try
                    {
                        InputStream inputStream = bluetoothSocket.getInputStream();
                        if (inputStream.available() > 0)
                        {
                            ArrayList<BluetoothMessageResponse> read = readUnsafe(inputStream);

                            if (!read.isEmpty())
                            {
                                BluetoothMessageResponse response = read.stream().filter(r -> Objects.equals(r.getCode(), Constants.CODE_ACK)).findFirst().orElse(null);
                                if (response != null)
                                {
                                    ackReceived = true;
                                    deviceData.setData(response.getData(), response.getCriticalPercentage(), response.getFullPercentage(), response.getCurrentPercentage(), response.getMaximumWeight(), response.getIsCalibrating());
                                    BluetoothDeviceData messageBody = new BluetoothDeviceData(deviceData);
                                    socketReader = new SocketReader();
                                    socketReader.start();
                                    BroadcastUtil.sendLocalBroadcast(context, Actions.ACTION_ACK, messageBody);
                                    removeCallbacksAndMessages(null);
                                }
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        BroadcastUtil.sendLocalBroadcast(context, Actions.ACTION_UNSUPPORTED_DEVICE, null);
                        disconnect();
                        removeCallbacksAndMessages(null);
                    }
                }

                if (!ackReceived)
                {
                    connectionAttempts++;
                    sendEmptyMessageDelayed(0, CONNECTION_INTERVAL);
                }
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
                synchronized (socketLock)
                {
                    try
                    {
                        ArrayList<BluetoothMessageResponse> messages = readUnsafe(bluetoothSocket.getInputStream());
                        if (!messages.isEmpty())
                        {
                            processMessages(messages);
                        }
                    }
                    catch (IOException e)
                    {
                        Log.i("BluetoothConnection.ReadingHandler", "Error leyendo del socket");
                    }
                }
                sendEmptyMessageDelayed(0, CONNECTION_INTERVAL);
            }

            private void processMessages(ArrayList<BluetoothMessageResponse> messages)
            {
                for (BluetoothMessageResponse message : messages)
                {
                    String action = message.getCode();
                    if (action != null && !action.isEmpty())
                    {
                        if (action.equals(Actions.ACTION_ACK) || action.equals(Actions.ACTION_UPDATE))
                        {
                            deviceData.setData(message.getData(), message.getCriticalPercentage(), message.getFullPercentage(), message.getCurrentPercentage(), message.getMaximumWeight(), message.getIsCalibrating());
                        }
                        BroadcastUtil.sendLocalBroadcast(context, action, new BluetoothDeviceData(deviceData));
                        try
                        {
                            Thread.sleep(200);
                        }
                        catch (InterruptedException ignored)
                        {
                        }
                    }
                }
            }
        }
    }
}
