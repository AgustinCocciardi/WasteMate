package com.grupom2.wastemate.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.grupom2.wastemate.R;
import com.grupom2.wastemate.bluetooth.BluetoothService;
import com.grupom2.wastemate.constant.Actions;
import com.grupom2.wastemate.constant.Constants;
import com.grupom2.wastemate.model.BluetoothDeviceData;
import com.grupom2.wastemate.model.BluetoothMessage;
import com.grupom2.wastemate.receiver.BluetoothDisabledBroadcastReceiver;
import com.grupom2.wastemate.util.BroadcastUtil;
import com.grupom2.wastemate.util.CustomProgressDialog;
import com.grupom2.wastemate.util.NavigationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class MainActivity extends AppCompatActivity
{
    public static final int MULTIPLE_PERMISSIONS = 10;

    private final BluetoothDisabledBroadcastReceiver bluetoothDisabledBroadcastReceiver;

    private final View.OnClickListener btnRefreshOnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            BluetoothMessage bluetoothMessage = new BluetoothMessage(Constants.CODE_UPDATE_REQUESTED);
            BluetoothService.getInstance().write(bluetoothMessage);
        }
    };

    private final View.OnClickListener btnSettingsOnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            NavigationUtil.navigateToSettingsActivity(view.getContext());
        }
    };

    private final View.OnClickListener btnStartMaintenanceOnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            BluetoothMessage bluetoothMessage = new BluetoothMessage(Constants.CODE_MAINTENANCE_STARTED);
            BluetoothService.getInstance().write(bluetoothMessage);
        }
    };

    private final View.OnClickListener btnCompleteMaintenanceOnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            BluetoothMessage bluetoothMessage = new BluetoothMessage(Constants.CODE_MAINTENANCE_COMPLETED);
            BluetoothService.getInstance().write(bluetoothMessage);
        }
    };

    private final View.OnClickListener btnDisableOnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            BluetoothMessage bluetoothMessage = new BluetoothMessage(Constants.CODE_DISABLE);
            BluetoothService.getInstance().write(bluetoothMessage);
        }
    };

    private final BroadcastReceiver updateStatusBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            BluetoothDeviceData data = BroadcastUtil.getData(intent, BluetoothDeviceData.class);
            updateLabels(data);
        }
    };

    private final BroadcastReceiver noDeviceConnectedBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AppTheme);
            builder.setTitle("Conexión Bluetooth requerida");
            builder.setMessage("Para el correcto funcionamiento de esta aplicación, es necesario conectarse a un dispositivo bluetooth. De lo contrario, no se recibirán actualizaciones de estado ni se podrá interactuar con un contenedor");
            builder.setPositiveButton("Conectar", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    NavigationUtil.navigateToSettingsActivity(MainActivity.this);
                }
            });
            builder.setNegativeButton("Ignorar", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    showDisabled();
                }
            });
            builder.setCancelable(true); // Prevent the user from dismissing the dialog without enabling Bluetooth
            builder.show();
        }
    };
    private ImageButton btnStartMaintenance;
    private Button btnCompleteMaintenance;
    private Button btnDisable;

    private void showDisabled()
    {
        this.btnStartMaintenance.setEnabled(false);
        this.btnCompleteMaintenance.setEnabled(false);
        this.btnDisable.setEnabled(false);
        this.lblCurrentPercentage.setText("DESCONECTADO");
        this.lblCurrentPercentage.setText("DESCONECTADO");
    }


    //se crea un array de String con los permisos a solicitar en tiempo de ejecucion
    //Esto se debe realizar a partir de Android 6.0, ya que con verdiones anteriores
    //con solo solicitarlos en el Manifest es suficiente
    private ArrayList<String> permissions;

    //region Attributes
    private TextView lblStatusDescription;
    private TextView lblCurrentPercentage;
    private BroadcastReceiver serviceConnectedBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            customProgressDialog.dismiss();
        }
    };
    private CustomProgressDialog customProgressDialog;


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

        bluetoothDisabledBroadcastReceiver = new BluetoothDisabledBroadcastReceiver();
    }

    private void updateLabels(BluetoothDeviceData response)
    {
        //TODO: MEJORAR
        lblStatusDescription.setText(response.getStatus());
        lblCurrentPercentage.setText(response.getCurrentPercentage() + "%");
        if (Objects.equals(response.getStatus(), "CON CAPACIDAD"))
        {
            lblStatusDescription.setTextColor(Color.GREEN);
        }
        else if (Objects.equals(response.getStatus(), "CAPACIDAD CRITICA"))
        {
            lblStatusDescription.setTextColor(Color.YELLOW);
        }
        else if (Objects.equals(response.getStatus(), "SIN CAPACIDAD"))
        {
            lblStatusDescription.setTextColor(Color.RED);
        }
        else if (Objects.equals(response.getStatus(), "EN MANTENIMIENTO"))
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
        btnStartMaintenance = findViewById(R.id.button_start_maintenance);
        btnCompleteMaintenance = findViewById(R.id.button_complete_maintenance);
        btnDisable = findViewById(R.id.button_disable);
        lblStatusDescription = findViewById(R.id.label_status_description);
        lblCurrentPercentage = findViewById(R.id.label_current_percentage_description);
        //endregion

        // using toolbar as ActionBar
        //setSupportActionBar(toolbar);

        //region Listeners
        btnRefresh.setOnClickListener(btnRefreshOnClickListener);
        btnSettings.setOnClickListener(btnSettingsOnClickListener);
        btnStartMaintenance.setOnClickListener(btnStartMaintenanceOnClickListener);
        btnCompleteMaintenance.setOnClickListener(btnCompleteMaintenanceOnClickListener);
        btnDisable.setOnClickListener(btnDisableOnClickListener);
        //endregion

        checkPermissions();

        BroadcastUtil.registerLocalReceiver(this, serviceConnectedBroadcastReceiver, Actions.ACTION_SERVICE_CONNECTED);
        BroadcastUtil.registerReceiver(this, bluetoothDisabledBroadcastReceiver);
        BroadcastUtil.registerLocalReceiver(this, updateStatusBroadcastReceiver, Actions.ACTION_UPDATE, Actions.ACTION_ACK);

        BluetoothService.startService(getApplicationContext());

        customProgressDialog = new CustomProgressDialog(MainActivity.this);
        customProgressDialog.show();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        BroadcastUtil.registerLocalReceiver(this, noDeviceConnectedBroadcastReceiver, Actions.ACTION_NO_DEVICE_CONNECTED);
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
        BroadcastUtil.unregisterLocalReceiver(this, noDeviceConnectedBroadcastReceiver);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        BluetoothService.stopService(getApplicationContext());
        BroadcastUtil.unregisterLocalReceiver(this, serviceConnectedBroadcastReceiver);
        BroadcastUtil.unregisterReceiver(this, bluetoothDisabledBroadcastReceiver);
        BroadcastUtil.unregisterLocalReceiver(this, updateStatusBroadcastReceiver);
    }
    //endregion

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
                permissionMissingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Bundle extras = new Bundle();
                extras.putStringArrayList("deniedPermissions", deniedPermissions);
                permissionMissingIntent.putExtras(extras);
                startActivity(permissionMissingIntent);
            }
        }
    }
}