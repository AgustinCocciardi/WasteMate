package soadv.grupom2.wastemate;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import java.text.DecimalFormat;

public class Capacidad extends AppCompatActivity {

    private double capacidadReal = 80;
    private final static double capacidadMaxima = 150;
    private final static int pasarAPorcentaje = 100;
    private final static int tresCuartos = 75;
    private final static int unCuarto = 25;
    private final static int rojo = Color.RED;
    private final static int azul = Color.BLUE;
    private final static int verde = Color.GREEN;
    private TextView capacidadText;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_capacidad);
        capacidadText = findViewById(R.id.mostrarCapacidad);
        DecimalFormat formatoDecimal = new DecimalFormat("#.00");
        capacidadText.setText(Double.toString(Double.parseDouble(formatoDecimal.format(capacidadReal/capacidadMaxima))*pasarAPorcentaje)+" %");
        int color = capacidadReal/capacidadMaxima*(pasarAPorcentaje) > tresCuartos ? rojo : capacidadReal/capacidadMaxima*(pasarAPorcentaje) < unCuarto ? verde : azul;
        capacidadText.setTextColor(color);
    }
}