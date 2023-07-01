package com.grupom2.wastemate.activity;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.grupom2.wastemate.R;
import com.grupom2.wastemate.adapter.BaseDeviceListAdapter;
import com.grupom2.wastemate.adapter.PairedDeviceListAdapter;
import com.grupom2.wastemate.bluetooth.BluetoothManager;
import com.grupom2.wastemate.bluetooth.BluetoothService;
import com.grupom2.wastemate.constant.Actions;
import com.grupom2.wastemate.model.BluetoothDeviceData;
import com.grupom2.wastemate.model.BluetoothMessage;
import com.grupom2.wastemate.receiver.BluetoothDisabledBroadcastReceiver;
import com.grupom2.wastemate.util.BroadcastUtil;
import com.grupom2.wastemate.util.CustomProgressDialog;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity implements SensorEventListener
{

    private static final float PRECISION_CHANGE = 20;
    private final BaseDeviceListAdapter.OnClickListener unpairedListItemOnClickListener = new BaseDeviceListAdapter.OnClickListener()
    {
        @Override
        public void onClick(int position, BluetoothDevice model)
        {
            if (model.getBondState() == BluetoothDevice.BOND_NONE)
            {
                try
                {
                    Method method = model.getClass().getMethod("createBond", (Class[]) null);
                    method.invoke(model, (Object[]) null);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    };
    private final BaseDeviceListAdapter.OnClickListener pairedListItemOnUnpairListener = new BaseDeviceListAdapter.OnClickListener()
    {
        @Override
        public void onClick(int position, BluetoothDevice model)
        {
            if (model.getBondState() == BluetoothDevice.BOND_BONDED)
            {
                try
                {
                    Method method = model.getClass().getMethod("removeBond", (Class[]) null);
                    method.invoke(model, (Object[]) null);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    };

    private final BroadcastReceiver deviceUnsupportedBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            customProgressDialog.dismiss();
            Toast.makeText(getApplicationContext(), "Dispositivo no soportado", Toast.LENGTH_SHORT).show();
        }
    };

    private final BroadcastReceiver deviceConnectedBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            BluetoothDeviceData data = BroadcastUtil.getData(intent, BluetoothDeviceData.class);
            //TODO: PINTAR DE VIOLETA?? mejorar
            Log.d("deviceConnectedBroadcastReceiver:onReceive", new Gson().toJson(data));

            //TODO: MEJORAR: NO LLAMAR DIRECTO A GETBTCONN
            String deviceAddress = bluetoothService.getBluetoothConnection().getDeviceAddress();
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
            if (index > 0)
            {
                PairedDeviceListAdapter.PairedDeviceViewHolder viewHolder = (PairedDeviceListAdapter.PairedDeviceViewHolder) pairedDevicesRecyclerView.findViewHolderForAdapterPosition(index);
                viewHolder.setConnectedIndicatorColor(getResources().getColor(R.color.purple_500, getTheme()));
            }
            customProgressDialog.dismiss();
        }
    };

    private ArrayList<BluetoothDevice> pairedDevices;
    private ArrayList<BluetoothDevice> availableDevices;
    private BluetoothManager bluetoothService;
    private ConstraintLayout adminSettingsLayout;
    private TextView txtWeightLimit;
    private TextView txtMinimumDistance;
    private TextView txtCriticalDistance;
    private boolean showingAdminSettings;

    private ConstraintLayout mainSettingsLayout;
    private final View.OnClickListener btnSendSettingsOnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            int weightLimit = Integer.parseInt(txtWeightLimit.getText().toString());
            double minimumDistance = Integer.parseInt(txtMinimumDistance.getText().toString()) / 100.0;
            double criticalDistance = Integer.parseInt(txtCriticalDistance.getText().toString()) / 100.0;
            BluetoothMessage message = new BluetoothMessage(3, weightLimit, minimumDistance, criticalDistance);
            BluetoothService.getInstance().write(message);

            adminSettingsLayout.setVisibility(View.GONE);
            mainSettingsLayout.setVisibility(View.VISIBLE);
            showingAdminSettings = false;
        }
    };
    private final View.OnClickListener toolbarButtonBackOnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            onBackPressed();
        }
    };
    private RecyclerView pairedDevicesRecyclerView;
    private PairedDeviceListAdapter pairedDevicesAdapter;
    private BaseDeviceListAdapter<BaseDeviceListAdapter.BaseDeviceViewHolder> availableDevicesAdapter;

    private final BroadcastReceiver bluetoothDeviceBondStateChangedReceiver = new BroadcastReceiver()
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
    };
    private final BroadcastReceiver bluetoothDeviceFoundReceiver = new BroadcastReceiver()
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
    };
    private SensorManager sensor;
    private IntentFilter bluetoothStatusChangedFilter;
    private CustomProgressDialog customProgressDialog;


    private final PairedDeviceListAdapter.OnClickListener pairedDeviceListItemOnClickListener = new BaseDeviceListAdapter.OnClickListener()
    {
        @Override
        public void onClick(int position, BluetoothDevice device)
        {
            try
            {
                customProgressDialog.show();
                if (bluetoothService != null)
                {
                    if (bluetoothService.getBluetoothConnection().isConnected())
                    {
                        bluetoothService.disconnectAndForget();
                    }
                    else
                    {
                        bluetoothService.connectToDevice(device.getAddress());
                    }
                }
            }
            catch (SecurityException e)
            {
                customProgressDialog.dismiss();
                //TODO: SEND TO PERMISSIONS SCREEN
                //android.permission.BLUETOOTH_CONNECT
            }

        }
    };
    private final BroadcastReceiver bluetoothDeviceDisconnectedReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            extracted(intent, device.getAddress());
        }
    };
    private BroadcastReceiver bluetoothDisabledBroadcastReceiver = new BluetoothDisabledBroadcastReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //Se asigna un layout al activity para poder vincular los distintos componentes
        setContentView(R.layout.activity_settings);

        bluetoothService = BluetoothService.getInstance();
        availableDevices = new ArrayList<>();
        pairedDevices = new ArrayList<>();

        sensor = (SensorManager) getSystemService(SENSOR_SERVICE);
        showingAdminSettings = false;

        Toolbar toolbar = findViewById(R.id.toolbar);
        ImageView toolbarButtonBack = findViewById(R.id.toolbar_button_back);

        adminSettingsLayout = findViewById(R.id.layoutAdminSettings);
        txtCriticalDistance = findViewById(R.id.txt_critical_percentage);
        txtMinimumDistance = findViewById(R.id.txt_full_percentage);
        txtWeightLimit = findViewById(R.id.txt_weight_limit);
        Button btnSendSettings = findViewById(R.id.button_send_settings);

        mainSettingsLayout = findViewById(R.id.layoutMainSettings);
        RecyclerView availableDevicesRecyclerView = findViewById(R.id.recycler_view_available);
        pairedDevicesRecyclerView = findViewById(R.id.recycler_view_paired);

        // using toolbar as ActionBar
        setSupportActionBar(toolbar);

        customProgressDialog = new CustomProgressDialog(SettingsActivity.this);

