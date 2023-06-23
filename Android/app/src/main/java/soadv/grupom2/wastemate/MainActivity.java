package soadv.grupom2.wastemate;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    //region Attributes
    private BluetoothService myService;
    private boolean isBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            BluetoothService.LocalBinder localBinder = (BluetoothService.LocalBinder) binder;
            myService = localBinder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }
    };
    private Button botonCapacidad;
    private ImageButton botonConfiguracion;

    private TextView txtEstado;
    private Button btnActivar;

    private ProgressDialog mProgressDlg;

    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();

//    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();

    private ConnectedThread mConnectedThread;

    // SPP UUID service  - Funciona en la mayoria de los dispositivos
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address del Hc05
    private static String address = null;

    Handler bluetoothIn;
    final int handlerState = 0; //used to identify handler message

    public static final int MULTIPLE_PERMISSIONS = 10;

    //se crea un array de String con los permisos a solicitar en tiempo de ejecucion
    //Esto se debe realizar a partir de Android 6.0, ya que con verdiones anteriores
    //con solo solicitarlos en el Manifest es suficiente

    ArrayList<String> permissions;

    ActivityResultLauncher<Intent> enableBluetoothActivityLauncher;
    ActivityResultLauncher<Intent> disableBluetoothActivityLauncher;

    private TextView capacidadText;
    private double capacidadReal = 0;
    private double capacidadCritica = 0;
    private double capacidadMinima = 0;
    private static final int CAPACIDAD_REAL_POSICION = 0;
    private static final int CAPACIDAD_CRITICA_POSICION = 1;
    private static final int CAPACIDAD_MINIMA_POSICION = 2;
    private static final int rojo = Color.RED;
    private static final int azul = Color.BLUE;
    private static final int verde = Color.GREEN;

    //endregion

    //region Constructor
    public MainActivity() {
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
    //endregion

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        botonCapacidad = (Button) findViewById(R.id.botonCapacidad);
        botonConfiguracion = (ImageButton) findViewById(R.id.botonConfiguracion);

        txtEstado = (TextView) findViewById(R.id.txtEstado);
        btnActivar = (Button) findViewById(R.id.btnActivar);

        capacidadText = (TextView) findViewById(R.id.capacidadText);
        capacidadText.setVisibility(View.GONE);

        botonCapacidad.setEnabled(false);

        //Se crea un adaptador para podermanejar el bluethoot del celular
        //mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

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

        //defino el Handler de comunicacion entre el hilo Principal  el secundario.
        //El hilo secundario va a mostrar informacion utilizando indirectamente este handler
        bluetoothIn = Handler_Msg_Hilo_Principal();

        //Boton que le enviará un comando al Arduino. El Arduino lo recibirá y deberá responder los valores de capacidad
        botonCapacidad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //showToast("Voy a mandar un pedido para ver capacidad al arduino");
                Log.i("Envio","Mando al arduino solicitud para ver la capacidad");
                mConnectedThread.write("3");
            }
        });

        //Boton que me llevará al activity de configuracion
        botonConfiguracion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Cuando esté emparejado con el arduino, le pasaré la dirección del bluetooth a esta activity
                if(address != null)
                {
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    //intent.putExtra("Direccion_Bluethoot", address);
                    startActivity(intent);
                }
                else
                {
                    //Si no estoy emparejado con el arduino, no debo enviar la direccion
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(intent);
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

    @Override
    //Cada vez que se detecta el evento OnResume se establece la comunicacion con el HC05, creando un
    //socketBluethoot
    protected void onResume()
    {
        Log.i("Ejecuto","Ejecuto OnResume");
        super.onResume();
        BluetoothManager.bindService(this);
//
//        //Obtengo el parametro, aplicando un Bundle, que me indica la Mac Adress del HC05
//        Intent intent=getIntent();
//        Bundle extras=intent.getExtras();
//
//        //Si todavia no me emparejé con ningun dispositivo, esto va a pinchar por todos lados
//        //debo verificar que me haya emparejado con el Arduino
//        if(extras != null)
//        {
//            address= extras.getString("Direccion_Bluethoot");
//
//            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
//
//            //se realiza la conexion del Bluethoot crea y se conectandose a atraves de un socket
//            try
//            {
//                btSocket = createBluetoothSocket(device);
//            }
//            catch (IOException e)
//            {
//                showToast( "La creacción del Socket fallo");
//            }
//            // Establish the Bluetooth socket connection.
//            try
//            {
//                btSocket.connect();
//            }
//            catch (IOException e)
//            {
//                try
//                {
//                    btSocket.close();
//                }
//                catch (IOException e2)
//                {
//                    //insert code to deal with this
//                }
//            }
//
//            //Una vez establecida la conexion con el Hc05 se crea el hilo secundario, el cual va a recibir
//            // los datos de Arduino atraves del bluethoot
//            mConnectedThread = new ConnectedThread(btSocket);
//            mConnectedThread.start();
//
//            //Voy a volver visible el text view para ver la capacidad tan pronto la conexión con Arduino esté establecida
//            capacidadText.setText("Presione el boton para ver capacidad");
//            capacidadText.setVisibility(View.VISIBLE);
//
//            //Voy a habilitar el boton de capacidad tan pronto como esté lista la conexion con el Arduino
//            //El botón capacidad envía un comando write por bluetooth, no debe mandar nada a conexiones no establecidas
//            botonCapacidad.setEnabled(true);
//        }
    }

    @Override
    //Cuando se ejecuta el evento onPause se cierra el socket Bluethoot, para no estar recibiendo datos
    protected void onPause() {
//        if (mBluetoothAdapter != null) {
//            if (mBluetoothAdapter.isDiscovering()) {
//                mBluetoothAdapter.cancelDiscovery();
//            }
//        }
        super.onPause();
        BluetoothManager.unbindService(this);

//        if (btSocket != null)
//        {
//            try
//            {
//                //Don't leave Bluetooth sockets open when leaving activity
//                btSocket.close();
//            } catch (IOException e2) {
//                //insert code to deal with this
//            }
//        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("Ejecuto","Ejecuto OnStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i("Ejecuto","Ejecuto OnRestart");
    }

    @Override
    //Cuando se detruye la Acivity se quita el registro de los brodcast. Apartir de este momento no se
    //recibe mas broadcast del SO. del bluethoot
    public void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, BluetoothService.class));

    }

    protected  void enableComponent()
    {

        //se determina si existe bluethoot en el celular
//        if (mBluetoothAdapter == null)
//        {
//            //si el celular no soporta bluethoot
//            showUnsupported();
//        }
//        else
//        {
//            //si el celular soporta bluethoot, se definen los listener para los botones de la activity
//            btnActivar.setOnClickListener(btnActivarListener);
//
//            //se determina si esta activado el bluethoot
//            if (mBluetoothAdapter.isEnabled())
//            {
//                //se informa si esta habilitado
//                showEnabled();
//            }
//            else
//            {
//                //se informa si esta deshabilitado
//                showDisabled();
//            }
//        }
    }

    private void showEnabled() {
        txtEstado.setText("Bluetooth Activado");
        txtEstado.setTextColor(Color.BLUE);

        btnActivar.setText("Desactivar");
        btnActivar.setEnabled(true);
    }

    private void showDisabled() {
        txtEstado.setText("Bluetooth Desactivado");
        txtEstado.setTextColor(Color.RED);

        btnActivar.setText("Activar");
        btnActivar.setEnabled(true);

        capacidadText.setText("");
        capacidadText.setVisibility(View.GONE);

        botonCapacidad.setEnabled(false);
    }

    private void showUnsupported() {
        txtEstado.setText("Bluetooth no es soportado por el dispositivo movil");

        btnActivar.setText("Activar");
        btnActivar.setEnabled(false);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }


    private View.OnClickListener btnActivarListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
