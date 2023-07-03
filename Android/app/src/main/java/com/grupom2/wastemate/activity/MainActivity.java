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
import com.grupom2.wastemate.util.CustomProgressDialog;
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
    CustomProgressDialog customProgressDialog;
    private TextView lblConnectedDeviceName;
    private TextView lblStatusDescription;
    private TextView lblCurrentPercentageHeader;
    private TextView lblCurrentPercentageDescription;
    private Button btnStartMaintenance;
    private Button btnCompleteMaintenance;
    private Button btnDisable;
    private ImageButton btnRefresh;
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
        isInitialized = false;
        deviceConnectedBroadcastReceiver = new DeviceConnectedBroadcastReceiver();
        bluetoothDeviceDisconnectedReceiver = new DeviceDisconnectedBroadcastReceiver();
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
        if (PermissionHelper.checkPermissions(this))
        {
            doCreate();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if(PermissionHelper.checkPermissions(this)){
            if (bluetoothManager != null && !bluetoothManager.isEnabled())
            {
                NavigationUtil.navigateToBluetoothRequiredActivity(this);
            }
            BroadcastUtil.registerLocalReceiver(this, noDeviceConnectedBroadcastReceiver, Actions.ACTION_NO_DEVICE_CONNECTED);
            BroadcastUtil.registerReceiver(this, bluetoothDisabledBroadcastReceiver);
        }

    }

    @Override
    protected void onStop()
    {
        super.onStop();
        BroadcastUtil.unregisterLocalReceiver(this, noDeviceConnectedBroadcastReceiver);

        BroadcastUtil.unregisterReceiver(this, bluetoothDisabledBroadcastReceiver);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        BluetoothService.stopService(getApplicationContext());
        BroadcastUtil.unregisterLocalReceiver(this, serviceStartedBroadcastReceiver);
        BroadcastUtil.unregisterLocalReceiver(this, updateStatusBroadcastReceiver);
        BroadcastUtil.unregisterReceiver(this, bluetoothDeviceDisconnectedReceiver);
    }
    //endregion Activity Life Cycle

    //region Other Overrides
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Constants.MULTIPLE_PERMISSIONS)
        {

            ArrayList<String> deniedPermissions = new ArrayList<>();
            boolean hasDeniedPermissions = PermissionHelper.hasDeniedPermissions(permissions, grantResults, deniedPermissions);
            if (!hasDeniedPermissions)
            {
                if (!isInitialized)
                {
                    doCreate();
                }
            }
            else
            {
                NavigationUtil.navigateToMissingPermissionsActivity(this, deniedPermissions);
            }
        }
    }
    //endregion Other Overrides
    //endregion Overrides

    //region Listeners
    private static void btnRefreshOnClickListener(View view)
    {
        BluetoothMessage bluetoothMessage = new BluetoothMessage(Constants.CODE_UPDATE_REQUESTED);
        BluetoothService.getInstance().write(bluetoothMessage);
    }

    private static void btnSettingsOnClickListener(View view)
    {
        NavigationUtil.navigateToSettingsActivity(view.getContext());
    }

    private static void btnStartMaintenanceOnClickListener(View view)
    {
        BluetoothMessage bluetoothMessage = new BluetoothMessage(Constants.CODE_MAINTENANCE_STARTED);
        BluetoothService.getInstance().write(bluetoothMessage);
    }

    private static void btnCompleteMaintenanceOnClickListener(View view)
    {
        BluetoothMessage bluetoothMessage = new BluetoothMessage(Constants.CODE_MAINTENANCE_COMPLETED);
        BluetoothService.getInstance().write(bluetoothMessage);
    }

    private static void btnDisableOnClickListener(View view)
    {
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
            showDisconnected();
            showBluetoothConnectionMissingDialog();
        }

        private void showBluetoothConnectionMissingDialog()
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.bluetooth_connection_required);
            builder.setMessage(R.string.bluetooth_connection_required_message);
            builder.setPositiveButton(R.string.button_go_to_settings, (dialog, which) ->
                    NavigationUtil.navigateToSettingsActivity(MainActivity.this));
            builder.setNegativeButton(R.string.button_ignore, (dialog, which) ->
            {
            });
            builder.setCancelable(false); // Prevent the user from dismissing the dialog without enabling Bluetooth
            builder.show();
        }
    }

    private class DeviceConnectedBroadcastReceiver extends SafeBroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
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
            showDisconnected();
        }
    }

    private class UpdateStatusBroadcastReceiver extends SafeBroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
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
            customProgressDialog.dismiss();
            bluetoothManager = BluetoothService.getInstance();
            if (!bluetoothManager.isEnabled())
            {
                NavigationUtil.navigateToBluetoothRequiredActivity(MainActivity.this);
            }
        }
    }
    //endregion Broadcast Receivers

    //region Other Methods
    private void doCreate()
    {
        isInitialized = true;

        // Se registran los broadcast receiver que deben estar mientras la activity no sea destruida.
        BroadcastUtil.registerLocalReceiver(this, serviceStartedBroadcastReceiver, Actions.ACTION_SERVICE_CONNECTED);
        BroadcastUtil.registerLocalReceiver(this, updateStatusBroadcastReceiver, Actions.ACTION_UPDATE);
        BroadcastUtil.registerLocalReceiver(this, deviceConnectedBroadcastReceiver, Actions.ACTION_ACK);
        BroadcastUtil.registerReceiver(this, bluetoothDeviceDisconnectedReceiver, BluetoothDevice.ACTION_ACL_DISCONNECTED);
        BroadcastUtil.registerLocalReceiver(this, noDeviceConnectedBroadcastReceiver, Actions.ACTION_NO_DEVICE_CONNECTED);
        BroadcastUtil.registerReceiver(this, bluetoothDisabledBroadcastReceiver);

        // Se vinculan los controles a su correspondiente layout y se les asignan los listeners.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        lblStatusDescription = findViewById(R.id.label_status_description);

        lblCurrentPercentageHeader = findViewById(R.id.label_current_percentage_header);
        lblCurrentPercentageDescription = findViewById(R.id.label_current_percentage_description);

        lblConnectedDeviceName = findViewById(R.id.label_connected_device_name);

        btnRefresh = findViewById(R.id.button_refresh_status);
        btnRefresh.setOnClickListener(btnRefreshOnClickListener);

        ImageView btnSettings = findViewById(R.id.button_settings);
        btnSettings.setOnClickListener(btnSettingsOnClickListener);

        btnStartMaintenance = findViewById(R.id.button_start_maintenance);
        btnStartMaintenance.setOnClickListener(btnStartMaintenanceOnClickListener);

        btnCompleteMaintenance = findViewById(R.id.button_complete_maintenance);
        btnCompleteMaintenance.setOnClickListener(btnCompleteMaintenanceOnClickListener);

        btnDisable = findViewById(R.id.button_disable);
        btnDisable.setOnClickListener(btnDisableOnClickListener);

        showConnecting();

        /* Se inicia el proceso de creación del servicio.
           Esto todavía no garantiza que el servicio esté creado. */
        startService();

    }

    private void startService()
    {
        BluetoothService.startService(getApplicationContext());
        customProgressDialog = new CustomProgressDialog(MainActivity.this);
        customProgressDialog.show();
    }

    void updateLabels(BluetoothDeviceData deviceData)
    {
        Status currentStatus = Arrays.stream(Status.values())
                .filter(status -> Objects.equals(status.getValue(), deviceData.getStatus()))
                .findFirst()
                .orElse(Status.ERROR);
        lblStatusDescription.setText(currentStatus.getDisplayName(this));
        lblCurrentPercentageDescription.setText(new DecimalFormat("#0.00%").format(deviceData.getCurrentPercentage()));
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

    public void showEnabled()
    {
        lblCurrentPercentageHeader.setVisibility(View.VISIBLE);
        lblCurrentPercentageDescription.setVisibility(View.VISIBLE);
        btnStartMaintenance.setEnabled(true);
        btnCompleteMaintenance.setEnabled(true);
        btnDisable.setEnabled(true);
        btnRefresh.setEnabled(true);
    }
    //endregion Other Methods
}