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

import androidx.annotation.NonNull;

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
    private BluetoothAdapter bluetoothAdapter;
    private final Context context;
    private final BluetoothDeviceData deviceData;
    private final Object socketLock = new Object();
    private String lastDeviceConnectedAddress;
    private static final int MESSAGE_DELAY = 500; // 0.5 segundos

    private boolean isWriting = false;

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
        data = withEndLine(data);
        byte[] msgBuffer = data.getBytes();
        synchronized (lock)
        {
            if (!isWriting)
            {
                isWriting = true;
                writeData(msgBuffer);
                startTimer();
            }
        }
    }

    @NonNull
    private static String withEndLine(String data)
    {
        if (!data.endsWith("\n"))
        {
            data += "\n";
        }
        return data;
    }

    private void writeData(byte[] data)
    {
        try
        {
            OutputStream outputStream = bluetoothSocket.getOutputStream();
            outputStream.write(data);
            outputStream.flush();
        }
        catch (IOException ignored)
        {
        }
    }

    private void startTimer()
    {
        Thread timerThread = new Thread(() ->
        {
            try
            {
                Thread.sleep(MESSAGE_DELAY);
                synchronized (lock)
                {
                    isWriting = false;
                }
            }
            catch (InterruptedException e)
            {
                // Handle interruption if needed
            }
        });
        timerThread.start();
    }

    public void disconnect()
    {
        synchronized (socketLock)
        {
            if (isConnectedUnsafe())
            {
                lastDeviceConnectedAddress = bluetoothDevice.getAddress();
            }
            if (bluetoothSocket != null)
            {
                try
                {
                    bluetoothSocket.close();
                }
                catch (IOException | SecurityException ignored)
                {
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

    private BluetoothMessageResponse readUnsafe(InputStream inputStream)
    {
        BluetoothMessageResponse response = null;
        try
        {
            int bytesRead;

            if (inputStream.available() > 0 && (bytesRead = inputStream.read()) == '{')
            {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append((char) bytesRead);

                do
                {
                    bytesRead = inputStream.read();
                    stringBuilder.append((char) bytesRead);

                } while (bytesRead != '}');

                String readMessage = stringBuilder.toString();

                BluetoothMessageResponse messageResponse = BluetoothMessageResponse.fromJson(readMessage);
                if (messageResponse != null)
                {
                    response = messageResponse;
                }
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
        return getBluetoothAdapter().getBondedDevices();
    }

    private BluetoothAdapter getBluetoothAdapter()
    {
        if (bluetoothAdapter == null)
        {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        return bluetoothAdapter;
    }

    public void startDiscovery() throws SecurityException
    {
        getBluetoothAdapter().startDiscovery();
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

    public boolean isLastDeviceConnected(BluetoothDevice device)
    {
        synchronized (socketLock)
        {
            return Objects.equals(lastDeviceConnectedAddress, device.getAddress());
        }
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
                public void handleMessage(Message msg)
                {
                    try
                    {
                        synchronized (socketLock)
                        {
                            bluetoothDevice = getBluetoothAdapter().getRemoteDevice(deviceAddress);
                            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                            bluetoothSocket.connect();
                        }
                        write(new BluetoothMessage(Constants.CODE_CONNECTION_REQUESTED).Serialize());
                        handler = new ConnectionHandler(handlerThread.getLooper());
                        handler.sendEmptyMessage(0);
                    }
                    catch (IOException | SecurityException e)
                    {
                        BroadcastUtil.sendLocalBroadcast(context, Actions.LOCAL_ACTION_CONNECTION_CANCELED, null);
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
        private static final int CONNECTION_INTERVAL = 2000;
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
                BroadcastUtil.sendLocalBroadcast(context, Actions.LOCAL_ACTION_UNSUPPORTED_DEVICE, null);
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
                            BluetoothMessageResponse response = readUnsafe(inputStream);

                            if (response != null)
                            {
                                ackReceived = true;
                                deviceData.setData(response.getData(), response.getCriticalPercentage(), response.getFullPercentage(), response.getCurrentPercentage(), response.getMaximumWeight(), response.getIsCalibrating());
                                BluetoothDeviceData messageBody = new BluetoothDeviceData(deviceData);
                                socketReader = new SocketReader();
                                socketReader.start();
                                BroadcastUtil.sendLocalBroadcast(context, Actions.ARDUINO_ACTION_ACK, messageBody);
                                removeCallbacksAndMessages(null);
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        BroadcastUtil.sendLocalBroadcast(context, Actions.LOCAL_ACTION_UNSUPPORTED_DEVICE, null);
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
            private static final int CONNECTION_INTERVAL = 50;

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
                        BluetoothMessageResponse message = readUnsafe(bluetoothSocket.getInputStream());
                        if (message != null)
                        {
                            processMessage(message);
                        }
                    }
                    catch (IOException e)
                    {
                        Log.i("BluetoothConnection.ReadingHandler", "Error leyendo del socket");
                    }
                }
                sendEmptyMessageDelayed(0, CONNECTION_INTERVAL);
            }

            private void processMessage(BluetoothMessageResponse message)
            {
                String action = message.getCode();
                if (action != null && !action.isEmpty())
                {
                    if (action.equals(Actions.ARDUINO_ACTION_ACK) || action.equals(Actions.ARDUINO_ACTION_UPDATE))
                    {
                        deviceData.setData(message.getData(), message.getCriticalPercentage(), message.getFullPercentage(), message.getCurrentPercentage(), message.getMaximumWeight(), message.getIsCalibrating());
                    }
                    BroadcastUtil.sendLocalBroadcast(context, action, new BluetoothDeviceData(deviceData));
                }
            }
        }
    }
}
