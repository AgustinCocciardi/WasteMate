package soadv.grupom2.wastemate;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class BluetoothManager {

    private static BluetoothService bluetoothService;
    private static boolean isServiceBound;

    private static ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            bluetoothService = binder.getService();
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
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

    public static void stopService(Context context){
        bluetoothService.disconnect();
        context.unbindService(serviceConnection);
    }

    public static BluetoothService getService() {
        return bluetoothService;
    }
}