//            if (mBluetoothAdapter.isEnabled()) {
//                disableBluetooth();
//            } else {
//                enableBluetooth();
//            }
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

    private void enableBluetoothCallback(){
        showEnabled();
    }

    private void disableBluetoothCallback(){
        showDisabled();
    }

    private DialogInterface.OnClickListener btnCancelarDialogListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
//            mBluetoothAdapter.cancelDiscovery();
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

    //Handler que sirve que permite mostrar datos en el Layout al hilo secundario
    private Handler Handler_Msg_Hilo_Principal ()
    {
        return new Handler() {
            public void handleMessage(android.os.Message msg)
            {
                //si se recibio un msj del hilo secundario
                if (msg.what == handlerState)
                {
                    //voy concatenando el msj
                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);
                    int endOfLineIndex = recDataString.indexOf("\r\n");

                    //cuando recibo toda una linea, lleno el valor del text view de capacidad
                    if (endOfLineIndex > 0)
                    {
                        //copio la cadena recibida por el bluetooth
                        String datosRecibidosBluetooth = recDataString.substring(0, endOfLineIndex);
                        //divido la cadena recibida en 3 partes
                        String[] partes = datosRecibidosBluetooth.split("\\|");
                        capacidadReal = Double.parseDouble(partes[CAPACIDAD_REAL_POSICION]);
                        capacidadCritica = Double.parseDouble(partes[CAPACIDAD_CRITICA_POSICION]);
                        capacidadMinima = Double.parseDouble(partes[CAPACIDAD_MINIMA_POSICION]);

                        if (capacidadReal >= capacidadMinima)
                        {
                            capacidadText.setText("Sin Capacidad");
                            capacidadText.setTextColor(rojo);
                        }
                        else if(capacidadReal >= capacidadCritica)
                        {
                            capacidadText.setText("Capacidad Critica");
                            capacidadText.setTextColor(azul);
                        }
                        else
                        {
                            capacidadText.setText("Con Capacidad");
                            capacidadText.setTextColor(verde);
                        }
                        recDataString.delete(0, recDataString.length());
                    }
                }
            }
        };
    }

    //Metodo que crea el socket bluethoot
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    private class ConnectedThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //Constructor de la clase del hilo secundario
        public ConnectedThread(BluetoothSocket socket)
        {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try
            {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        //metodo run del hilo, que va a entrar en una espera activa para recibir los msjs del HC05
        public void run()
        {
            byte[] buffer = new byte[256];
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