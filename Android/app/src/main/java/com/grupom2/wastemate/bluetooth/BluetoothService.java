package com.grupom2.wastemate.bluetooth;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.grupom2.wastemate.constant.Actions;
import com.grupom2.wastemate.util.BroadcastUtil;

// Servicio para manejar las conexiones Bluetooth
// El servicio debe tener su propio ciclo de vida, independiente de las activities.
public class BluetoothService extends Service
{
    // Implementamos un Singleton dentro del servicio
    // para poder acceder desde cualquier activity.
    private static volatile BluetoothManager bluetoothManager;

    public BluetoothService()
    {
    }

    public static BluetoothManager getInstance()
    {
        return bluetoothManager;
    }

    // Method to start the service
    public static void startService(Context context)
    {
        Intent serviceIntent = new Intent(context, BluetoothService.class);
        context.startService(serviceIntent);
    }

    // Method to stop the service
    public static void stopService(Context context)
    {
        bluetoothManager.disconnect();
        Intent serviceIntent = new Intent(context, BluetoothService.class);
        context.stopService(serviceIntent);
    }

    //region Overrides
    @Override
    public void onCreate()
    {
        super.onCreate();
        bluetoothManager = new BluetoothManager(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        BroadcastUtil.sendLocalBroadcast(this, Actions.LOCAL_ACTION_SERVICE_CONNECTED, null);
        return START_STICKY;
    }

    // Para poder conectarnos al bluetooth independientemente de la activity,
    // deshabilitamos el onBind para que no se pueda vincular el servicio a una activity.
    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
    //endregion
}
