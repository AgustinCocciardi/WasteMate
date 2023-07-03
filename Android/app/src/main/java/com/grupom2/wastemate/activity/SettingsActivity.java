package com.grupom2.wastemate.activity;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.grupom2.wastemate.R;
import com.grupom2.wastemate.adapter.BaseDeviceListAdapter;
import com.grupom2.wastemate.adapter.PairedDeviceListAdapter;
import com.grupom2.wastemate.bluetooth.BluetoothManager;
import com.grupom2.wastemate.bluetooth.BluetoothService;
import com.grupom2.wastemate.constant.Actions;
import com.grupom2.wastemate.constant.Constants;
import com.grupom2.wastemate.model.BluetoothDeviceData;
import com.grupom2.wastemate.model.BluetoothMessage;
import com.grupom2.wastemate.receiver.BluetoothDisabledBroadcastReceiver;
import com.grupom2.wastemate.receiver.SafeBroadcastReceiver;
import com.grupom2.wastemate.util.BroadcastUtil;
import com.grupom2.wastemate.util.CalibrationHelpers;
import com.grupom2.wastemate.util.CustomProgressDialog;
import com.grupom2.wastemate.util.NavigationUtil;
import com.grupom2.wastemate.util.PermissionHelper;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity implements SensorEventListener
{

    //region Fields

    //region Controls
    private CustomProgressDialog customProgressDialog;

    //region Admin Settings
    private ConstraintLayout adminSettingsLayout;
    private TextView txtMaximumWeight;
    private TextView txtCriticalPercentage;
    private TextView txtFullPercentage;
    private Button btnStartCalibration;
    private Spinner spinnerSensors;
    //endregion Admin Settings

    //region Main Settings
    private ConstraintLayout mainSettingsLayout;
    private PairedDeviceListAdapter pairedDevicesAdapter;
    private RecyclerView pairedDevicesRecyclerView;
    private BaseDeviceListAdapter<BaseDeviceListAdapter.BaseDeviceViewHolder> availableDevicesAdapter;
    //endregion Main Settings
    //endregion Controls

    //region Listeners
    private final View.OnClickListener toolbarButtonBackOnClickListener;

    //region Main Settings
    private final BaseDeviceListAdapter.OnClickListener pairedListItemOnUnpairListener;
    private final PairedDeviceListAdapter.OnClickListener pairedDeviceListItemOnClickListener;
    private final BaseDeviceListAdapter.OnClickListener unpairedListItemOnClickListener;
    private TextView lblShake;
    //endregion Main Settings

    //region Admin Settings
    private final View.OnClickListener btnStartCalibrationOnClickListener;
    private final View.OnClickListener btnSendSettingsOnClickListener;
    //endregion Admin Settings
    //endregion Listeners

    //region Broadcast Receivers
    private final BluetoothDisabledBroadcastReceiver bluetoothDisabledBroadcastReceiver;
    private final BluetoothDeviceFoundReceiver bluetoothDeviceFoundReceiver;
    private final BluetoothDeviceConnectedBroadcastReceiver deviceConnectedBroadcastReceiver;
    private final BluetoothDeviceDisconnectedReceiver bluetoothDeviceDisconnectedReceiver;
    private final DeviceUnsupportedBroadcastReceiver deviceUnsupportedBroadcastReceiver;
    private final BluetoothDeviceBondStateChangedReceiver bluetoothDeviceBondStateChangedReceiver;
    private final BluetoothConnectionCanceledBroadcastReceiver bluetoothConnectionCanceledBroadcastReceiver;
    private final BluetoothCalibrationFinishedBroadcastReceiver bluetoothCalibrationFinishedBroadcastReceiver;
    //endregion Broadcast Receivers

    //region Other Fields
    private static final float PRECISION_CHANGE = 20;
    private SensorManager sensor;
    private boolean showingAdminSettings;
    private boolean isSensorRegistered;
    private BluetoothManager bluetoothManager;
    private ArrayList<BluetoothDevice> pairedDevices;
    private ArrayList<BluetoothDevice> availableDevices;
    //endregion Other Fields

    //endregion Fields

    //region Constructor
    public SettingsActivity()
    {
        toolbarButtonBackOnClickListener = this::toolbarButtonBackOnClickListener;

        pairedListItemOnUnpairListener = this::pairedListItemOnUnpairListener;
        pairedDeviceListItemOnClickListener = this::pairedDeviceListItemOnClickListener;
        unpairedListItemOnClickListener = this::unpairedListItemOnClickListener;
        btnSendSettingsOnClickListener = this::btnSendSettingsOnClickListener;
        btnStartCalibrationOnClickListener = this::btnStartCalibrationOnClickListener;

        bluetoothDisabledBroadcastReceiver = new BluetoothDisabledBroadcastReceiver();
        bluetoothDeviceFoundReceiver = new BluetoothDeviceFoundReceiver();
        deviceConnectedBroadcastReceiver = new BluetoothDeviceConnectedBroadcastReceiver();
        bluetoothDeviceDisconnectedReceiver = new BluetoothDeviceDisconnectedReceiver();
        deviceUnsupportedBroadcastReceiver = new DeviceUnsupportedBroadcastReceiver();
        bluetoothDeviceBondStateChangedReceiver = new BluetoothDeviceBondStateChangedReceiver();
        bluetoothConnectionCanceledBroadcastReceiver = new BluetoothConnectionCanceledBroadcastReceiver();
        bluetoothCalibrationFinishedBroadcastReceiver = new BluetoothCalibrationFinishedBroadcastReceiver();
    }
    //endregion Constructor

    //region Overrides
    //region Activity Life Cycle
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        try
        {
            super.onCreate(savedInstanceState);

            //Se asigna un layout al activity para poder vincular los distintos componentes
            setContentView(R.layout.activity_settings);

            sensor = (SensorManager) getSystemService(SENSOR_SERVICE);
            showingAdminSettings = false;
            bluetoothManager = BluetoothService.getInstance();
            pairedDevices = new ArrayList<>();
            availableDevices = new ArrayList<>();

            customProgressDialog = new CustomProgressDialog(SettingsActivity.this);

            //region Toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            ImageView toolbarButtonBack = findViewById(R.id.toolbar_button_back);
            toolbarButtonBack.setOnClickListener(toolbarButtonBackOnClickListener);
            setSupportActionBar(toolbar);
            //endregion Toolbar

            onCreateMainSettings();

            onCreateAdminSettings();

            BroadcastUtil.registerReceiver(this, bluetoothDisabledBroadcastReceiver);
            BroadcastUtil.registerReceiver(this, bluetoothDeviceFoundReceiver, BluetoothDevice.ACTION_FOUND);
            BroadcastUtil.registerLocalReceiver(this, deviceConnectedBroadcastReceiver, Actions.ACTION_ACK);
            BroadcastUtil.registerReceiver(this, bluetoothDeviceDisconnectedReceiver, BluetoothDevice.ACTION_ACL_DISCONNECTED);
            BroadcastUtil.registerLocalReceiver(this, deviceUnsupportedBroadcastReceiver, Actions.ACTION_UNSUPPORTED_DEVICE);
            BroadcastUtil.registerReceiver(this, bluetoothDeviceBondStateChangedReceiver, BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            BroadcastUtil.registerLocalReceiver(this, bluetoothConnectionCanceledBroadcastReceiver, Actions.ACTION_CONNECTION_CANCELED);
            BroadcastUtil.registerLocalReceiver(this, bluetoothCalibrationFinishedBroadcastReceiver, Actions.ACTION_CALIBRATION_FINISHED);
            bluetoothManager.startDiscovery();
        }
        catch (SecurityException e)
        {
            NavigationUtil.navigateToSettingsActivity(null);
        }
    }

    @Override
    public void onDestroy()
    {
        BroadcastUtil.unregisterReceiver(this, bluetoothDeviceFoundReceiver);
        BroadcastUtil.unregisterReceiver(this, bluetoothDeviceBondStateChangedReceiver);
        BroadcastUtil.unregisterReceiver(this, bluetoothDeviceDisconnectedReceiver);
        BroadcastUtil.unregisterReceiver(this, bluetoothDisabledBroadcastReceiver);
        BroadcastUtil.unregisterLocalReceiver(this, deviceConnectedBroadcastReceiver);
        BroadcastUtil.unregisterLocalReceiver(this, deviceUnsupportedBroadcastReceiver);
        BroadcastUtil.unregisterLocalReceiver(this, bluetoothConnectionCanceledBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (PermissionHelper.checkPermissions(this))
        {
            if (bluetoothManager != null)
            {
                if (!bluetoothManager.isEnabled())
                {
                    NavigationUtil.navigateToBluetoothRequiredActivity(this);
                }

                if (bluetoothManager.isConnected())
                {
                    registerSensor();
                }
            }
        }
    }

    @Override
    protected void onStop()
    {

        super.onStop();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterSensor();
    }
    //endregion Activity Life Cycle

    //region Other Overrides
    @Override
    public void onBackPressed()
    {
        if (showingAdminSettings)
        {
            adminSettingsLayout.setVisibility(View.GONE);
            mainSettingsLayout.setVisibility(View.VISIBLE);
            showingAdminSettings = false;
        }
        else
        {
            super.onBackPressed();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        int sensorType = sensorEvent.sensor.getType();
        float[] sensorValues = sensorEvent.values;
        if (sensorType == Sensor.TYPE_ACCELEROMETER)
        {
            if ((Math.abs(sensorValues[0]) > PRECISION_CHANGE || Math.abs(sensorValues[1]) > PRECISION_CHANGE || Math.abs(sensorValues[2]) > PRECISION_CHANGE))
            {
                adminSettingsLayout.setVisibility(View.VISIBLE);
                mainSettingsLayout.setVisibility(View.GONE);
                showingAdminSettings = true;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }
    //endregion Other Overrides
    //endregion Overrides

    //region Listeners
    private void unpairedListItemOnClickListener(int position, BluetoothDevice model)
    {
        try
        {
            if (model.getBondState() == BluetoothDevice.BOND_NONE)
            {
                try
                {
                    Method method = model.getClass().getMethod(Constants.BLUETOOTH_DEVICE_METHOD_CREATE_BOND, (Class[]) null);
                    method.invoke(model, (Object[]) null);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
        catch (SecurityException e)
        {
            NavigationUtil.navigateToMissingPermissionsActivity(SettingsActivity.this, null);
        }
    }

    private void pairedListItemOnUnpairListener(int position, BluetoothDevice model)
    {
        try
        {
            if (model.getBondState() == BluetoothDevice.BOND_BONDED)
            {
                try
                {
                    disconnect();
                    Method method = model.getClass().getMethod(Constants.BLUETOOTH_DEVICE_METHOD_REMOVE_BOND, (Class[]) null);
                    method.invoke(model, (Object[]) null);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
        catch (SecurityException e)
        {
            NavigationUtil.navigateToMissingPermissionsActivity(SettingsActivity.this, null);
        }
    }

    private void disconnect()
    {
        bluetoothManager.removeLastConnectedDevice();
        bluetoothManager.disconnectAndForget();
    }

    private void btnStartCalibrationOnClickListener(View v)
    {
        Spinner spinnerSensors = findViewById(R.id.spinner_sensors);
        btnStartCalibration.setEnabled(false);
        spinnerSensors.setEnabled(false);
        int sensorCalibrationCode = CalibrationHelpers.sensorsDictionary.get(spinnerSensors.getSelectedItem().toString());
        bluetoothManager.setCalibrating();
        BluetoothMessage message = new BluetoothMessage(sensorCalibrationCode);
        bluetoothManager.write(message);
    }

    private void btnSendSettingsOnClickListener(View v)
    {
        if (!validateField(txtMaximumWeight, Constants.MAXIMUM_WEIGHT_MIN_VALUE, Constants.MAXIMUM_WEIGHT_MAX_VALUE) |
                !validateField(txtCriticalPercentage, Constants.CRITICAL_CAPACITY_MIN_VALUE, Constants.CRITICAL_CAPACITY_MAX_VALUE) |
                !validateField(txtFullPercentage, Constants.FULL_CAPACITY_MIN_VALUE, Constants.FULL_CAPACITY_MAX_VALUE))
        {
            return;
        }

        int weightLimit = Integer.parseInt(txtMaximumWeight.getText().toString());
        double minimumDistance = Integer.parseInt(txtFullPercentage.getText().toString()) / 100.0; //TODO: sacar numero mágico
        double criticalDistance = Integer.parseInt(txtCriticalPercentage.getText().toString()) / 100.0;
        BluetoothMessage message = new BluetoothMessage(Constants.CODE_CONFIGURE_THRESHOLDS, weightLimit, minimumDistance, criticalDistance);
        BluetoothService.getInstance().write(message);

        adminSettingsLayout.setVisibility(View.GONE);
        mainSettingsLayout.setVisibility(View.VISIBLE);
        showingAdminSettings = false;
    }

    private void toolbarButtonBackOnClickListener(View v)
    {

        onBackPressed();
    }

    private void pairedDeviceListItemOnClickListener(int position, BluetoothDevice device)
    {
        customProgressDialog.show();
        if (bluetoothManager != null)
        {
            if (bluetoothManager.getBluetoothConnection().isConnected(device))
            {
                disconnect();
            }
            else
            {
                bluetoothManager.connectToDevice(device.getAddress());
            }
        }
    }
    //endregion Listeners

    //region Broadcast Receivers
    private class BluetoothDeviceFoundReceiver extends SafeBroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            //Se lo agregan sus datos a una lista de dispositivos encontrados
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (!pairedDevices.contains(device))
            {
                int i = availableDevices.indexOf(device);
                if (i >= 0)
                {
                    availableDevices.set(i, device);
                }
                else
                {
                    availableDevices.add(device);
                }
                availableDevicesAdapter.setData(availableDevices);
                availableDevicesAdapter.notifyDataSetChanged();
            }
        }
    }

    private class BluetoothDeviceConnectedBroadcastReceiver extends SafeBroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            BluetoothDeviceData data = BroadcastUtil.getData(intent, BluetoothDeviceData.class);
            handleDeviceConnectionStatus(bluetoothManager.getConnectedDeviceAddress(), true);
            customProgressDialog.dismiss();

            if (data != null)
            {
                initializeForm(data);
            }
            registerSensor();
        }
    }

    private class BluetoothDeviceDisconnectedReceiver extends SafeBroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            handleDeviceConnectionStatus(device.getAddress(), false);
            customProgressDialog.dismiss();
            unregisterSensor();
        }
    }

    private class DeviceUnsupportedBroadcastReceiver extends SafeBroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            customProgressDialog.dismiss();
            Toast.makeText(getApplicationContext(), "Dispositivo no soportado", Toast.LENGTH_SHORT).show();
        }
    }

    private class BluetoothDeviceBondStateChangedReceiver extends SafeBroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
            if (state == BluetoothDevice.BOND_BONDED)
            {
                availableDevices.remove(device);
                pairedDevices.add(device);
            }
            else if (state == BluetoothDevice.BOND_NONE)
            {
                availableDevices.add(device);
                pairedDevices.remove(device);
            }
            availableDevicesAdapter.setData(availableDevices);
            pairedDevicesAdapter.setData(pairedDevices);
            availableDevicesAdapter.notifyDataSetChanged();
            pairedDevicesAdapter.notifyDataSetChanged();
        }
    }

    private class BluetoothConnectionCanceledBroadcastReceiver extends SafeBroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Toast.makeText(SettingsActivity.this, "Error en la conexión", Toast.LENGTH_SHORT);
        }
    }

    private class BluetoothCalibrationFinishedBroadcastReceiver extends SafeBroadcastReceiver
    {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            btnStartCalibration.setEnabled(true);
            spinnerSensors.setEnabled(true);
        }
    }

    //endregion Broadcast Receivers
    //region Other Methods
    private void onCreateAdminSettings()
    {
        //region Admin Settings
        adminSettingsLayout = findViewById(R.id.layoutAdminSettings);

        txtMaximumWeight = findViewById(R.id.txt_weight_limit);
        //txtMaximumWeight.setFilters(new InputFilter[]{new MinMaxFilter(1, 3)});

        txtCriticalPercentage = findViewById(R.id.txt_critical_percentage);
        //txtCriticalPercentage.setFilters(new InputFilter[]{new MinMaxFilter(50, 80)});

        txtFullPercentage = findViewById(R.id.txt_full_percentage);
        //txtFullPercentage.setFilters(new InputFilter[]{new MinMaxFilter(50, 90)});

        Button btnSendSettings = findViewById(R.id.button_send_settings);
        btnSendSettings.setOnClickListener(btnSendSettingsOnClickListener);


        btnStartCalibration = findViewById(R.id.button_start_calibration);
        btnStartCalibration.setOnClickListener(btnStartCalibrationOnClickListener);

        //region Spinner
        spinnerSensors = findViewById(R.id.spinner_sensors);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        adapter.addAll(CalibrationHelpers.sensorsDictionary.keySet());
        spinnerSensors.setAdapter(adapter);

        if (bluetoothManager != null)
        {
            BluetoothDeviceData bluetoothDeviceData = bluetoothManager.getDeviceData();
            initializeForm(bluetoothDeviceData);
            boolean available = !bluetoothDeviceData.getIsCalibrating();
            spinnerSensors.setEnabled(available);
            btnSendSettings.setEnabled(available);
        }
        //endregion Spinner
        //endregion Admin Settings
    }

    private void onCreateMainSettings() throws SecurityException
    {
        //region Main Settings
        mainSettingsLayout = findViewById(R.id.layoutMainSettings);

        lblShake = findViewById(R.id.label_shake);

        //region Paired Devices
        pairedDevicesRecyclerView = findViewById(R.id.recycler_view_paired);
        LinearLayoutManager pairedDevicesLayoutManager = new LinearLayoutManager(this);
        pairedDevicesLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        pairedDevicesRecyclerView.setLayoutManager(pairedDevicesLayoutManager);
        pairedDevicesAdapter = new PairedDeviceListAdapter(getApplicationContext());
        pairedDevicesAdapter.setOnItemClickedListener(pairedDeviceListItemOnClickListener);
        pairedDevicesAdapter.setOnUnpairDeviceClickListener(pairedListItemOnUnpairListener);
        pairedDevicesRecyclerView.setAdapter(pairedDevicesAdapter);
        //endregion Paired Devices

        //region Available Devices
        RecyclerView availableDevicesRecyclerView = findViewById(R.id.recycler_view_available);
        availableDevicesAdapter = new BaseDeviceListAdapter<>();
        LinearLayoutManager availableDevicesLayoutManager = new LinearLayoutManager(this);
        availableDevicesLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        availableDevicesRecyclerView.setLayoutManager(availableDevicesLayoutManager);
        availableDevicesAdapter.setOnItemClickedListener(unpairedListItemOnClickListener);
        availableDevicesRecyclerView.setAdapter(availableDevicesAdapter);
        //endregion

        Set<BluetoothDevice> bondedDevices = bluetoothManager.getBondedDevices();
        if (bondedDevices != null && bondedDevices.size() != 0)
        {
            pairedDevices.addAll(bondedDevices);
        }
        else
        {
            //TODO: MOSTRAR NO HAY DISPOSITIVOS?
        }
        pairedDevicesAdapter.setData(pairedDevices);
        availableDevicesAdapter.setData(availableDevices);
        //endregion Main Settings
    }

    private void handleDeviceConnectionStatus(String deviceAddress, boolean isConnected)
    {
        int index = findViewHolder(deviceAddress);
        if (index >= 0)
        {
            PairedDeviceListAdapter.PairedDeviceViewHolder viewHolder = (PairedDeviceListAdapter.PairedDeviceViewHolder) pairedDevicesRecyclerView.findViewHolderForAdapterPosition(index);
            if (viewHolder != null)
            {
                viewHolder.setConnectedIndicatorColor(getApplicationContext(), isConnected);
            }
        }
    }

    private int findViewHolder(String deviceAddress)
    {
        int index = -1;
        for (int i = 0; i < pairedDevices.size(); i++)
        {
            BluetoothDevice pairedDevice = pairedDevices.get(i);
            if (pairedDevice.getAddress().equals(deviceAddress))
            {
                index = i;
                break;
            }
        }
        return index;
    }

    private void unregisterSensor()
    {
        if (isSensorRegistered)
        {
            lblShake.setVisibility(View.GONE);
            sensor.unregisterListener(this);
            isSensorRegistered = false;
        }
    }

    private void registerSensor()
    {
        if (!isSensorRegistered)
        {
            lblShake.setVisibility(View.VISIBLE);
            sensor.registerListener(this, sensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
            isSensorRegistered = true;
        }
    }

    private boolean validateField(TextView txtMaximumWeight, int minValue, int maxValue)
    {
        String maximumWeightString = txtMaximumWeight.getText().toString();
        if (maximumWeightString.isEmpty())
        {
            txtMaximumWeight.setError("Campo obligatorio");
            return false;
        }
        int parsedMaximumWeight = Integer.parseInt(maximumWeightString);
        if (parsedMaximumWeight < minValue || parsedMaximumWeight > maxValue)
        {
            txtMaximumWeight.setError("El valor debe estar entre " + minValue + " y " + maxValue);
            return false;
        }
        return true;
    }

    private void initializeForm(BluetoothDeviceData deviceData)
    {
        setFormattedText(txtMaximumWeight, deviceData.getMaxAllowedWeight());
        setFormattedText(txtCriticalPercentage, deviceData.getCriticalPercentage() * 100);
        setFormattedText(txtFullPercentage, deviceData.getFullPercentage() * 100);
    }

    private void setFormattedText(TextView txtMaximumWeight, double deviceData)
    {
        DecimalFormat decimalFormat = new DecimalFormat("#");
        txtMaximumWeight.setText(decimalFormat.format(deviceData));
    }
    //endregion Other Methods

}