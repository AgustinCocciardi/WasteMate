package soadv.grupom2.wastemate;

import android.bluetooth.BluetoothDevice;

public interface OnMessageReceivedListener {
    void onMessageReceived(BluetoothDevice model, BluetoothMessageResponse response);
}