//TODO: ARREGLAR ESTO
        Set<BluetoothDevice> bondedDevices = bluetoothService.getBluetoothConnection().getAdapter().getBondedDevices();
        if (bondedDevices != null && bondedDevices.size() != 0)
        {
            pairedDevices.addAll(bondedDevices);
        }

        pairedDevicesAdapter = new PairedDeviceListAdapter();
        pairedDevicesAdapter.setData(pairedDevices);
        LinearLayoutManager pairedDevicesLayoutManager = new LinearLayoutManager(this);
        pairedDevicesLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        pairedDevicesRecyclerView.setLayoutManager(pairedDevicesLayoutManager);

        availableDevicesAdapter = new BaseDeviceListAdapter<>();
        availableDevicesAdapter.setData(availableDevices);

        LinearLayoutManager availableDevicesLayoutManager = new LinearLayoutManager(this);
        availableDevicesLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        availableDevicesRecyclerView.setLayoutManager(availableDevicesLayoutManager);

        BroadcastUtil.registerReceiver(this, bluetoothDeviceFoundReceiver, BluetoothDevice.ACTION_FOUND);
        BroadcastUtil.registerReceiver(this, bluetoothDeviceBondStateChangedReceiver, BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        BroadcastUtil.registerReceiver(this, bluetoothDeviceDisconnectedReceiver, BluetoothDevice.ACTION_ACL_DISCONNECTED);
        BroadcastUtil.registerReceiver(this, bluetoothDisabledBroadcastReceiver);
        BroadcastUtil.registerLocalReceiver(this, deviceConnectedBroadcastReceiver, Actions.ACTION_ACK);
        BroadcastUtil.registerLocalReceiver(this, deviceUnsupportedBroadcastReceiver, Actions.ACTION_UNSUPPORTED_DEVICE);

        btnSendSettings.setOnClickListener(btnSendSettingsOnClickListener);
        pairedDevicesAdapter.setOnItemClickedListener(pairedDeviceListItemOnClickListener);
        pairedDevicesAdapter.setOnUnpairDeviceClickListener(pairedListItemOnUnpairListener);
        availableDevicesAdapter.setOnItemClickedListener(unpairedListItemOnClickListener);
        toolbarButtonBack.setOnClickListener(toolbarButtonBackOnClickListener);

        pairedDevicesRecyclerView.setAdapter(pairedDevicesAdapter);
        availableDevicesRecyclerView.setAdapter(availableDevicesAdapter);

        //TODO: MODIFICAR, NO LLAMAR A TODO ESTO.
        bluetoothService.getBluetoothConnection().getAdapter().startDiscovery();
    }

    @Override
//Cuando se detruye la Acivity se quita el registro de los brodcast. Apartir de este momento no se
//recibe mas broadcast del SO. del bluethoot
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
//Cada vez que se detecta el evento OnResume se establece la comunicacion con el HC05, creando un
//socketBluethoot
    protected void onResume()
    {
        super.onResume();
        //TODO: REHACER
//        if (bluetoothService != null)
//        {
//            if (!bluetoothService.getAdapter().isEnabled())
//            {
//                Intent btintent = new Intent(SettingsActivity.this, BluetoothDisabledActivity.class);
//                btintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(btintent);
//            }
//        }

        sensor.registerListener(this, sensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
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
        sensor.unregisterListener(this);
    }

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


    private void extracted(Intent intent, String deviceAddress)
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
        if (index > 0)
        {
            PairedDeviceListAdapter.PairedDeviceViewHolder viewHolder = (PairedDeviceListAdapter.PairedDeviceViewHolder) pairedDevicesRecyclerView.findViewHolderForAdapterPosition(index);
            if (Objects.equals(intent.getAction(), BluetoothDevice.ACTION_ACL_DISCONNECTED))
            {
                viewHolder.setConnectedIndicatorColor(getResources().getColor(R.color.grey, getTheme()));
            }
        }
        customProgressDialog.dismiss();
    }
}