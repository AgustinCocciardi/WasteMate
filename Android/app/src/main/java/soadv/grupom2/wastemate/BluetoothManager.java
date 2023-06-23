package soadv.grupom2.wastemate;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class BluetoothManager {

    private static BluetoothService bluetoothService;
    private static boolean isServiceBound;

    private static ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) iBinder;
            bluetoothService = binder.getService();
            isServiceBound = true;
            // The service is now bound, and you can interact with it
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothService = null;
            isServiceBound = false;
        }
    };

    public static void bindService(Context context) {
        Intent serviceIntent = new Intent(context, BluetoothService.class);
        context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public static void unbindService(Context context) {
        if (isServiceBound) {
            context.unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    public static BluetoothService getService() {
        return bluetoothService;
    }
}

