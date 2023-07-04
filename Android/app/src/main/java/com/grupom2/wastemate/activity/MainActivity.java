package com.grupom2.wastemate.activity;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.grupom2.wastemate.R;
import com.grupom2.wastemate.bluetooth.BluetoothManager;
import com.grupom2.wastemate.bluetooth.BluetoothService;
import com.grupom2.wastemate.constant.Actions;
import com.grupom2.wastemate.constant.Constants;
import com.grupom2.wastemate.model.BluetoothDeviceData;
import com.grupom2.wastemate.model.BluetoothMessage;
import com.grupom2.wastemate.model.Status;
import com.grupom2.wastemate.receiver.BluetoothDisabledBroadcastReceiver;
import com.grupom2.wastemate.receiver.SafeBroadcastReceiver;
import com.grupom2.wastemate.util.BroadcastUtil;
import com.grupom2.wastemate.util.NavigationUtil;
import com.grupom2.wastemate.util.PermissionHelper;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;


public class MainActivity extends AppCompatActivity
{

    //region Fields
    //region Controls
    private TextView lblConnectedDeviceName;
    private TextView lblStatusDescription;
    private TextView lblCurrentPercentageHeader;
    private TextView lblCurrentPercentageDescription;
    private Button btnStartMaintenance;
    private Button btnCompleteMaintenance;
    private Button btnDisable;
    private ImageButton btnRefresh;
    private ImageView btnSettings;
    //endregion Controls

    //region Listeners
    private final View.OnClickListener btnSettingsOnClickListener;
    private final View.OnClickListener btnRefreshOnClickListener;
    private final View.OnClickListener btnStartMaintenanceOnClickListener;
    private final View.OnClickListener btnCompleteMaintenanceOnClickListener;
    private final View.OnClickListener btnDisableOnClickListener;
    //endregion Listeners

    //region BroadcastReceivers
    private final BluetoothDisabledBroadcastReceiver bluetoothDisabledBroadcastReceiver;
    private final NoDeviceConnectedBroadcastReceiver noDeviceConnectedBroadcastReceiver;
    private final DeviceConnectedBroadcastReceiver deviceConnectedBroadcastReceiver;
    private final ServiceStartedBroadcastReceiver serviceStartedBroadcastReceiver;
    private final UpdateStatusBroadcastReceiver updateStatusBroadcastReceiver;
    private final DeviceDisconnectedBroadcastReceiver bluetoothDeviceDisconnectedReceiver;
    private final SafeBroadcastReceiver deviceUnsupportedBroadcastReceiver;
    //endregion BroadcastReceivers

    //region Other Fields
    private BluetoothManager bluetoothManager;
    private boolean isInitialized;


    //endregion Other Fields
    //endregion Fields

    //region Constructor
    public MainActivity()
    {
        btnSettingsOnClickListener = MainActivity::btnSettingsOnClickListener;
        btnRefreshOnClickListener = MainActivity::btnRefreshOnClickListener;
        btnStartMaintenanceOnClickListener = MainActivity::btnStartMaintenanceOnClickListener;
        btnCompleteMaintenanceOnClickListener = MainActivity::btnCompleteMaintenanceOnClickListener;
        btnDisableOnClickListener = MainActivity::btnDisableOnClickListener;
        bluetoothDisabledBroadcastReceiver = new BluetoothDisabledBroadcastReceiver();
        noDeviceConnectedBroadcastReceiver = new NoDeviceConnectedBroadcastReceiver();
        serviceStartedBroadcastReceiver = new ServiceStartedBroadcastReceiver();
        updateStatusBroadcastReceiver = new UpdateStatusBroadcastReceiver();
        deviceConnectedBroadcastReceiver = new DeviceConnectedBroadcastReceiver();
        bluetoothDeviceDisconnectedReceiver = new DeviceDisconnectedBroadcastReceiver();
        deviceUnsupportedBroadcastReceiver = new DeviceUnsupportedBroadcastReceiver();

        isInitialized = false;
    }
    //endregion Constructor

    //region Overrides
    //region Activity Life Cycle
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //Se asigna un layout a la activity para poder vincular los distintos componentes.
        setContentView(R.layout.activity_main);

        // Se vinculan los controles a su correspondiente layout y se les asignan los listeners.
        doCreate();

        //Si tiene todos los permisos necesarios para funcionar, inicializar el servicio y registrar los broadcast receivers.
        if (PermissionHelper.checkPermissions(this))
        {
            initialize();
        }
        else
        {
            //Si le falta algún permiso, mostrar como desconectado.
            showDisconnected();
        }
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();

