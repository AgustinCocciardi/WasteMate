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
import android.content.SharedPreferences;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity{
    //region Attributes
    private BluetoothService myService;
    private boolean isBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            BluetoothService.LocalBinder localBinder = (BluetoothService.LocalBinder) binder;
            myService = localBinder.getService();
            if(!myService.getAdapter().isEnabled()){
                Intent btintent = new Intent(MainActivity.this, BluetoothDisabledActivity.class);
                btintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(btintent);
            }
            //get SharedPreferences from getSharedPreferences("name_file", MODE_PRIVATE)
            SharedPreferences shared = getSharedPreferences("info",MODE_PRIVATE);
            //Using getXXX- with XX is type date you wrote to file "name_file"
            String string_temp = shared.getString("connectedDevice", null);
            if(string_temp!= null){
                BluetoothDevice device = myService.getAdapter().getRemoteDevice(string_temp);
                myService.connectToDevice(device);
            }
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }
    };
    private Button botonCapacidad;
    private Button btnIniciarMantenimiento;
    private Button btnMantenimientoFinalizado;
    private Button btnDisable;
    private ImageButton botonConfiguracion;

    private ProgressDialog mProgressDlg;


    private StringBuilder recDataString = new StringBuilder();

    Handler bluetoothIn;
    final int handlerState = 0; //used to identify handler message

    public static final int MULTIPLE_PERMISSIONS = 10;

    //se crea un array de String con los permisos a solicitar en tiempo de ejecucion
    //Esto se debe realizar a partir de Android 6.0, ya que con verdiones anteriores
    //con solo solicitarlos en el Manifest es suficiente

    ArrayList<String> permissions;
    ActivityResultLauncher<Intent> disableBluetoothActivityLauncher;

    private TextView capacidadText;
    private double capacidadReal = 0;
    private double capacidadCritica = 0;
    private double capacidadMinima = 0;
    private static final int CAPACIDAD_REAL_POSICION = 0;
    private static final int CAPACIDAD_CRITICA_POSICION = 1;
    private static final int CAPACIDAD_MINIMA_POSICION = 2;

    private BroadcastReceiver broadcastReceiver;
    private BroadcastReceiver bluetoothStatusChangedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    // Bluetooth is enabled
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    Intent btintent = new Intent(MainActivity.this, BluetoothDisabledActivity.class);
                    btintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(btintent);
                }
            } // Handle Bluetooth state change
        }
    };
    private IntentFilter bluetoothStatusChangedFilter;

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

        btnIniciarMantenimiento = (Button) findViewById(R.id.btnIniciarMantenimiento);
        btnMantenimientoFinalizado = (Button) findViewById(R.id.btnMantenimientoFinalizado);
        btnDisable = (Button) findViewById(R.id.btnDisable);
        capacidadText = (TextView) findViewById(R.id.capacidadText);
        //capacidadText.setVisibility(View.GONE);

        botonCapacidad.setEnabled(true);

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
                //Create a object SharedPreferences from getSharedPreferences("name_file",MODE_PRIVATE) of Context
                pref = getSharedPreferences("info", MODE_PRIVATE);
                //Using putXXX - with XXX is type data you want to write like: putString, putInt...   from      Editor object

                SharedPreferences.Editor editor = pref.edit();
                editor.putString("connectedDevice",myService.getDevice().getAddress());
                //finally, when you are done saving the values, call the commit() method.
                editor.commit();
                //showToast("Voy a mandar un pedido para ver capacidad al arduino");
                Log.i("Envio","Mando al arduino solicitud para ver la capacidad");
                BluetoothMessage bluetoothMessage = new BluetoothMessage(0);
                BluetoothService.getInstance().sendData(bluetoothMessage.Serialize());
            }
        });

        //Boton que me llevará al activity de configuracion
        botonConfiguracion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });

        disableBluetoothActivityLauncher= registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                (result) -> disableBluetoothCallback());

        Intent serviceIntent = new Intent(this, BluetoothService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

        //startService(serviceIntent);

        // Register the broadcast receiver to receive messages from the service
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = intent.getStringExtra("status");
                Log.d(BluetoothService.TAG, "Received message: " + message);
                if (message == "CON CAPACIDAD")
                {
                    capacidadText.setTextColor(Color.GREEN);
                }
                else if(message == "CAPACIDAD CRITICA")
                {
                    capacidadText.setTextColor(Color.YELLOW);
                }
                else if(message == "SIN CAPACIDAD")
                {
                    capacidadText.setTextColor(Color.RED);
                }
                else  if(message == "MANTENIMIENTO"){
                    capacidadText.setTextColor(Color.BLUE);
                }
                capacidadText.setText(message);
            }
        };

        IntentFilter filter = new IntentFilter(Actions.CUSTOM_ACTION_STATUS_CHANGED);
        registerReceiver(broadcastReceiver, filter);

        bluetoothStatusChangedFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStatusChangedBroadcastReceiver, bluetoothStatusChangedFilter);

        // Access the singleton instance of the service
        myService = BluetoothService.getInstance();

        btnIniciarMantenimiento.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Envio","Envio mensaje de inicio mantenimiento");
                BluetoothMessage bluetoothMessage = new BluetoothMessage(0);
                BluetoothService.getInstance().sendData(bluetoothMessage.Serialize());
            }
        });

        btnMantenimientoFinalizado.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Envio","Envio mensaje de fin de mantenimiento");
                BluetoothMessage bluetoothMessage = new BluetoothMessage(1);
               BluetoothService.getInstance().sendData(bluetoothMessage.Serialize());
            }
        });

        btnDisable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Envio","Envio mensaje de desahabilitar????");
                BluetoothMessage bluetoothMessage = new BluetoothMessage(2);
               BluetoothService.getInstance().sendData(bluetoothMessage.Serialize());
            }
        });
    }

    @Override
    //Cada vez que se detecta el evento OnResume se establece la comunicacion con el HC05, creando un
    //socketBluethoot
    protected void onResume()
    {
        Log.i("Ejecuto","Ejecuto OnResume");
        super.onResume();
        registerReceiver(bluetoothStatusChangedBroadcastReceiver, bluetoothStatusChangedFilter);
        BluetoothService bt = BluetoothService.getInstance();
        if(myService != null){
            if(!myService.getAdapter().isEnabled()){
                Intent btintent = new Intent(MainActivity.this, BluetoothDisabledActivity.class);
                btintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(btintent);
            }
        }
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
        unregisterReceiver(bluetoothStatusChangedBroadcastReceiver);
        Log.i("Ejecuto","Ejecuto OnStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i("Ejecuto","Ejecuto OnRestart");
    }
    private SharedPreferences pref;

    @Override
    //Cuando se detruye la Acivity se quita el registro de los brodcast. Apartir de este momento no se
    //recibe mas broadcast del SO. del bluethoot
    public void onDestroy()
    {
        BluetoothService.getInstance().disconnect();

        super.onDestroy();
        Intent serviceIntent = new Intent(this, BluetoothService.class);
        unbindService(serviceConnection);
        //stopService(serviceIntent);
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

    private void showDisabled() {
        //capacidadText.setText("");
        //capacidadText.setVisibility(View.GONE);

        botonCapacidad.setEnabled(false);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
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
                            capacidadText.setTextColor(Color.RED);
                        }
                        else if(capacidadReal >= capacidadCritica)
                        {
                            capacidadText.setText("Capacidad Critica");
                            capacidadText.setTextColor(Color.BLUE);
                        }
                        else
                        {
                            capacidadText.setText("Con Capacidad");
                            capacidadText.setTextColor(Color.GREEN);
                        }
                        recDataString.delete(0, recDataString.length());
                    }
                }
            }
        };
    }
}