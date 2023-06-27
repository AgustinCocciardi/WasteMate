package soadv.grupom2.wastemate;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothConnection {
    BluetoothDevice device;
    BluetoothSocket socket;
    InputStream inputStream;
    OutputStream outputStream;
}