        //Cuando la activity ya fue creada previamente y se le vuelve a hacer foco, se deben verificar
        //nuevamente los permisos en caso de que alguno haya sido revocado.
        if (PermissionHelper.checkPermissions(this))
        {
            //Si tiene todos los permisos necesarios, continúa.


            //Si está iniciado el servicio y no se encuentra un adapter habilitado, es porque no está habilitado el bluetooth.
            //Se redirige a otra activity porque la aplicación no puede funcionar sin bluetooth.
            if (bluetoothManager != null && !bluetoothManager.isEnabled())
            {
                NavigationUtil.navigateToBluetoothRequiredActivity(this);
            }

            //Si aún no fue inicializada, iniciar el servicio y registrar todos los broadcast receivers.
            if (!isInitialized)
            {
                initialize();
            }
            else
            {
                //Se registran los broadcast listeners que sólo deben estar activos mientras la activity tenga foco.
                BroadcastUtil.registerLocalReceiver(this, noDeviceConnectedBroadcastReceiver, Actions.LOCAL_ACTION_NO_DEVICE_CONNECTED);
                BroadcastUtil.registerReceiver(this, bluetoothDisabledBroadcastReceiver);
                BroadcastUtil.registerLocalReceiver(this, deviceUnsupportedBroadcastReceiver, Actions.LOCAL_ACTION_UNSUPPORTED_DEVICE, Actions.LOCAL_ACTION_CONNECTION_CANCELED);
            }
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        //Se eliminan los broadcast listeners que sólo deben estar activos mientras la activity tenga foco.
        BroadcastUtil.unregisterLocalReceiver(this, noDeviceConnectedBroadcastReceiver);
        BroadcastUtil.unregisterReceiver(this, bluetoothDisabledBroadcastReceiver);
        BroadcastUtil.unregisterLocalReceiver(this, deviceUnsupportedBroadcastReceiver);

    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        //Se eliminan los broadcast listeners que deben estar activos mientras la activity no sea destruida.
        BluetoothService.stopService(getApplicationContext());
        BroadcastUtil.unregisterLocalReceiver(this, serviceStartedBroadcastReceiver);
        BroadcastUtil.unregisterLocalReceiver(this, updateStatusBroadcastReceiver);
        BroadcastUtil.unregisterReceiver(this, bluetoothDeviceDisconnectedReceiver);
    }
    //endregion Activity Life Cycle

    //region Other Overrides

