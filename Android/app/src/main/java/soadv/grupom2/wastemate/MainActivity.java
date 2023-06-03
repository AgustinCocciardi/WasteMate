package soadv.grupom2.wastemate;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private Button boton;
    private ImageButton botonCapacidad;

    private TextView estadoBluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boton = (Button) findViewById(R.id.boton);
        botonCapacidad = (ImageButton) findViewById(R.id.botonCapacidad);
        estadoBluetooth = (TextView) findViewById(R.id.bluetoothEstado);
        boton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, Sensores.class);
                startActivity(intent);
            }
        });
        botonCapacidad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, Capacidad.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume()
    {
        Log.i("Ejecuto","Ejecuto OnResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.i("Ejecuto","Ejecuto OnPause");
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
    protected void onDestroy() {
        super.onDestroy();
        Log.i("Ejecuto","Ejecuto OnDestroy");
    }

}