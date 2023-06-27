package soadv.grupom2.wastemate;

import static android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity implements SensorEventListener {

    //region Attributes
    private ArrayList<BluetoothDevice> pairedDevices;
    private ArrayList<BluetoothDevice> availableDevices;
    private BluetoothService bluetoothService;

    //region Admin Settings
    private ConstraintLayout adminSettingsLayout;
    private TextView txtWeightLimit;
    private TextView txtMinimumDistance;
    private TextView txtCriticalDistance;
    private boolean showingAdminSettings;
    //endregion

    //region Main Settings
    private ConstraintLayout mainSettingsLayout;
    private RecyclerView pairedDevicesRecyclerView;
    private PairedDeviceListAdapter mPairedDevicesAdapter;
    private BaseDeviceListAdapter<BaseDeviceListAdapter.BaseDeviceViewHolder> mAvailableDevicesAdapter;
    //endregion
    //endregion

    //region Shake
    private SensorManager sensor;
    private static final float PRECISION_CHANGE = 20;
    private IntentFilter bluetoothStatusChangedFilter;

    private  CustomProgressDialog customProgressDialog;

    private final OnMessageReceivedListener onDeviceUnSupportedListener = new OnMessageReceivedListener() {
        @Override
        public void onMessageReceived(BluetoothDevice model, BluetoothMessageResponse response) {
            customProgressDialog.dismiss();
        }
    };
    private final OnMessageReceivedListener onAckMessageReceivedListener = new OnMessageReceivedListener() {
        @Override
        public void onMessageReceived(BluetoothDevice model, BluetoothMessageResponse response) {

            String deviceAddress = model.getAddress();
            int index = -1;
            for (int i = 0; i < pairedDevices.size(); i++) {
                BluetoothDevice pairedDevice = pairedDevices.get(i);
                if (pairedDevice.getAddress().equals(deviceAddress)) {
                    index = i;
                    break;
                }
            }
            if (index > 0) {
                PairedDeviceListAdapter.PairedDeviceViewHolder viewHolder = (PairedDeviceListAdapter.PairedDeviceViewHolder) pairedDevicesRecyclerView.findViewHolderForAdapterPosition(index);
                SharedPreferences pref = getSharedPreferences("info", MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("connectedDevice", bluetoothService.getDevice().getAddress());
                editor.apply();
                viewHolder.setConnectedIndicatorColor(getResources().getColor(R.color.purple_500, getTheme()));
            }
            customProgressDialog.dismiss();
        }
    };
    //endregion
    //endregion

    //region Overrides
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Se asigna un layout al activity para poder vincular los distintos componentes
        setContentView(R.layout.activity_settings);

        bluetoothService = BluetoothService.getInstance();
        availableDevices = new ArrayList<>();
        pairedDevices = new ArrayList<>();

        sensor = (SensorManager) getSystemService(SENSOR_SERVICE);
        showingAdminSettings = false;

        //region Layout Links
        Toolbar toolbar = findViewById(R.id.toolbar);
        //region ViewLinks
        ImageView toolbarButtonBack = findViewById(R.id.toolbar_button_back);

        adminSettingsLayout = findViewById(R.id.layoutAdminSettings);
        txtCriticalDistance = findViewById(R.id.txt_critical_percentage);
        txtMinimumDistance = findViewById(R.id.txt_full_percentage);
        txtWeightLimit = findViewById(R.id.txt_weight_limit);
        Button btnSendSettings = findViewById(R.id.button_send_settings);

        mainSettingsLayout = findViewById(R.id.layoutMainSettings);
        RecyclerView availableDevicesRecyclerView = findViewById(R.id.recycler_view_available);
        pairedDevicesRecyclerView = findViewById(R.id.recycler_view_paired);
        //endregion

        // using toolbar as ActionBar
        setSupportActionBar(toolbar);

        customProgressDialog = new CustomProgressDialog(SettingsActivity.this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Set<BluetoothDevice> bondedDevices = bluetoothService.getAdapter().getBondedDevices();
        if (bondedDevices != null && bondedDevices.size() != 0) {
            pairedDevices.addAll(bondedDevices);
        }

        mPairedDevicesAdapter = new PairedDeviceListAdapter();
        mPairedDevicesAdapter.setData(pairedDevices);
        LinearLayoutManager pairedDevicesLayoutManager = new LinearLayoutManager(this);
        pairedDevicesLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        pairedDevicesRecyclerView.setLayoutManager(pairedDevicesLayoutManager);

        mAvailableDevicesAdapter = new BaseDeviceListAdapter<>();
        mAvailableDevicesAdapter.setData(availableDevices);

        LinearLayoutManager availableDevicesLayoutManager = new LinearLayoutManager(this);
        availableDevicesLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        availableDevicesRecyclerView.setLayoutManager(availableDevicesLayoutManager);

        IntentFilter deviceFoundFilter = new IntentFilter();
        deviceFoundFilter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothDeviceFoundReceiver, deviceFoundFilter);

        IntentFilter deviceUnpaired = new IntentFilter();
        deviceUnpaired.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(bluetoothDeviceBondStateChangedReceiver, deviceUnpaired);

        IntentFilter deviceConnected = new IntentFilter();
        deviceConnected.addAction(ACTION_ACL_DISCONNECTED);
        registerReceiver(bluetoothDeviceConnectedReceiver, deviceConnected);

        bluetoothStatusChangedFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStatusChangedBroadcastReceiver, bluetoothStatusChangedFilter);

        //region Set Listeners
        btnSendSettings.setOnClickListener(btnSendSettingsOnClickListener);
        mPairedDevicesAdapter.setOnItemClickedListener(on);
        mPairedDevicesAdapter.setOnUnpairDeviceClickListener(unpairListener);
        mAvailableDevicesAdapter.setOnItemClickedListener(connectListener);
        toolbarButtonBack.setOnClickListener(toolbarButtonBackOnClickListener);
        //endregion

        pairedDevicesRecyclerView.setAdapter(mPairedDevicesAdapter);
        availableDevicesRecyclerView.setAdapter(mAvailableDevicesAdapter);

        bluetoothService.getAdapter().startDiscovery();
    }

    @Override
    //Cuando se detruye la Acivity se quita el registro de los brodcast. Apartir de este momento no se
    //recibe mas broadcast del SO. del bluethoot
    public void onDestroy() {
        unregisterReceiver(bluetoothDeviceFoundReceiver);
        unregisterReceiver(bluetoothDeviceBondStateChangedReceiver);
        super.onDestroy();
    }

    @Override
    //Cada vez que se detecta el evento OnResume se establece la comunicacion con el HC05, creando un
    //socketBluethoot
    protected void onResume() {
        super.onResume();
        if (bluetoothService != null) {
            if (!bluetoothService.getAdapter().isEnabled()) {
                Intent btintent = new Intent(SettingsActivity.this, BluetoothDisabledActivity.class);
                btintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(btintent);
            }
        }

        registerReceiver(bluetoothStatusChangedBroadcastReceiver, bluetoothStatusChangedFilter);

        bluetoothService.setOnDeviceUnsupportedListener(onDeviceUnSupportedListener);
        bluetoothService.setOnAckMessageReceivedListener(onAckMessageReceivedListener);

        sensor.registerListener(this, sensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensor.unregisterListener(this);
        unregisterReceiver(bluetoothStatusChangedBroadcastReceiver);
    }

    @Override
    public void onBackPressed() {
        if (showingAdminSettings) {
            adminSettingsLayout.setVisibility(View.GONE);
            mainSettingsLayout.setVisibility(View.VISIBLE);
            showingAdminSettings = false;
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();
        float sensorValues[] = sensorEvent.values;
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            if ((Math.abs(sensorValues[0]) > PRECISION_CHANGE || Math.abs(sensorValues[1]) > PRECISION_CHANGE || Math.abs(sensorValues[2]) > PRECISION_CHANGE)) {
                adminSettingsLayout.setVisibility(View.VISIBLE);
                mainSettingsLayout.setVisibility(View.GONE);
                showingAdminSettings = true;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    //endregion

    //region Listeners
    private final View.OnClickListener btnSendSettingsOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int weightLimit = Integer.parseInt(txtWeightLimit.getText().toString());
            double minimumDistance = Integer.parseInt(txtMinimumDistance.getText().toString()) / 100.0;
            double criticalDistance = Integer.parseInt(txtCriticalDistance.getText().toString()) / 100.0;
            BluetoothMessage message = new BluetoothMessage(3, weightLimit, minimumDistance, criticalDistance);
            BluetoothService.getInstance().sendData(message.Serialize());

            adminSettingsLayout.setVisibility(View.GONE);
            mainSettingsLayout.setVisibility(View.VISIBLE);
            showingAdminSettings = false;
        }
    };

    private final BaseDeviceListAdapter.OnClickListener connectListener = new BaseDeviceListAdapter.OnClickListener() {
        @Override
        public void onClick(int position, BluetoothDevice model) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            if (model.getBondState() == BluetoothDevice.BOND_NONE) {
                try {
                    Method method = model.getClass().getMethod("createBond", (Class[]) null);
                    method.invoke(model, (Object[]) null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private final PairedDeviceListAdapter.OnClickListener on = new BaseDeviceListAdapter.OnClickListener() {
        @Override
        public void onClick(int position, BluetoothDevice device) {
            try{
                customProgressDialog.show();
                if (bluetoothService != null)
                {
                    if(bluetoothService.isDeviceConnected(device))
                    {
                        bluetoothService.disconnect();
                    }
                    else if(device.getBondState() == BluetoothDevice.BOND_BONDED)
                    {
                        bluetoothService.connectToDevice(device);
                    }
                }
            }
            catch (SecurityException e){
                customProgressDialog.dismiss();
                //TODO: SEND TO PERMISSIONS SCREEN
                //android.permission.BLUETOOTH_CONNECT
            }

        }
    };

    private final BaseDeviceListAdapter.OnClickListener unpairListener = new BaseDeviceListAdapter.OnClickListener() {
        @Override
        public void onClick(int position, BluetoothDevice model) {
            try {
                if (model.getBondState() == BluetoothDevice.BOND_BONDED) {
                    try {
                        Method method = model.getClass().getMethod("removeBond", (Class[]) null);
                        method.invoke(model, (Object[]) null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }catch (SecurityException e){
                //TODO: SEND TO PERMISSIONS SCREEN
                //android.permission.BLUETOOTH_CONNECT
            }
        }
    };

    private final View.OnClickListener toolbarButtonBackOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onBackPressed();
        }
    };
    //endregion

    //region Broadcast Receivers
    private final BroadcastReceiver bluetoothDeviceBondStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
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
            mAvailableDevicesAdapter.setData(availableDevices);
            mPairedDevicesAdapter.setData(pairedDevices);
            mAvailableDevicesAdapter.notifyDataSetChanged();
            mPairedDevicesAdapter.notifyDataSetChanged();
        }
    };

    private final BroadcastReceiver bluetoothDeviceConnectedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            extracted(intent, device.getAddress());
        }
    };

    private final BroadcastReceiver bluetoothStatusChangedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_OFF) {
                    Intent btintent = new Intent(SettingsActivity.this, BluetoothDisabledActivity.class);
                    startActivity(btintent);
                }
            }
        }
    };

    private void extracted(Intent intent, String deviceAddress) {
        int index = -1;
        for (int i = 0; i < pairedDevices.size(); i++) {
            BluetoothDevice pairedDevice = pairedDevices.get(i);
            if (pairedDevice.getAddress().equals(deviceAddress)) {
                index = i;
                break;
            }
        }
        if(index > 0)
        {
            PairedDeviceListAdapter.PairedDeviceViewHolder viewHolder = (PairedDeviceListAdapter.PairedDeviceViewHolder) pairedDevicesRecyclerView.findViewHolderForAdapterPosition(index);
            if(Objects.equals(intent.getAction(), ACTION_ACL_DISCONNECTED))
            {
                SharedPreferences pref = getSharedPreferences("info", MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.remove("connectedDevice");
                editor.apply();
                viewHolder.setConnectedIndicatorColor(getResources().getColor(R.color.grey, getTheme()));
            }
        }
        customProgressDialog.dismiss();
    }
    //endregion

    private  final BroadcastReceiver bluetoothDeviceFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Se lo agregan sus datos a una lista de dispositivos encontrados
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if(!pairedDevices.contains(device)){
                int i = availableDevices.indexOf(device);
                if(i>=0){
                    availableDevices.set(i,device);
                }
            else {
                    availableDevices.add(device);
                }
                mAvailableDevicesAdapter.setData(availableDevices);
                mAvailableDevicesAdapter.notifyDataSetChanged();
            }
        }
    };
}