package soadv.grupom2.wastemate;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.os.Build;
import android.os.Bundle;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    //region Attributes

    private Button botonCapacidad;
    private ImageButton botonConfiguracion;

    private TextView txtEstado;
    private Button btnActivar;

    private ProgressDialog mProgressDlg;

    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();

    private BluetoothAdapter mBluetoothAdapter;

    public static final int MULTIPLE_PERMISSIONS = 10;

    //se crea un array de String con los permisos a solicitar en tiempo de ejecucion
    //Esto se debe realizar a partir de Android 6.0, ya que con verdiones anteriores
    //con solo solicitarlos en el Manifest es suficiente

    ArrayList<String> permissions;

    ActivityResultLauncher<Intent> enableBluetoothActivityLauncher;
    ActivityResultLauncher<Intent> disableBluetoothActivityLauncher;

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

        //Botones que me llevarán a las otras activities
        botonCapacidad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, Capacidad.class);
                startActivity(intent);
            }
        });
        botonConfiguracion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, Sensores.class);
                startActivity(intent);
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
    protected void onResume()
    {
        Log.i("Ejecuto","Ejecuto OnResume");
        super.onResume();
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
            btnActivar.setOnClickListener(btnActivarListener);

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
            if (mBluetoothAdapter.isEnabled()) {
                disableBluetooth();
            } else {
                enableBluetooth();
            }
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
}