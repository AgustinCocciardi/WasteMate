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
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private ArrayList<BluetoothDevice> pairedDevices;
    private ArrayList<BluetoothDevice> availableDevices;
    private RecyclerView availableDevicesListView;
    private RecyclerView pairedDevicesListView;
    private DeviceListAdapter mPairedDevicesAdapter;
    private DeviceListAdapter mAvailableDevicesAdapter;
    private BluetoothSwitchCompat bluetoothSwitch;
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
                BluetoothService bluetoothService = BluetoothManager.getService();
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

        availableDevices = new ArrayList<>();
        setContentView(R.layout.activity_settings);

        //defino los componentes de layout
        availableDevicesListView = (RecyclerView) findViewById(R.id.table_available);
        pairedDevicesListView = (RecyclerView) findViewById(R.id.table_paired);


        bluetoothSwitch = (findViewById(R.id.switch1));
        pairedDevices = new ArrayList<>();
        //Se crea un adaptador para podermanejar el bluethoot del celular
        //mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothService bluetoothService = BluetoothManager.getService();
        if (bluetoothService != null) {
            Set<BluetoothDevice> bondedDevices = bluetoothService.getAdapter().getBondedDevices();
            if (bondedDevices != null && bondedDevices.size() != 0) {
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
        BluetoothManager.bindService(this);

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        BluetoothManager.unbindService(this);
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

    //Metodo que actua como Listener de los eventos que ocurren en los componentes graficos de la activty
//    private DeviceListAdapter.OnPairButtonClickListener listenerBotonEmparejar = new DeviceListAdapter.OnPairButtonClickListener() {
//        @Override
//        public void onPairButtonClick(int position) {
//            //Obtengo los datos del dispostivo seleccionado del listview por el usuario
//            BluetoothDevice device = availableDevices.get(position);
//
//            //Se checkea si el sipositivo ya esta emparejado
//            if (device.getBondState() == BluetoothDevice.BOND_BONDED)
//            {
//                //Si esta emparejado,quiere decir que se selecciono desemparjar y entonces se le desempareja
//                unpairDevice(device);
//            }
//            else
//            {
//
//                //Si no esta emparejado,quiere decir que se selecciono emparjar y entonces se le empareja
//                pairDevice(device);
//
//            }
//        }
//    };

}