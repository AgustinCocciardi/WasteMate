package soadv.grupom2.wastemate;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity
{
    public static final int MULTIPLE_PERMISSIONS = 10;
    private final BroadcastReceiver bluetoothStatusChangedBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
            {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_OFF)
                {
                    Intent btintent = new Intent(MainActivity.this, BluetoothDisabledActivity.class);
                    btintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(btintent);
                }
            } // Handle Bluetooth state change
        }
    };
    private final View.OnClickListener btnRefreshOnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            BluetoothMessage bluetoothMessage = new BluetoothMessage(4);
            BluetoothService.getInstance().sendData(bluetoothMessage.Serialize());
        }
    };
    private final View.OnClickListener btnSettingsOnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    };
    private final View.OnClickListener btnStartMaintenanceOnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            Log.i("Envio", "Envio mensaje de inicio mantenimiento");
            BluetoothMessage bluetoothMessage = new BluetoothMessage(0);
            BluetoothService.getInstance().sendData(bluetoothMessage.Serialize());
        }
    };
    private final View.OnClickListener btnCompleteMaintenanceOnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            Log.i("Envio", "Envio mensaje de fin de mantenimiento");
            BluetoothMessage bluetoothMessage = new BluetoothMessage(1);
            BluetoothService.getInstance().sendData(bluetoothMessage.Serialize());
        }
    };
    private final View.OnClickListener btnDisableOnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            Log.i("Envio", "Envio mensaje de desahabilitar????");
            BluetoothMessage bluetoothMessage = new BluetoothMessage(2);
            BluetoothService.getInstance().sendData(bluetoothMessage.Serialize());
        }
    };
    //se crea un array de String con los permisos a solicitar en tiempo de ejecucion
    //Esto se debe realizar a partir de Android 6.0, ya que con verdiones anteriores
    //con solo solicitarlos en el Manifest es suficiente
    ArrayList<String> permissions;
    //region Attributes
    private BluetoothService bluetoothService;
    private SharedPreferences pref;
    private TextView lblStatusDescription;
    private TextView lblCurrentPercentage;
    private final OnMessageReceivedListener onMessageReceivedListener = new OnMessageReceivedListener()
    {
        @Override
        public void onMessageReceived(BluetoothDevice model, BluetoothMessageResponse response)
        {
            updateLabels(response);
        }
    };
    private final OnMessageReceivedListener onUpdateMessageReceivedListener = new OnMessageReceivedListener()
    {
        @Override
        public void onMessageReceived(BluetoothDevice model, BluetoothMessageResponse response)
        {
            updateLabels(response);
        }
    };
    private final ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder)
        {
            BluetoothService.LocalBinder localBinder = (BluetoothService.LocalBinder) binder;
            bluetoothService = localBinder.getService();
            if (!bluetoothService.getAdapter().isEnabled())
            {
                Intent btintent = new Intent(MainActivity.this, BluetoothDisabledActivity.class);
                btintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(btintent);
            }
            bluetoothService.setOnUpdateMessageReceivedListener(onUpdateMessageReceivedListener);
            bluetoothService.setOnAckMessageReceivedListener(onMessageReceivedListener);
            SharedPreferences shared = getSharedPreferences("info", MODE_PRIVATE);
            String string_temp = shared.getString("connectedDevice", null);
            if (string_temp != null)
            {
                BluetoothDevice device = bluetoothService.getAdapter().getRemoteDevice(string_temp);
                bluetoothService.connectToDevice(device);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
        }
    };

    //endregion
    private IntentFilter bluetoothStatusChangedFilter;
    //endregion


    //region Constructor
    public MainActivity()
    {
        permissions = new ArrayList<String>()
        {{
            add(Manifest.permission.ACCESS_COARSE_LOCATION);
            add(Manifest.permission.ACCESS_FINE_LOCATION);
            add(Manifest.permission.READ_PHONE_STATE);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            {
                add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            else
            {
                add(Manifest.permission.READ_MEDIA_AUDIO);
                add(Manifest.permission.READ_MEDIA_VIDEO);
                add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            {
                add(Manifest.permission.BLUETOOTH_SCAN);
                add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            else
            {
                add(Manifest.permission.BLUETOOTH);
                add(Manifest.permission.BLUETOOTH_ADMIN);
            }
        }};
    }

    private void updateLabels(BluetoothMessageResponse response)
    {
        lblStatusDescription.setText(response.data);
        lblCurrentPercentage.setText(response.getCurrentPercentage() + "%");
        if (Objects.equals(response.data, "CON CAPACIDAD"))
        {
            lblStatusDescription.setTextColor(Color.GREEN);
        }
        else if (Objects.equals(response.data, "CAPACIDAD CRITICA"))
        {
            lblStatusDescription.setTextColor(Color.YELLOW);
        }
        else if (Objects.equals(response.data, "SIN CAPACIDAD"))
        {
            lblStatusDescription.setTextColor(Color.RED);
        }
        else if (Objects.equals(response.data, "EN MANTENIMIENTO"))
        {
            lblStatusDescription.setTextColor(Color.BLUE);
        }
    }

    //region Overrides
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //Se asigna un layout al activity para poder vincular los distintos componentes
        setContentView(R.layout.activity_main);

        //region Layout Links
        Toolbar toolbar = findViewById(R.id.toolbar);
        ImageButton btnRefresh = findViewById(R.id.button_refresh_status);
        ImageView btnSettings = findViewById(R.id.button_settings);
        Button btnStartMaintenance = findViewById(R.id.button_start_maintenance);
        Button btnCompleteMaintenance = findViewById(R.id.button_complete_maintenance);
        Button btnDisable = findViewById(R.id.button_disable);
        lblStatusDescription = findViewById(R.id.label_status_description);
        lblCurrentPercentage = findViewById(R.id.label_current_percentage_description);
        //endregion

        // using toolbar as ActionBar
        setSupportActionBar(toolbar);

        //region Listeners
        btnRefresh.setOnClickListener(btnRefreshOnClickListener);
        btnSettings.setOnClickListener(btnSettingsOnClickListener);
        btnStartMaintenance.setOnClickListener(btnStartMaintenanceOnClickListener);
        btnCompleteMaintenance.setOnClickListener(btnCompleteMaintenanceOnClickListener);
        btnDisable.setOnClickListener(btnDisableOnClickListener);
        //endregion

        checkPermissions();

        //Inicio el servicio de Bluetooth
        Intent serviceIntent = new Intent(this, BluetoothService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

        IntentFilter filter = new IntentFilter(Actions.CUSTOM_ACTION_STATUS_CHANGED);

        bluetoothStatusChangedFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStatusChangedBroadcastReceiver, bluetoothStatusChangedFilter);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (bluetoothService != null)
        {
            if (!bluetoothService.getAdapter().isEnabled())
            {
                Intent btintent = new Intent(MainActivity.this, BluetoothDisabledActivity.class);
                btintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(btintent);
            }
        }
        registerReceiver(bluetoothStatusChangedBroadcastReceiver, bluetoothStatusChangedFilter);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        unregisterReceiver(bluetoothStatusChangedBroadcastReceiver);
    }
    //endregion

    @Override
    protected void onRestart()
    {
        super.onRestart();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        BluetoothService.getInstance().disconnect();
        unbindService(serviceConnection);
    }

    //Metodo que chequea si estan habilitados los permisos
    private boolean checkPermissions()
    {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();

        for (String p : permissions)
        {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED)
            {
                listPermissionsNeeded.add(p);
            }
        }

        if (!listPermissionsNeeded.isEmpty())
        {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        ArrayList<String> deniedPermissions = new ArrayList<>();
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MULTIPLE_PERMISSIONS)
        {
            for (int i = 0; i < permissions.length; i++)
            {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                {
                    deniedPermissions.add(permissions[i]);
                }
            }

            if (!deniedPermissions.isEmpty())
            {
                Intent permissionMissingIntent = new Intent(MainActivity.this, PermissionsMissingActivity.class);
                permissionMissingIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                Bundle extras = new Bundle();
                extras.putStringArrayList("deniedPermissions", deniedPermissions);
                permissionMissingIntent.putExtras(extras);
                startActivity(permissionMissingIntent);
            }
        }
    }
}