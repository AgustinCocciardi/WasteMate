package soadv.grupom2.wastemate;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.w3c.dom.Text;

public class Sensores extends Activity implements SensorEventListener, CompoundButton.OnCheckedChangeListener {

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

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensores);
        sensor = (SensorManager) getSystemService(SENSOR_SERVICE);
        etiquetaPeso = findViewById(R.id.etiquetaPeso);
        etiquetaCapacidad = findViewById(R.id.etiquetaCapacidad);
        etiquetaShake = findViewById(R.id.etiquetaShake);
        limitePeso = findViewById(R.id.limitePeso);
        limitePeso.setText(Double.toString(pesoMaximo));
        limiteCapacidad = findViewById(R.id.limiteCapacidad);
        limiteCapacidad.setText(Double.toString(capacidadMaxima));
        grabarBoton = (Button) findViewById(R.id.botonGrabar);
        etiquetaShake.setVisibility(View.VISIBLE);
        etiquetaCapacidad.setVisibility(View.GONE);
        etiquetaPeso.setVisibility(View.GONE);
        limiteCapacidad.setVisibility(View.GONE);
        limitePeso.setVisibility(View.GONE);
        grabarBoton.setVisibility(View.GONE);
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
    protected void onPause()
    {
        unregisterSenser();
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int tipoSensor = sensorEvent.sensor.getType();
        float valoresSensor[] = sensorEvent.values;
        if (tipoSensor == Sensor.TYPE_ACCELEROMETER)
        {
            if ((Math.abs(valoresSensor[0]) > cambioPrecision || Math.abs(valoresSensor[1]) > cambioPrecision || Math.abs(valoresSensor[2]) > cambioPrecision))
            {
                etiquetaShake.setVisibility(View.GONE);
                etiquetaCapacidad.setVisibility(View.VISIBLE);
                etiquetaPeso.setVisibility(View.VISIBLE);
                limiteCapacidad.setVisibility(View.VISIBLE);
                limitePeso.setVisibility(View.VISIBLE);
                grabarBoton.setVisibility(View.VISIBLE);
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

}