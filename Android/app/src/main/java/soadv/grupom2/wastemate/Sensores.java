package soadv.grupom2.wastemate;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
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
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Sensores extends AppCompatActivity implements SensorEventListener, CompoundButton.OnCheckedChangeListener {

    private boolean botonAtrasPresionado = false;

    private boolean puedoMandarValoresLimite;
    private TextView txtEstadoBluetooth;
    private static final float ZERO = 0;
    private static final float CAMBIO_PRECISION = 30;
    private static final double PESO_MAXIMO = 500;
    private static final double CAPACIDAD = 50;

    private static final double CAPACIDAD_MINIMA = 3;
    private TextView limitePeso;
    private TextView limiteCapacidad;

    private TextView limiteCapacidadMinima;

    private TextView etiquetaCapacidadMinima;
    private TextView etiquetaPeso;
    private TextView etiquetaCapacidad;
    private TextView etiquetaShake;
    private Button grabarBoton;
    private SensorManager sensor;

    private Button btnEmparejarDispositivos;
    private Button btnBuscarDispositivos;
    private ProgressDialog mProgressDlg;

    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();

    Handler bluetoothIn;
    final int handlerState = 0; //used to identify handler message
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothAdapter btAdapter = null;

    private BluetoothSocket btSocket = null;
    private ConnectedThread mConnectedThread;

    // SPP UUID service  - Funciona en la mayoria de los dispositivos
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address del Hc05
    private static String address = null;

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
        etiquetaPeso = findViewById(R.id.tvWeightLimit);
        etiquetaCapacidad = findViewById(R.id.tvCriticalCapacity);
        etiquetaCapacidadMinima = findViewById(R.id.tvMinimumDistance);
        etiquetaShake = findViewById(R.id.etiquetaShake);
        limitePeso = findViewById(R.id.txtWeightLimit);
        limitePeso.setText(Double.toString(PESO_MAXIMO));
        limiteCapacidad = findViewById(R.id.txtCriticalCapacity);
        limiteCapacidad.setText(Double.toString(CAPACIDAD));
        limiteCapacidadMinima = findViewById(R.id.limiteCapacidadMinima);
        limiteCapacidadMinima.setText(Double.toString(CAPACIDAD_MINIMA));
        grabarBoton = (Button) findViewById(R.id.btnSendSettings);
        btnEmparejarDispositivos.setVisibility(View.VISIBLE);
        btnBuscarDispositivos.setVisibility(View.VISIBLE);
        etiquetaShake.setVisibility(View.VISIBLE);
        etiquetaCapacidad.setVisibility(View.GONE);
        etiquetaCapacidadMinima.setVisibility(View.GONE);
        etiquetaPeso.setVisibility(View.GONE);
        limiteCapacidad.setVisibility(View.GONE);
        limiteCapacidadMinima.setVisibility(View.GONE);
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
                //Voy a ver si los valores limite cumplen la condicion para enviarse. Si no, muestro mensajes
                puedoMandarValoresLimite = true;
                if(Double.parseDouble(String.valueOf(limitePeso.getText())) > PESO_MAXIMO || Double.parseDouble(String.valueOf(limitePeso.getText())) <= ZERO)
                {
                    Toast.makeText(getApplicationContext(),"El peso no puede ser mayor a 500, ni menor o igual a 0", Toast.LENGTH_LONG).show();
                    puedoMandarValoresLimite = false;
                }
                if (Double.parseDouble(String.valueOf(limiteCapacidad.getText())) <= Double.parseDouble(String.valueOf(limiteCapacidadMinima.getText())))
                {
                    Toast.makeText(getApplicationContext(),"La capacidad critica no puede ser menor o igual a la minima", Toast.LENGTH_LONG).show();
                    puedoMandarValoresLimite = false;
                }
                if (Double.parseDouble(String.valueOf(limiteCapacidadMinima.getText())) < CAPACIDAD_MINIMA)
                {
                    Toast.makeText(getApplicationContext(),"La capacidad minima no puede ser menor a 3", Toast.LENGTH_LONG).show();
                    puedoMandarValoresLimite = false;
                }
                if(puedoMandarValoresLimite)
                {
                    //Pasé todas las validaciones. Voy a mandar los valores al Arduino
                    Log.i("Ejecutando", String.valueOf(limitePeso.getText()));
                    Log.i("Ejecutando", String.valueOf(limiteCapacidad.getText()));
                    Log.i("Ejecutando", String.valueOf(limiteCapacidadMinima.getText()));
                    StringBuilder valoresLimiteParaArduino = new StringBuilder();
                    valoresLimiteParaArduino.append("A");
                    valoresLimiteParaArduino.append("|");
                    valoresLimiteParaArduino.append(limitePeso.getText());
                    valoresLimiteParaArduino.append("|");
                    valoresLimiteParaArduino.append(limiteCapacidad.getText());
                    valoresLimiteParaArduino.append("|");
                    valoresLimiteParaArduino.append(limiteCapacidadMinima.getText());
                    String valoresParaArduino = valoresLimiteParaArduino.toString();
                    mConnectedThread.write(valoresParaArduino);
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
    //Cada vez que se detecta el evento OnResume se establece la comunicacion con el HC05, creando un
    //socketBluethoot
    protected void onResume()
    {
        super.onResume();
        registerSenser();

        //Obtengo el parametro, aplicando un Bundle, que me indica la Mac Adress del HC05
        Intent intent=getIntent();
        Bundle extras=intent.getExtras();

        //Si todavia no me emparejé con ningun dispositivo, esto va a pinchar por todos lados
        //debo verificar que me haya emparejado con el Arduino
        if(extras != null)
        {
            address= extras.getString("Direccion_Bluethoot");
            if (btSocket != null) {
                try {
                    btSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = btAdapter.getRemoteDevice(address);

            //se realiza la conexion del Bluethoot crea y se conectandose a atraves de un socket
            try
            {
                btSocket = createBluetoothSocket(device);
            }
            catch (IOException e)
            {
                showToast( "La creacción del Socket fallo");
            }
            // Establish the Bluetooth socket connection.
            try
            {
                btSocket.connect();
            }
            catch (IOException e)
            {
                try
                {
                    btSocket.close();
                }
                catch (IOException e2)
                {
                    //insert code to deal with this
                }
            }

            //Una establecida la conexion con el Hc05 se crea el hilo secundario, el cual va a recibir
            // los datos de Arduino atraves del bluethoot
            mConnectedThread = new ConnectedThread(btSocket);
            mConnectedThread.start();

            //Voy a habilitar el boton para grabar solo cuando ya tenga mi conexion con arduino creada
            grabarBoton.setEnabled(true);
            grabarBoton.setBackgroundColor(Color.BLUE);
        }
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

    //Metodo que crea el socket bluethoot
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
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
            if ((Math.abs(valoresSensor[0]) > CAMBIO_PRECISION || Math.abs(valoresSensor[1]) > CAMBIO_PRECISION || Math.abs(valoresSensor[2]) > CAMBIO_PRECISION))
            {
                botonAtrasPresionado = true;
                etiquetaShake.setVisibility(View.GONE);
                etiquetaCapacidad.setVisibility(View.VISIBLE);
                etiquetaCapacidadMinima.setVisibility(View.VISIBLE);
                etiquetaPeso.setVisibility(View.VISIBLE);
                limiteCapacidad.setVisibility(View.VISIBLE);
                limiteCapacidadMinima.setVisibility(View.VISIBLE);
                limitePeso.setVisibility(View.VISIBLE);
                grabarBoton.setVisibility(View.VISIBLE);
                if (address != null)
                {
                    grabarBoton.setEnabled(true);
                }
                else
                {
                    grabarBoton.setEnabled(false);
                }
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
            etiquetaCapacidadMinima.setVisibility(View.GONE);
            etiquetaPeso.setVisibility(View.GONE);
            limiteCapacidad.setVisibility(View.GONE);
            limiteCapacidadMinima.setVisibility(View.GONE);
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

    private class ConnectedThread extends Thread
    {
        private final OutputStream mmOutStream;

        //Constructor de la clase del hilo secundario
        public ConnectedThread(BluetoothSocket socket)
        {
            OutputStream tmpOut = null;

            try
            {
                //Create I/O streams for connection
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmOutStream = tmpOut;
        }

        //metodo run del hilo, que va a entrar en una espera activa para recibir los msjs del HC05
        public void run()
        {
            //El hilo secundario no debe recibir información de Arduino en esta activity

            /*byte[] buffer = new byte[256];
            int bytes;

            //el hilo secundario se queda esperando mensajes del HC05
            while (true)
            {
                try
                {
                    //se leen los datos del Bluethoot
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);

                    //se muestran en el layout de la activity, utilizando el handler del hilo
                    // principal antes mencionado
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
            */
        }


        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                showToast("La conexion fallo");
                finish();
            }
        }
    }
}