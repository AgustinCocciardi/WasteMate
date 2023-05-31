package soadv.grupom2.wastemate;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;

import java.text.DecimalFormat;

public class Capacidad extends AppCompatActivity {

    private double capacidadReal = 80;
    private final static double capacidadMaxima = 150;
    private TextView capacidadText;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capacidad);
        capacidadText = findViewById(R.id.mostrarCapacidad);
        //capacidadText.setText(Double.toString(capacidadReal/capacidadMaxima));
        DecimalFormat formatoDecimal = new DecimalFormat("#.00");
        capacidadText.setText(Double.toString(Double.parseDouble(formatoDecimal.format(capacidadReal/capacidadMaxima))*100)+" %");
    }
}