package com.grupom2.wastemate.activity;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
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
import com.grupom2.wastemate.util.BroadcastUtil;
import com.grupom2.wastemate.util.CalibrationHelpers;
import com.grupom2.wastemate.util.CustomProgressDialog;

import java.lang.reflect.Method;
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
    //endregion Broadcast Receivers

    //region Other Fields
    private static final float PRECISION_CHANGE = 20;
    private SensorManager sensor;
    private boolean showingAdminSettings;
    private boolean isSensorRegistered;
    private static BluetoothManager bluetoothService;
    private ArrayList<BluetoothDevice> pairedDevices;
    private ArrayList<BluetoothDevice> availableDevices;
    //endregion Other Fields

    //endregion Fields

    //region Constructor
    public SettingsActivity()
    {
        toolbarButtonBackOnClickListener = this::toolbarButtonBackOnClickListener;

        pairedListItemOnUnpairListener = SettingsActivity::pairedListItemOnUnpairListener;
        pairedDeviceListItemOnClickListener = this::pairedDeviceListItemOnClickListener;
        unpairedListItemOnClickListener = SettingsActivity::unpairedListItemOnClickListener;
        btnSendSettingsOnClickListener = this::btnSendSettingsOnClickListener;
        btnStartCalibrationOnClickListener = this::btnStartCalibrationOnClickListener;

        bluetoothDisabledBroadcastReceiver = new BluetoothDisabledBroadcastReceiver();
        bluetoothDeviceFoundReceiver = new BluetoothDeviceFoundReceiver();
        deviceConnectedBroadcastReceiver = new BluetoothDeviceConnectedBroadcastReceiver();
        bluetoothDeviceDisconnectedReceiver = new BluetoothDeviceDisconnectedReceiver();
        deviceUnsupportedBroadcastReceiver = new DeviceUnsupportedBroadcastReceiver();
        bluetoothDeviceBondStateChangedReceiver = new BluetoothDeviceBondStateChangedReceiver();
    }
    //endregion Constructor

    //region Overrides
    //region Activity Life Cycle
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //Se asigna un layout al activity para poder vincular los distintos componentes
        setContentView(R.layout.activity_settings);

        sensor = (SensorManager) getSystemService(SENSOR_SERVICE);
        showingAdminSettings = false;
        bluetoothService = BluetoothService.getInstance();
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

        //TODO: VALIDAR PERMISOS.
        bluetoothService.startDiscovery();
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
        super.onDestroy();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (bluetoothService.isConnected())
        {
            registerSensor();
        }
    }

    private void registerSensor()
    {
        if (!isSensorRegistered)
        {
            sensor.registerListener(this, sensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
            isSensorRegistered = true;
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
    private static void unpairedListItemOnClickListener(int position, BluetoothDevice model)
    {
        //TODO: VALIDAR PERMISOS?
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

    private static void pairedListItemOnUnpairListener(int position, BluetoothDevice model)
    {
        //TODO: VALIDAR PERMISOS?
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

    private static void disconnect()
    {
        bluetoothService.removeLastConnectedDevice();
        bluetoothService.disconnectAndForget();
    }

    private void btnStartCalibrationOnClickListener(View v)
    {
        Spinner spinnerSensores = (Spinner) findViewById(R.id.spinner_sensors);
        int sensorCalibrationCode = CalibrationHelpers.sensorsDictionary.get(spinnerSensores.getSelectedItem().toString());
        BluetoothMessage message = new BluetoothMessage(sensorCalibrationCode);
        BluetoothService.getInstance().write(message);
    }

    private void btnSendSettingsOnClickListener(View v)
    {
        int weightLimit = Integer.parseInt(txtMaximumWeight.getText().toString());
        double minimumDistance = Integer.parseInt(txtFullPercentage.getText().toString()) / 100.0; //TODO: sacar numero magico
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
        if (bluetoothService != null)
        {
            if (bluetoothService.getBluetoothConnection().isConnected(device))
            {
                disconnect();
            }
            else
            {
                bluetoothService.connectToDevice(device.getAddress());
            }
        }
    }
    //endregion Listeners

    //region Broadcast Receivers
    private class BluetoothDeviceFoundReceiver extends BroadcastReceiver
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

    private class BluetoothDeviceConnectedBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            BluetoothDeviceData data = BroadcastUtil.getData(intent, BluetoothDeviceData.class);//TODO: GUARDAR LOS PARAMETROS?
            handleDeviceConnectionStatus(bluetoothService.getConnectedDeviceAddress(), true);
            customProgressDialog.dismiss();

            if (data != null)
            {
                txtMaximumWeight.setText(String.valueOf(data.getMaxAllowedWeight()));
                txtCriticalPercentage.setText(String.valueOf(data.getCriticalPercentage() * 100));
                txtFullPercentage.setText(String.valueOf(data.getFullPercentage() * 100));
            }
            registerSensor();
        }
    }

    private class BluetoothDeviceDisconnectedReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);//TODO: PONER ESTE EXTRA EN ALGUN LADO?
            handleDeviceConnectionStatus(device.getAddress(), false);
            customProgressDialog.dismiss();
            unregisterSensor();
        }
    }

    private void unregisterSensor()
    {
        if (isSensorRegistered)
        {
            sensor.unregisterListener(this);
            isSensorRegistered = false;
        }
    }

    private class DeviceUnsupportedBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            customProgressDialog.dismiss();
            Toast.makeText(getApplicationContext(), "Dispositivo no soportado", Toast.LENGTH_SHORT).show();
        }
    }

    private class BluetoothDeviceBondStateChangedReceiver extends BroadcastReceiver
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

        Button btnStartCalibration = findViewById(R.id.button_start_calibration);
        btnStartCalibration.setOnClickListener(btnStartCalibrationOnClickListener);

        //region Spinner
        Spinner spinnerSensors = findViewById(R.id.spinner_sensors);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        adapter.addAll(CalibrationHelpers.sensorsDictionary.keySet());
        spinnerSensors.setAdapter(adapter);
        //endregion Spinner
        //endregion Admin Settings
    }

    private void onCreateMainSettings()
    {
        //region Main Settings
        mainSettingsLayout = findViewById(R.id.layoutMainSettings);

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

        //TODO: VALIDAR PERMISOS.
        Set<BluetoothDevice> bondedDevices = bluetoothService.getBondedDevices();
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
        if (index > 0)
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
    //endregion Other Methods

}


class MinMaxFilter implements InputFilter
{
    private int intMin = 0;
    private int intMax = 0;

    // Initialized
    MinMaxFilter(int minValue, int maxValue)
    {
        this.intMin = minValue;
        this.intMax = maxValue;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend)
    {
        try
        {
            int input = Integer.parseInt(dest.toString() + source.toString());
            if (isInRange(intMin, intMax, input))
            {
                return null;
            }
        }
        catch (NumberFormatException e)
        {
            e.printStackTrace();
        }
        return "";
    }

    private boolean isInRange(int a, int b, int c)
    {
        if (b > a)
        {
            return c >= a && c <= b;
        }
        else
        {
            return c >= b && c <= a;
        }
    }
}