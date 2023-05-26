package soadv.grupom2.wastemate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.w3c.dom.Text;

public class Sensores extends AppCompatActivity {

    private final static double pesoMaximo = 150;
    private final static double capacidadMaxima = 50;
    private TextView limitePeso;
    private TextView limiteCapacidad;
    private Button grabarBoton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensores);
        limitePeso = findViewById(R.id.limitePeso);
        limitePeso.setText(Double.toString(pesoMaximo));
        limiteCapacidad = findViewById(R.id.limiteCapacidad);
        limiteCapacidad.setText(Double.toString(capacidadMaxima));
        grabarBoton = (Button) findViewById(R.id.botonGrabar);
        grabarBoton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Double.parseDouble(String.valueOf(limitePeso.getText())) > pesoMaximo || Double.parseDouble(String.valueOf(limiteCapacidad.getText())) > capacidadMaxima)
                {
                    Toast.makeText(getApplicationContext(),"Excedio los valores de capacidad o peso. \nPeso maximo: 150. Capacidad maxima: 50", Toast.LENGTH_LONG).show();
                }
                else
                {
                    Log.i("Ejecutando", String.valueOf(limitePeso.getText()));
                    Log.i("Ejecutando", String.valueOf(limiteCapacidad.getText()));
                    //Comentario para probar commits
                }
            }
        });
    }




}