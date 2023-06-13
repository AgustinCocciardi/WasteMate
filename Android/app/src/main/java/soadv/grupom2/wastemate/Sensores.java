package soadv.grupom2.wastemate;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Sensores extends AppCompatActivity implements SensorEventListener, CompoundButton.OnCheckedChangeListener {

    private boolean botonAtrasPresionado;
    private TextView txtEstadoBluetooth;
    private final static float cambioPrecision = 30;
    private final static double pesoMaximo = 150;
    private final static double capacidadMaxima = 50;
    private TextView limitePeso;
    private TextView limiteCapacidad;
    private TextView etiquetaPeso;
    private TextView etiquetaCapacidad;
    private TextView etiquetaShake;
    private Button grabarBoton;
    private SensorManager sensor;

    private Button btnEmparejarDispositivos;
    private Button btnBuscarDispositivos;
    private ProgressDialog mProgressDlg;

    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();

    private BluetoothAdapter mBluetoothAdapter;

    public static final int MULTIPLE_PERMISSIONS = 10;

    ArrayList<String> permissions;
    ActivityResultLauncher<Intent> enableBluetoothActivityLauncher;
    ActivityResultLauncher<Intent> disableBluetoothActivityLauncher;
    public  Sensores(){
        permissions = new ArrayList<String>(){{
            add(Manifest.permission.ACCESS_COARSE_LOCATION);
            add(Manifest.permission.ACCESS_FINE_LOCATION);
            add(Manifest.permission.READ_PHONE_STATE);
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU){
                add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }else{
                add(Manifest.permission.READ_MEDIA_AUDIO);
                add(Manifest.permission.READ_MEDIA_VIDEO);
                add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
                add(Manifest.permission.BLUETOOTH_SCAN);
                add(Manifest.permission.BLUETOOTH_CONNECT);
            }else{
                add(Manifest.permission.BLUETOOTH);
                add(Manifest.permission.BLUETOOTH_ADMIN);
            }
        }};
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sensores);
        txtEstadoBluetooth = findViewById(R.id.txtEstadoBluetooth);
        btnBuscarDispositivos = (Button) findViewById(R.id.btnBuscarDispositivos);
        btnEmparejarDispositivos = (Button) findViewById(R.id.btnEmparejarDispositivos);
        sensor = (SensorManager) getSystemService(SENSOR_SERVICE);
        etiquetaPeso = findViewById(R.id.etiquetaPeso);
        etiquetaCapacidad = findViewById(R.id.etiquetaCapacidad);
        etiquetaShake = findViewById(R.id.etiquetaShake);
        limitePeso = findViewById(R.id.limitePeso);
        limitePeso.setText(Double.toString(pesoMaximo));
        limiteCapacidad = findViewById(R.id.limiteCapacidad);
        limiteCapacidad.setText(Double.toString(capacidadMaxima));
        grabarBoton = (Button) findViewById(R.id.botonGrabar);
        btnEmparejarDispositivos.setVisibility(View.VISIBLE);
        btnBuscarDispositivos.setVisibility(View.VISIBLE);
        etiquetaShake.setVisibility(View.VISIBLE);
        etiquetaCapacidad.setVisibility(View.GONE);
        etiquetaPeso.setVisibility(View.GONE);
        limiteCapacidad.setVisibility(View.GONE);
        limitePeso.setVisibility(View.GONE);
        grabarBoton.setVisibility(View.GONE);

        //Se crea un adaptador para podermanejar el bluethoot del celular
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //Se Crea la ventana de dialogo que indica que se esta buscando dispositivos bluethoot
        mProgressDlg = new ProgressDialog(this);

        mProgressDlg.setMessage("Buscando dispositivos...");
        mProgressDlg.setCancelable(false);

        //se asocia un listener al boton cancelar para la ventana de dialogo ue busca los dispositivos bluethoot
        mProgressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancelar", btnCancelarDialogListener);

        if (checkPermissions())
        {
            enableComponent();
        }
        grabarBoton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Double.parseDouble(String.valueOf(limitePeso.getText())) > pesoMaximo || Double.parseDouble(String.valueOf(limiteCapacidad.getText())) > capacidadMaxima)
                {
                    Toast.makeText(getApplicationContext(),"Excedio los valores de capacidad o peso. \nPeso maximo: 150. Capacidad maxima: 50", Toast.LENGTH_LONG).show();
                }
                else
                {
                    //Pasar con Bluetooth
                    Log.i("Ejecutando", String.valueOf(limitePeso.getText()));
                    Log.i("Ejecutando", String.valueOf(limiteCapacidad.getText()));
                }
            }
        });
        enableBluetoothActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                (result) -> enableBluetoothCallback());

        disableBluetoothActivityLauncher= registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                (result) -> disableBluetoothCallback());
    }
    private void enableBluetoothCallback(){
        showEnabled();
    }

    private void disableBluetoothCallback(){
        showDisabled();
    }
    @Override
    protected void onResume()
    {
        super.onResume();
        registerSenser();
    }

    @Override
    protected void onStop()
    {
        unregisterSenser();
        super.onStop();
    }

    @Override
    protected void onPause() {
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
        }
        super.onPause();
    }

    @Override
    //Cuando se detruye la Acivity se quita el registro de los brodcast. Apartir de este momento no se
    //recibe mas broadcast del SO. del bluethoot
    public void onDestroy() {
        unregisterReceiver(bluetoothDiscoveryFinishedReceiver);
        unregisterReceiver(bluetoothDiscoveryStartedReceiver);
        unregisterReceiver(bluetoothDeviceFoundReceiver);
        super.onDestroy();
    }

    protected  void enableComponent()
    {
        //se determina si existe bluethoot en el celular
        if (mBluetoothAdapter == null)
        {
            //si el celular no soporta bluethoot
            showUnsupported();
        }
        else
        {
            //si el celular soporta bluethoot, se definen los listener para los botones de la activity
            btnEmparejarDispositivos.setOnClickListener(btnEmparejarListener);

            btnBuscarDispositivos.setOnClickListener(btnBuscarListener);

            //se determina si esta activado el bluethoot
            if (mBluetoothAdapter.isEnabled())
            {
                //se informa si esta habilitado
                showEnabled();
            }
            else
            {
                //se informa si esta deshabilitado
                showDisabled();
            }
        }

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
    }

    private void showEnabled() {
        txtEstadoBluetooth.setText("Bluetooth Activado");
        txtEstadoBluetooth.setTextColor(Color.BLUE);
        btnEmparejarDispositivos.setEnabled(true);
        btnBuscarDispositivos.setEnabled(true);
        btnBuscarDispositivos.setBackgroundColor(Color.BLUE);
        btnEmparejarDispositivos.setBackgroundColor(Color.BLUE);
        btnBuscarDispositivos.setTextColor(Color.WHITE);
        btnEmparejarDispositivos.setTextColor(Color.WHITE);
        grabarBoton.setEnabled(true);
        grabarBoton.setBackgroundColor(Color.BLUE);
        grabarBoton.setTextColor(Color.WHITE);
    }

    private void showDisabled() {
        txtEstadoBluetooth.setText("Bluetooth Desactivado");
        txtEstadoBluetooth.setTextColor(Color.RED);
        btnEmparejarDispositivos.setEnabled(false);
        btnBuscarDispositivos.setEnabled(false);
        btnBuscarDispositivos.setBackgroundColor(Color.GRAY);
        btnEmparejarDispositivos.setBackgroundColor(Color.GRAY);
        btnBuscarDispositivos.setTextColor(Color.WHITE);
        btnEmparejarDispositivos.setTextColor(Color.WHITE);
        grabarBoton.setEnabled(false);
        grabarBoton.setBackgroundColor(Color.GRAY);
        grabarBoton.setTextColor(Color.WHITE);
    }

    private void showUnsupported() {
        txtEstadoBluetooth.setText("Bluetooth no es soportado por el dispositivo movil");

        btnEmparejarDispositivos.setEnabled(false);
        btnBuscarDispositivos.setEnabled(false);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private  final BroadcastReceiver bluetoothDiscoveryStartedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Creo la lista donde voy a mostrar los dispositivos encontrados
            mDeviceList = new ArrayList<BluetoothDevice>();

            //muestro el cuadro de dialogo de busqueda
            mProgressDlg.show();
        }
    };
    private  final BroadcastReceiver bluetoothDiscoveryFinishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //se cierra el cuadro de dialogo de busqueda
            mProgressDlg.dismiss();

            //se inicia el activity DeviceListActivity pasandole como parametros, por intent,
            //el listado de dispositovos encontrados
            Intent newIntent = new Intent(Sensores.this, DeviceListActivity.class);

            newIntent.putParcelableArrayListExtra("device.list", mDeviceList);

            startActivity(newIntent);
        }
    };

    private  final BroadcastReceiver bluetoothDeviceFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Se lo agregan sus datos a una lista de dispositivos encontrados
            BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            mDeviceList.add(device);
            showToast("Dispositivo Encontrado:" + device.getName());
        }
    };

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int tipoSensor = sensorEvent.sensor.getType();
        float valoresSensor[] = sensorEvent.values;
        if (tipoSensor == Sensor.TYPE_ACCELEROMETER)
        {
            if ((Math.abs(valoresSensor[0]) > cambioPrecision || Math.abs(valoresSensor[1]) > cambioPrecision || Math.abs(valoresSensor[2]) > cambioPrecision))
            {
                botonAtrasPresionado = true;
                etiquetaShake.setVisibility(View.GONE);
                etiquetaCapacidad.setVisibility(View.VISIBLE);
                etiquetaPeso.setVisibility(View.VISIBLE);
                limiteCapacidad.setVisibility(View.VISIBLE);
                limitePeso.setVisibility(View.VISIBLE);
                grabarBoton.setVisibility(View.VISIBLE);
                btnEmparejarDispositivos.setVisibility(View.GONE);
                btnBuscarDispositivos.setVisibility(View.GONE);
            }
        }
    }

    //Metodo que actua como Listener de los eventos que ocurren en los componentes graficos de la activty
    private View.OnClickListener btnEmparejarListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

            if (pairedDevices == null || pairedDevices.size() == 0)
            {
                showToast("No se encontraron dispositivos emparejados");
            }
            else
            {
                ArrayList<BluetoothDevice> list = new ArrayList<BluetoothDevice>();

                list.addAll(pairedDevices);

                Intent intent = new Intent(Sensores.this, DeviceListActivity.class);

                intent.putParcelableArrayListExtra("device.list", list);

                startActivity(intent);
            }
        }
    };

    private View.OnClickListener btnBuscarListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mBluetoothAdapter.startDiscovery();
        }
    };

    private View.OnClickListener btnActivarListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mBluetoothAdapter.isEnabled()) {
                disableBluetooth();
            } else {
                enableBluetooth();
            }
        }
    };
    private void disableBluetooth() {
        unregisterReceiver(bluetoothDiscoveryFinishedReceiver);
        Intent intent = new Intent("android.bluetooth.adapter.action.REQUEST_DISABLE");
        disableBluetoothActivityLauncher.launch(intent);
    }

    private void enableBluetooth(){
        IntentFilter discoveryFinished = new IntentFilter();
        discoveryFinished.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothDiscoveryFinishedReceiver, discoveryFinished);
        Intent intent = new Intent("android.bluetooth.adapter.action.REQUEST_ENABLE");
        enableBluetoothActivityLauncher.launch(intent);
    }

    private DialogInterface.OnClickListener btnCancelarDialogListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();

            mBluetoothAdapter.cancelDiscovery();
        }
    };


    //Metodo que chequea si estan habilitados los permisos
    private  boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();

        //Se chequea si la version de Android es menor a la 6
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }


        for (String p:permissions) {
            result = ContextCompat.checkSelfPermission(this,p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),MULTIPLE_PERMISSIONS );
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permissions granted.
                    enableComponent(); // Now you call here what ever you want :)
                } else {
                    String perStr = "";
                    for (String per : permissions) {
                        perStr += "\n" + per;
                    }
                    // permissions list of don't granted permission
                    Toast.makeText(this, "ATENCION: La aplicacion no funcionara " +
                            "correctamente debido a la falta de Permisos", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if (isChecked)
        {
            registerSenser();
        }
        else
        {
            unregisterSenser();
        }
    }

    private void registerSenser()
    {
        boolean done;
        done = sensor.registerListener(this, sensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        Log.i("sensor", "Registrado");
    }

    private void unregisterSenser()
    {
        sensor.unregisterListener(this);
        Log.i("sensor", "No registrado");
    }

    @Override
    public void onBackPressed() {
        if (botonAtrasPresionado)
        {
            botonAtrasPresionado = false;
            etiquetaShake.setVisibility(View.VISIBLE);
            etiquetaCapacidad.setVisibility(View.GONE);
            etiquetaPeso.setVisibility(View.GONE);
            limiteCapacidad.setVisibility(View.GONE);
            limitePeso.setVisibility(View.GONE);
            grabarBoton.setVisibility(View.GONE);
            btnEmparejarDispositivos.setVisibility(View.VISIBLE);
            btnBuscarDispositivos.setVisibility(View.VISIBLE);
        }
        else
        {
            //ejecuta super.onBackPressed() para que finalice el metodo cerrando el activity
            super.onBackPressed();
        }
    }

}