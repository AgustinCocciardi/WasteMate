package soadv.grupom2.wastemate;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity implements SensorEventListener {

    private static final float PRECISION_CHANGE = 30;

    //region ViewLinks
    private ArrayList<BluetoothDevice> pairedDevices;
    private ArrayList<BluetoothDevice> availableDevices;
    private RecyclerView availableDevicesListView;
    private RecyclerView pairedDevicesListView;
    private DeviceListAdapter mPairedDevicesAdapter;
    private DeviceListAdapter mAvailableDevicesAdapter;
    private BluetoothSwitchCompat bluetoothSwitch;
    //region Admin Settings
    private ConstraintLayout adminSettingsLayout;
    private TextView txtWeightLimit;
    private  TextView txtMinimumDistance;
    private TextView txtCriticalDistance;

    //endregion
    private Button btnSendSettings;
    private ConstraintLayout mainSettingsLayout;

    //endregion

    private SensorManager sensor;
    private boolean showingAdminSettings;

    ActivityResultLauncher<Intent> enableBluetoothActivityLauncher;
    ActivityResultLauncher<Intent> disableBluetoothActivityLauncher;

    private CompoundButton.OnCheckedChangeListener bluetoothSwitchCheckedChangeListener=new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked)
            {
                enableBluetooth();
            }
            else
            {
                disableBluetooth();
            }
        }
    };
    private DeviceListAdapter.OnClickListener connectListener = new DeviceListAdapter.OnClickListener() {
        @Override
        public void onClick(int position, BluetoothDevice model) {
            if(model.getBondState() == BluetoothDevice.BOND_BONDED){
                BluetoothService bluetoothService = BluetoothService.getInstance();
                if (bluetoothService != null) {
                    // Use the Bluetooth service methods
                    bluetoothService.connectToDevice(model);
                }
                Toast.makeText(getApplicationContext(), "conectar",Toast.LENGTH_SHORT).show();
            } else if (model.getBondState() == BluetoothDevice.BOND_NONE) {
                try {
                    Method method = model.getClass().getMethod("createBond", (Class[]) null);
                    method.invoke(model, (Object[]) null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private DeviceListAdapter.OnClickListener unpairListener = new DeviceListAdapter.OnClickListener() {
        @Override
        public void onClick(int position, BluetoothDevice model) {
            if(model.getBondState() == BluetoothDevice.BOND_BONDED){
                try {
                    Method method = model.getClass().getMethod("removeBond", (Class[]) null);
                    method.invoke(model, (Object[]) null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private BroadcastReceiver bluetoothDeviceBondStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
            if (state == BluetoothDevice.BOND_BONDED) {
                availableDevices.remove(device);
                pairedDevices.add(device);

            }
            else if (state == BluetoothDevice.BOND_NONE) {
                availableDevices.add(device);
                pairedDevices.remove(device);
            }
            mAvailableDevicesAdapter.setData(availableDevices);
            mPairedDevicesAdapter.setData(pairedDevices);
            mAvailableDevicesAdapter.notifyDataSetChanged();
            mPairedDevicesAdapter.notifyDataSetChanged();
        }
    };
    private BroadcastReceiver bluetoothDeviceConnectedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int index = pairedDevices.indexOf(device);
            DeviceListAdapter.ViewHolder vh = (DeviceListAdapter.ViewHolder) pairedDevicesListView.findViewHolderForAdapterPosition(index);
        vh.vw.setBackgroundColor(Color.parseColor("#FF6200EE"));
        }
    };
    private View.OnClickListener btnSendSettingsOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int weightLimit = Integer.parseInt(txtWeightLimit.getText().toString());
            int minimumDistance = Integer.parseInt(txtMinimumDistance.getText().toString());
            int criticalDistance = Integer.parseInt(txtCriticalDistance.getText().toString());
            BluetoothMessage message = new BluetoothMessage(3,weightLimit, minimumDistance, criticalDistance);
            BluetoothService.getInstance().sendData(message.Serialize());

            adminSettingsLayout.setVisibility(View.GONE);
            mainSettingsLayout.setVisibility(View.VISIBLE);
            showingAdminSettings = false;
        }
    };

    @Override
    public void onBackPressed() {
        if(showingAdminSettings){
            adminSettingsLayout.setVisibility(View.GONE);
            mainSettingsLayout.setVisibility(View.VISIBLE);
            showingAdminSettings = false;
        }
        else {
            super.onBackPressed();
        }
    }


    private void disableBluetooth() {
        Intent intent = new Intent("android.bluetooth.adapter.action.REQUEST_DISABLE");
        disableBluetoothActivityLauncher.launch(intent);
    }

    private void enableBluetooth(){
        IntentFilter discoveryFinished = new IntentFilter();
        discoveryFinished.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        Intent intent = new Intent("android.bluetooth.adapter.action.REQUEST_ENABLE");
        enableBluetoothActivityLauncher.launch(intent);
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showingAdminSettings = false;
        availableDevices = new ArrayList<>();
        setContentView(R.layout.activity_settings);

        adminSettingsLayout = findViewById(R.id.layoutAdminSettings);
        btnSendSettings = findViewById(R.id.btnSendSettings);
        btnSendSettings.setOnClickListener(btnSendSettingsOnClickListener);

        mainSettingsLayout = findViewById(R.id.layoutMainSettings);

        sensor = (SensorManager) getSystemService(SENSOR_SERVICE);

        //defino los componentes de layout
        availableDevicesListView = (RecyclerView) findViewById(R.id.table_available);
        pairedDevicesListView = (RecyclerView) findViewById(R.id.table_paired);


        bluetoothSwitch = (findViewById(R.id.switch1));
        pairedDevices = new ArrayList<>();
        //Se crea un adaptador para podermanejar el bluethoot del celular
        //mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothService bluetoothService = BluetoothService.getInstance();
        if (bluetoothService != null) {
            Set<BluetoothDevice> bondedDevices = bluetoothService.getAdapter().getBondedDevices();
            if (bondedDevices != null && bondedDevices.size() != 0) {
                pairedDevices.addAll(bondedDevices);
                pairedDevices.addAll(bondedDevices);
                pairedDevices.addAll(bondedDevices);
                pairedDevices.addAll(bondedDevices);

            }
        }

        enableBluetoothActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                (result) -> enableBluetoothCallback(result));

        disableBluetoothActivityLauncher= registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                (result) -> disableBluetoothCallback(result));
        bluetoothSwitch.setChecked(bluetoothService.getAdapter().isEnabled());
        bluetoothSwitch.setOnCheckedChangeListener(bluetoothSwitchCheckedChangeListener);

        //defino un adaptador para el ListView donde se van mostrar en la activity los dispositovs encontrados
        mPairedDevicesAdapter = new DeviceListAdapter(true);
        //asocio el listado de los dispositovos pasado en el bundle al adaptador del Listview
        mPairedDevicesAdapter.setData(pairedDevices);
        pairedDevicesListView.setAdapter(mPairedDevicesAdapter);
        mPairedDevicesAdapter.setOnClickListener(connectListener);
        mPairedDevicesAdapter.setOnClickListener2(unpairListener);

        LinearLayoutManager pairedDevicesLayoutManager = new LinearLayoutManager(this);
        pairedDevicesLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        pairedDevicesListView.setLayoutManager(pairedDevicesLayoutManager);

        //defino un adaptador para el ListView donde se van mostrar en la activity los dispositovs encontrados
        mAvailableDevicesAdapter = new DeviceListAdapter(false);
        //asocio el listado de los dispositovos pasado en el bundle al adaptador del Listview
        mAvailableDevicesAdapter.setData(availableDevices);
        availableDevicesListView.setAdapter(mAvailableDevicesAdapter);
        mAvailableDevicesAdapter.setOnClickListener(connectListener);

        LinearLayoutManager availableDeviceslayoutManager = new LinearLayoutManager(this);
        availableDeviceslayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        availableDevicesListView.setLayoutManager(availableDeviceslayoutManager);

        //se definen un broadcastReceiver que captura el broadcast del SO cuando captura los siguientes eventos:
        IntentFilter deviceFoundFilter = new IntentFilter();
        deviceFoundFilter.addAction(BluetoothDevice.ACTION_FOUND); //Se encuentra un dispositivo bluethoot al realizar una busqueda
        //se define (registra) el handler que captura los broadcast anterirmente mencionados.
        registerReceiver(bluetoothDeviceFoundReceiver, deviceFoundFilter);

        IntentFilter discoveryStartedFilter = new IntentFilter();
        discoveryStartedFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED); //Cuando se comienza una busqueda de bluethoot
        //se define (registra) el handler que captura los broadcast anterirmente mencionados.
        registerReceiver(bluetoothDiscoveryStartedReceiver, discoveryStartedFilter);

        IntentFilter discoveryFinished = new IntentFilter();
        discoveryFinished.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); //cuando la busqueda de bluethoot finaliza
        //se define (registra) el handler que captura los broadcast anterirmente mencionados.
        registerReceiver(bluetoothDiscoveryFinishedReceiver, discoveryFinished);

        IntentFilter deviceDisconnected = new IntentFilter();
        deviceDisconnected.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED); //cuando la busqueda de bluethoot finaliza
        //se define (registra) el handler que captura los broadcast anterirmente mencionados.
        registerReceiver(bluetoothDeviceBondStateChangedReceiver, deviceDisconnected);

        IntentFilter deviceConnected = new IntentFilter();
        deviceConnected.addAction(BluetoothDevice.ACTION_ACL_CONNECTED); //cuando la busqueda de bluethoot finaliza
        //se define (registra) el handler que captura los broadcast anterirmente mencionados.
        registerReceiver(bluetoothDeviceConnectedReceiver, deviceConnected);

        bluetoothService.getAdapter().startDiscovery();
    }

    private void enableBluetoothCallback(ActivityResult result) {
        if(result.getResultCode() != Activity.RESULT_OK)
        {
            bluetoothSwitch.setOnCheckedChangeListener(null);
            bluetoothSwitch.setChecked(false);
            bluetoothSwitch.setOnCheckedChangeListener(bluetoothSwitchCheckedChangeListener);
        }
    }

    private void disableBluetoothCallback(ActivityResult result) {
        if(result.getResultCode() != Activity.RESULT_OK)
        {
            bluetoothSwitch.setOnCheckedChangeListener(null);
            bluetoothSwitch.setChecked(true);
            bluetoothSwitch.setOnCheckedChangeListener(bluetoothSwitchCheckedChangeListener);
        }
    }

    @Override
    //Cada vez que se detecta el evento OnResume se establece la comunicacion con el HC05, creando un
    //socketBluethoot
    protected void onResume() {
        super.onResume();
registerSenser();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterSenser();
    }

    @Override
    //Cuando se detruye la Acivity se quita el registro de los brodcast. Apartir de este momento no se
    //recibe mas broadcast del SO. del bluethoot
    public void onDestroy() {
        unregisterReceiver(bluetoothDiscoveryFinishedReceiver);
        unregisterReceiver(bluetoothDiscoveryStartedReceiver);
        unregisterReceiver(bluetoothDeviceFoundReceiver);
        unregisterReceiver(bluetoothDeviceBondStateChangedReceiver);
        super.onDestroy();
    }

    private  final BroadcastReceiver bluetoothDiscoveryStartedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Creo la lista donde voy a mostrar los dispositivos encontrados
            //mDeviceList = new ArrayList<BluetoothDevice>();

            //muestro el cuadro de dialogo de busqueda
            //mProgressDlg.show();
        }
    };
    private  final BroadcastReceiver bluetoothDiscoveryFinishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //se cierra el cuadro de dialogo de busqueda
            //mProgressDlg.dismiss();

            //se inicia el activity DeviceListActivity pasandole como parametros, por intent,
            //el listado de dispositovos encontrados
            //Intent newIntent = new Intent(Sensores.this, DeviceListActivity.class);

            //newIntent.putParcelableArrayListExtra("device.list", mDeviceList);

            //startActivity(newIntent);
        }
    };

    private  final BroadcastReceiver bluetoothDeviceFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Se lo agregan sus datos a una lista de dispositivos encontrados
            BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
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

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int tipoSensor = sensorEvent.sensor.getType();
        float valoresSensor[] = sensorEvent.values;
        if (tipoSensor == Sensor.TYPE_ACCELEROMETER)
        {
            if ((Math.abs(valoresSensor[0]) > PRECISION_CHANGE || Math.abs(valoresSensor[1]) > PRECISION_CHANGE || Math.abs(valoresSensor[2]) > PRECISION_CHANGE))
            {
                adminSettingsLayout.setVisibility(View.VISIBLE);
                mainSettingsLayout.setVisibility(View.GONE);
                showingAdminSettings = true;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void registerSenser()
    {
        sensor.registerListener(this, sensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void unregisterSenser()
    {
        sensor.unregisterListener(this);
    }
}