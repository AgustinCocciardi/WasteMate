package com.grupom2.wastemate.activity.mainactivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
    private TextView lblStatusDescription;
    private TextView lblCurrentPercentage;
    private Button btnStartMaintenance;
    private Button btnCompleteMaintenance;
    private Button btnDisable;
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
    private ServiceStartedBroadcastReceiver serviceStartedBroadcastReceiver;
    private final UpdateStatusBroadcastReceiver updateStatusBroadcastReceiver;
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
        BroadcastUtil.registerLocalReceiver(this, noDeviceConnectedBroadcastReceiver, Actions.ACTION_NO_DEVICE_CONNECTED);
//        if (BluetoothService.getInstance().getBluetoothConnection().isConnected())
//        {
//            BluetoothService.getInstance().getBluetoothConnection().getDevice() {
//
//        }
//        }
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
        BroadcastUtil.unregisterLocalReceiver(this, serviceStartedBroadcastReceiver);
        BroadcastUtil.unregisterReceiver(this, bluetoothDisabledBroadcastReceiver);
        BroadcastUtil.unregisterLocalReceiver(this, updateStatusBroadcastReceiver);
    }
    //endregion Activity Life Cycle

    //region Other Overrides
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Constants.MULTIPLE_PERMISSIONS)
        {

            ArrayList<String> deniedPermissions = new ArrayList<>();
            boolean hasDeniedPermissions = hasDeniedPermissions(permissions, grantResults, deniedPermissions);
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

    private static boolean hasDeniedPermissions(String[] permissions, int[] grantResults, @NonNull ArrayList<String> deniedPermissions)
    {
        for (int i = 0; i < permissions.length; i++)
        {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
            {
                deniedPermissions.add(permissions[i]);
            }
        }
        return deniedPermissions.isEmpty();
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

    //region Other Methods
    private void doCreate()
    {
        isInitialized = true;

        /* Se inicia el proceso de creación del servicio.
           Esto todavía no garantiza que el servicio esté creado. */
        startService();

        // Se vinculan los controles a su correspondiente layout y se les asignan los listeners.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        lblStatusDescription = findViewById(R.id.label_status_description);

        lblCurrentPercentage = findViewById(R.id.label_current_percentage_description);

        ImageButton btnRefresh = findViewById(R.id.button_refresh_status);
        btnRefresh.setOnClickListener(btnRefreshOnClickListener);

        ImageView btnSettings = findViewById(R.id.button_settings);
        btnSettings.setOnClickListener(btnSettingsOnClickListener);

        btnStartMaintenance = findViewById(R.id.button_start_maintenance);
        btnStartMaintenance.setOnClickListener(btnStartMaintenanceOnClickListener);

        btnCompleteMaintenance = findViewById(R.id.button_complete_maintenance);
        btnCompleteMaintenance.setOnClickListener(btnCompleteMaintenanceOnClickListener);

        btnDisable = findViewById(R.id.button_disable);
        btnDisable.setOnClickListener(btnDisableOnClickListener);

        // Se registran los broadcast receiver que deben estar mientras la activity no sea destruida.
        BroadcastUtil.registerLocalReceiver(this,
                serviceStartedBroadcastReceiver,
                Actions.ACTION_SERVICE_CONNECTED);

        BroadcastUtil.registerReceiver(this, bluetoothDisabledBroadcastReceiver);

        BroadcastUtil.registerLocalReceiver(this,
                updateStatusBroadcastReceiver,
                Actions.ACTION_UPDATE, Actions.ACTION_ACK);
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
        lblCurrentPercentage.setText(new DecimalFormat("#0.00%").format(deviceData.getCurrentPercentage()));
        int color = currentStatus.getDisplayColor(this);
        lblCurrentPercentage.setTextColor(color);
        lblStatusDescription.setTextColor(color);
    }

    private void showDisabled()
    {

    }
    //endregion Other Methods

    //region Broadcast Receivers
    private class NoDeviceConnectedBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            showBluetoothConnectionDialog();
        }

        private void showBluetoothConnectionDialog()
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.bluetooth_connection_required);
            builder.setMessage(R.string.bluetooth_connection_required_message);
            builder.setPositiveButton(R.string.button_go_to_settings, (dialog, which) ->
            {
                NavigationUtil.navigateToSettingsActivity(MainActivity.this);
            });
            builder.setNegativeButton(R.string.button_ignore, (dialog, which) ->
            {
                showDisabled();
            });
            builder.setCancelable(true); // Prevent the user from dismissing the dialog without enabling Bluetooth
            builder.show();
        }
    }

    private class UpdateStatusBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            BluetoothDeviceData data = BroadcastUtil.getData(intent, BluetoothDeviceData.class);
            updateLabels(data);
        }
    }

    private class ServiceStartedBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            customProgressDialog.dismiss();
            bluetoothManager = BluetoothService.getInstance();
        }
    }
    //endregion Broadcast Receivers
}