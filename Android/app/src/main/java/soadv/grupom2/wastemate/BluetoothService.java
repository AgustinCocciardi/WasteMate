package soadv.grupom2.wastemate;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Objects;
import java.util.UUID;

public class BluetoothService extends Service
{
    private static BluetoothService instance;
    private BluetoothConnection bluetoothConnection;
    private BluetoothAdapter bluetoothAdapter;
    private static final int MAX_CONNECTION_ATTEMPTS = 10;
    private final IBinder binder = new LocalBinder();
    private boolean stop;
    private OnMessageReceivedListener onErrorMessageReceivedListener;
    private OnMessageReceivedListener onUpdateMessageReceivedListener;
    private OnMessageReceivedListener onAckMessageReceivedListener;
    private OnMessageReceivedListener onDeviceUnsupportedListener;

    private Thread connectThread;
    private Thread receiveDataThread;

    public static BluetoothService getInstance()
    {
        return instance;
    }

    //region Overrides
    @Override
    public void onCreate()
    {
        super.onCreate();
        instance = this;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy()
    {
        disconnect();
    }
    //endregion
    public BluetoothDevice getDevice()
    {
        return isConnected() ? bluetoothConnection.device : null;
    }

    public BluetoothAdapter getAdapter()
    {
        if(bluetoothAdapter == null)
        {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        return  bluetoothAdapter;
    }

    public boolean isDeviceConnected(BluetoothDevice device)
    {
        return isConnected() && bluetoothConnection.device.equals(device);
    }

    public void connectToDevice(BluetoothDevice device)
    {
        connectThread = new Thread(()->doConnect(device));
        connectThread.start();
    }

    private void doConnect(BluetoothDevice device)
    {
        BluetoothSocket tmpSocket = null;
        try
        {
            tmpSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            bluetoothAdapter.cancelDiscovery();
            tmpSocket.connect();
            InputStream tmpIn;
            OutputStream tmpOut;
            try {
                tmpIn = tmpSocket.getInputStream();
                tmpOut = tmpSocket.getOutputStream();

                boolean ack = false;
                int connectionAttempts = 0;
                doWrite(tmpOut,new BluetoothMessage(Constants.CODE_CONNECTION_REQUESTED).Serialize());
                int bytes;
                byte[] buffer = new byte[256];

                while (!ack && connectionAttempts < MAX_CONNECTION_ATTEMPTS) {
                    try {
                        if (tmpIn.available() > 0) {
                            bytes = tmpIn.read(buffer);
                            String readMessage = new String(buffer, 0, bytes);
                            BluetoothMessageResponse response = BluetoothMessageResponse.fromJson(readMessage);
                            if (response.code.equals(Constants.CODE_ACK)) {
                                ack = true;

                                this.bluetoothConnection = new BluetoothConnection();
                                bluetoothConnection.device = device;
                                bluetoothConnection.inputStream = tmpIn;
                                bluetoothConnection.outputStream = tmpOut;
                                bluetoothConnection.socket = tmpSocket;

                                receiveDataThread = new Thread(this::doReceive);
                                receiveDataThread.start();

                                if(onAckMessageReceivedListener!= null){
                                    onAckMessageReceivedListener.onMessageReceived(device, response);
                                }
                            }
                        } else {
                            Thread.sleep(1000);
                        }
                        connectionAttempts++;
                    } catch (IOException e) {
                        break;
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (!ack) {
                    if(onDeviceUnsupportedListener!= null){
                        this.onDeviceUnsupportedListener.onMessageReceived(device, null);
                    }
                    tmpIn.close();
                    tmpOut.close();
                    tmpSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                if(onDeviceUnsupportedListener!= null){
                    this.onDeviceUnsupportedListener.onMessageReceived(device, null);
                }
            }
        } catch (IOException connectException) {
            connectException.printStackTrace();
            if(onDeviceUnsupportedListener!= null){
                this.onDeviceUnsupportedListener.onMessageReceived(device, null);
            }
            try {
                if (tmpSocket != null) {
                    tmpSocket.close();
                }
            } catch (IOException closeException) {
                closeException.printStackTrace();
            }
        }
        catch (SecurityException securityException){
            //TODO BLUETOOTH permission.
        }
    }

    private void doReceive()
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(bluetoothConnection.inputStream));
        while (!stop)
        {
            try
            {


                if(bluetoothConnection.inputStream.available()>0)
                {
                    String message = reader.readLine();
                    BluetoothMessageResponse response =BluetoothMessageResponse.fromJson(message);
                    if(Objects.equals(response.code, Constants.CODE_ERROR))
                    {
                        if(onErrorMessageReceivedListener!= null){
                            this.onErrorMessageReceivedListener.onMessageReceived(bluetoothConnection.device, response);
                        }
                    }
                    else if (Objects.equals(response.code, Constants.CODE_UPDATE_STATUS))
                    {
                        if(onUpdateMessageReceivedListener!= null){
                            this.onUpdateMessageReceivedListener.onMessageReceived(bluetoothConnection.device, response);
                        }
                    }
                }
            } catch (IOException e) {
                break;
            }
        }
        try {
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void sendData(String data)
    {
        if(isConnected())
        {
            Thread sendDataThread = new Thread(()->doWrite(bluetoothConnection.outputStream, data));
            sendDataThread.start();
        }
    }

    private boolean isConnected()
    {
        return bluetoothConnection != null;
    }

    public void disconnect()
    {
        if (isConnected())
        {
            stop = true;
            receiveDataThread = null;
            connectThread = null;
            try {
                bluetoothConnection.outputStream.close();
                bluetoothConnection.inputStream.close();
                bluetoothConnection.socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            bluetoothConnection = null;
        }
    }

    public void doWrite(OutputStream outputStream, String data)
    {
        byte[] msgBuffer = data.getBytes();
        try {
            outputStream.write(msgBuffer);
        }
        catch (IOException e)
        {
        }
    }

    public void setOnErrorMessageReceivedListener(OnMessageReceivedListener onErrorMessageReceivedListener) {
        this.onErrorMessageReceivedListener = onErrorMessageReceivedListener;
    }
    public void setOnUpdateMessageReceivedListener(OnMessageReceivedListener onUpdateMessageReceivedListener) {
        this.onUpdateMessageReceivedListener = onUpdateMessageReceivedListener;
    }

    public void setOnAckMessageReceivedListener(OnMessageReceivedListener onAckMessageReceivedListener) {
        this.onAckMessageReceivedListener = onAckMessageReceivedListener;
    }

    public void setOnDeviceUnsupportedListener(OnMessageReceivedListener onDeviceUnsupportedListener) {
        this.onDeviceUnsupportedListener = onDeviceUnsupportedListener;
    }

    public class LocalBinder extends Binder
    {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }
}