    //Se ejecuta como respuesta de ActivityCompat.requestPermissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Constants.MULTIPLE_PERMISSIONS)
        {

            ArrayList<String> deniedPermissions = new ArrayList<>();
            boolean hasDeniedPermissions = PermissionHelper.hasDeniedPermissions(permissions, grantResults, deniedPermissions);

            //Si tiene todos los permisos necesarios, continuar.
            if (!hasDeniedPermissions)
            {
                //Si aún no se inicializó, iniciar el servicio y registrar los broadcast receivers correspondientes.
                if (!isInitialized)
                {
                    initialize();
                }
            }
            else
            {
                //Si falta algún permiso, ir a la pantalla de permisos.
                NavigationUtil.navigateToMissingPermissionsActivity(this, deniedPermissions);
            }
        }
    }
    //endregion Other Overrides
    //endregion Overrides

    //region Listeners
    private static void btnRefreshOnClickListener(View view)
    {
        //Enviar un mensaje al Arduino para refrescar el estado.
        BluetoothMessage bluetoothMessage = new BluetoothMessage(Constants.CODE_UPDATE_REQUESTED);
        BluetoothService.getInstance().write(bluetoothMessage);
    }

    private static void btnSettingsOnClickListener(View view)
    {
        //Ir a la activity de configuración.
        NavigationUtil.navigateToSettingsActivity(view.getContext());
    }

    private static void btnStartMaintenanceOnClickListener(View view)
    {
        //Enviar un mensaje al Arduino para iniciar mantenimiento.
        BluetoothMessage bluetoothMessage = new BluetoothMessage(Constants.CODE_MAINTENANCE_STARTED);
        BluetoothService.getInstance().write(bluetoothMessage);
    }

    private static void btnCompleteMaintenanceOnClickListener(View view)
    {
        //Enviar un mensaje al Arduino para completar el mantenimiento.
        BluetoothMessage bluetoothMessage = new BluetoothMessage(Constants.CODE_MAINTENANCE_COMPLETED);
        BluetoothService.getInstance().write(bluetoothMessage);
    }

    private static void btnDisableOnClickListener(View view)
    {
        //Enviar un mensaje al Arduino para deshabilitarlo.
        BluetoothMessage bluetoothMessage = new BluetoothMessage(Constants.CODE_DISABLE);
        BluetoothService.getInstance().write(bluetoothMessage);
    }
    //endregion Listeners

    //region Broadcast Receivers
    private class NoDeviceConnectedBroadcastReceiver extends SafeBroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            //Se necesita Bluetooth para interactuar con la aplicación, si no hay un dispositivo recordado, mostrar una alerta.
            showDisconnected();
            showBluetoothConnectionMissingDialog();
        }

        //Crear la alerta de "Conexión Bluetooth Requerida".
        private void showBluetoothConnectionMissingDialog()
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.bluetooth_connection_required);
            builder.setMessage(R.string.bluetooth_connection_required_message);
            builder.setPositiveButton(R.string.button_go_to_settings, (dialog, which) ->
                    NavigationUtil.navigateToSettingsActivity(MainActivity.this));
            builder.setNegativeButton(R.string.button_ignore, null);
            builder.setCancelable(true);
            builder.show();
        }
    }

    private class DeviceConnectedBroadcastReceiver extends SafeBroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            //Al conectarse a un dispositivo, habilitar la interfaz y registrar ese dispositivo en SharedPreferences.
            showEnabled();
            bluetoothManager.saveLastConnectedDevice(bluetoothManager.getConnectedDeviceAddress());
            lblConnectedDeviceName.setText(bluetoothManager.getConnectedDeviceName());
            BluetoothDeviceData data = BroadcastUtil.getData(intent, BluetoothDeviceData.class);
            if (data != null)
            {
                updateLabels(data);
            }
        }
    }

    private class DeviceDisconnectedBroadcastReceiver extends SafeBroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            //Si se desconecta el dispositivo conectado, deshabilitar la interfaz.
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (bluetoothManager.isLastDeviceConnected(device))
            {
                showDisconnected();
            }
        }
    }

    private class UpdateStatusBroadcastReceiver extends SafeBroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            //Cuando se recibe un mensaje de actualización de estado, actualizar la interfaz según corresponda.
            lblConnectedDeviceName.setText(bluetoothManager.getConnectedDeviceName());
            BluetoothDeviceData data = BroadcastUtil.getData(intent, BluetoothDeviceData.class);
            if (data != null)
            {
                updateLabels(data);
            }
        }
    }

    private class ServiceStartedBroadcastReceiver extends SafeBroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            //Cuando se completó la creación del servicio, obtener el singleton.
            bluetoothManager = BluetoothService.getInstance();
            //Se requiere bluetooth. Si no está activado ir a la activity que lo maneja.
            if (!bluetoothManager.isEnabled())
            {
                NavigationUtil.navigateToBluetoothRequiredActivity(MainActivity.this);
            }
        }
    }

    //Si el dispositivo no es compatible o no está disponible, actualizar la interfaz.
    private class DeviceUnsupportedBroadcastReceiver extends SafeBroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Toast.makeText(getApplicationContext(), "Dispositivo no disponible", Toast.LENGTH_SHORT).show();
            showDisconnected();
        }
    }
    //endregion Broadcast Receivers

    //region Other Methods

    //Vincular los controles y configurar los listeners.
    private void doCreate()
    {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btnSettings = findViewById(R.id.button_settings);
        btnSettings.setEnabled(false);
        btnSettings.setOnClickListener(btnSettingsOnClickListener);

        lblStatusDescription = findViewById(R.id.label_status_description);

        lblCurrentPercentageHeader = findViewById(R.id.label_current_percentage_header);
        lblCurrentPercentageDescription = findViewById(R.id.label_current_percentage_description);

        lblConnectedDeviceName = findViewById(R.id.label_connected_device_name);

        btnRefresh = findViewById(R.id.button_refresh_status);
        btnRefresh.setOnClickListener(btnRefreshOnClickListener);

        btnStartMaintenance = findViewById(R.id.button_start_maintenance);
        btnStartMaintenance.setOnClickListener(btnStartMaintenanceOnClickListener);

        btnCompleteMaintenance = findViewById(R.id.button_complete_maintenance);
        btnCompleteMaintenance.setOnClickListener(btnCompleteMaintenanceOnClickListener);

        btnDisable = findViewById(R.id.button_disable);
        btnDisable.setOnClickListener(btnDisableOnClickListener);
    }

    //Registrar los receivers e iniciar el servicio.
    private void initialize()
    {
        isInitialized = true;
        btnSettings.setEnabled(true);
        // Se registran los broadcast receiver que deben estar mientras la activity no sea destruida.
        BroadcastUtil.registerLocalReceiver(this, serviceStartedBroadcastReceiver, Actions.LOCAL_ACTION_SERVICE_CONNECTED);
        BroadcastUtil.registerLocalReceiver(this, updateStatusBroadcastReceiver, Actions.ARDUINO_ACTION_UPDATE);
        BroadcastUtil.registerLocalReceiver(this, deviceConnectedBroadcastReceiver, Actions.ARDUINO_ACTION_ACK);
        BroadcastUtil.registerReceiver(this, bluetoothDeviceDisconnectedReceiver, BluetoothDevice.ACTION_ACL_DISCONNECTED);
        BroadcastUtil.registerLocalReceiver(this, noDeviceConnectedBroadcastReceiver, Actions.LOCAL_ACTION_NO_DEVICE_CONNECTED);
        BroadcastUtil.registerReceiver(this, bluetoothDisabledBroadcastReceiver);
        BroadcastUtil.registerLocalReceiver(this, deviceUnsupportedBroadcastReceiver, Actions.LOCAL_ACTION_UNSUPPORTED_DEVICE, Actions.LOCAL_ACTION_CONNECTION_CANCELED);

        showConnecting();

        /* Se inicia el proceso de creación del servicio.
           Esto todavía no garantiza que el servicio esté creado. */
        startService();
    }

    private void startService()
    {
        BluetoothService.startService(getApplicationContext());
    }

    //Actualizar los labels que corresponden al estado del Arduino.
    void updateLabels(BluetoothDeviceData deviceData)
    {
        Status currentStatus = Arrays.stream(Status.values())
                .filter(status -> Objects.equals(status.getValue(), deviceData.getStatus()))
                .findFirst()
                .orElse(Status.ERROR);
        lblStatusDescription.setText(currentStatus.getDisplayName(this));
        lblCurrentPercentageDescription.setText(new DecimalFormat("#0.00%").format(deviceData.getCurrentPercentage() / 100));
        int color = currentStatus.getDisplayColor(this);
        lblCurrentPercentageDescription.setTextColor(color);
        lblStatusDescription.setTextColor(color);

        switch (currentStatus)
        {
            case CRITICAL_CAPACITY:
                btnStartMaintenance.setEnabled(false);
                btnCompleteMaintenance.setEnabled(false);
                btnDisable.setEnabled(true);
                break;
            case IN_MAINTENANCE:
                btnStartMaintenance.setEnabled(false);
                btnCompleteMaintenance.setEnabled(true);
                btnDisable.setEnabled(false);
                break;
            case NO_CAPACITY:
                btnStartMaintenance.setEnabled(true);
                btnCompleteMaintenance.setEnabled(false);
                btnDisable.setEnabled(false);
                break;
            case UNFILLED:
            case NOT_CONNECTED:
            case ERROR:
                btnStartMaintenance.setEnabled(false);
                btnCompleteMaintenance.setEnabled(false);
                btnDisable.setEnabled(false);
                break;
        }
    }

    //Mostrar la interfaz para Desconectado.
    private void showDisconnected()
    {
        lblStatusDescription.setText(R.string.lbl_status_description_disconnected);
        lblStatusDescription.setTextColor(getApplicationContext().getColor(R.color.grey));
        lblConnectedDeviceName.setText(R.string.lbl_connected_device_description_no_device_connected);
        lblCurrentPercentageDescription.setVisibility(View.INVISIBLE);
        lblCurrentPercentageHeader.setVisibility(View.INVISIBLE);
        btnStartMaintenance.setEnabled(false);
        btnCompleteMaintenance.setEnabled(false);
        btnDisable.setEnabled(false);
        btnRefresh.setEnabled(false);
    }

    //Mostrar la interfaz para Conectando.
    private void showConnecting()
    {
        lblStatusDescription.setText(R.string.lbl_status_description_connecting);
        lblStatusDescription.setTextColor(getApplicationContext().getColor(R.color.teal_700));
        lblConnectedDeviceName.setText(R.string.lbl_connected_device_description_non_recognized);
        lblCurrentPercentageDescription.setVisibility(View.INVISIBLE);
        lblCurrentPercentageHeader.setVisibility(View.INVISIBLE);
        btnStartMaintenance.setEnabled(false);
        btnCompleteMaintenance.setEnabled(false);
        btnDisable.setEnabled(false);
        btnRefresh.setEnabled(false);
    }

    //Habilitar los controles
    public void showEnabled()
    {
        lblCurrentPercentageHeader.setVisibility(View.VISIBLE);
        lblCurrentPercentageDescription.setVisibility(View.VISIBLE);
        btnRefresh.setEnabled(true);
    }
    //endregion Other Methods
}