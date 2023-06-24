package soadv.grupom2.wastemate;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class BluetoothService extends Service {

    private BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private final IBinder binder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
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

    public BluetoothAdapter getAdapter() {
        return  bluetoothAdapter;
    }

    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    public void connectToDevice(BluetoothDevice device) {
        connectThread = new ConnectThread(device);
        connectThread.start();
    }

    public void sendData(String data) {
        if (connectThread != null) {
            connectThread.write(data);
        }
    }

    public void disconnect() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket socket;
        private final BluetoothDevice device;
        private  InputStream mmInStream;
        private  OutputStream mmOutStream;
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            this.device = device;
            try {
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            socket = tmp;
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();
            try {
                //socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                socket.connect();
                InputStream tmpIn = null;
                OutputStream tmpOut = null;

                try
                {
                    //Create I/O streams for connection
                    tmpIn = socket.getInputStream();
                    tmpOut = socket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();

                }

                mmInStream = tmpIn;
                mmOutStream = tmpOut;

                byte[] buffer = new byte[256];
                int bytes;

                //el hilo secundario se queda esperando mensajes del HC05
                while (true)
                {
                    try
                    {
                        //se leen los datos del Bluethoot
                        if(mmInStream.available()>0) {
                            bytes = mmInStream.read(buffer);
                            String readMessage = new String(buffer, 0, bytes);
                        }
                        //se muestran en el layout de la activity, utilizando el handler del hilo
                        // principal antes mencionado
                        //bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                    } catch (IOException e) {
                        break;
                    }
                }

                //mConnectedThread.write("{\"c\":0,\"d\":{\"mw\":999,\"md\":99,\"cd\":888}}");
                // Handle the connected socket here
            } catch (IOException connectException) {
                connectException.printStackTrace();
                try {
                    socket.close();
                } catch (IOException closeException) {
                    closeException.printStackTrace();
                }
            }
        }
    }
}
