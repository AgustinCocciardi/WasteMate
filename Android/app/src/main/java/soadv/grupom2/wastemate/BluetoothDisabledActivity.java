package soadv.grupom2.wastemate;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class BluetoothDisabledActivity extends AppCompatActivity
{
    ActivityResultLauncher<Intent> enableBluetoothActivityLauncher;

    private final View.OnClickListener btnEnableBluetoothOnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Intent intent = new Intent("android.bluetooth.adapter.action.REQUEST_ENABLE");
            enableBluetoothActivityLauncher.launch(intent);
        }
    };

    @Override
    public void onBackPressed()
    {
        finishAffinity();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_disabled);

        Button btnEnableBluetooth = findViewById(R.id.button_activate_bluetooth);
        enableBluetoothActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                (result) -> enableBluetoothCallback(result));
        btnEnableBluetooth.setOnClickListener(btnEnableBluetoothOnClickListener);

    }

    private void enableBluetoothCallback(ActivityResult result)
    {
        if (result.getResultCode() == Activity.RESULT_OK)
        {
            onBackPressed();
        }
    }
}
