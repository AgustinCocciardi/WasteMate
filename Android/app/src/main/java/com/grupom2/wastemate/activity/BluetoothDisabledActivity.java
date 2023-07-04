package com.grupom2.wastemate.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.grupom2.wastemate.R;
import com.grupom2.wastemate.bluetooth.BluetoothManager;
import com.grupom2.wastemate.bluetooth.BluetoothService;
import com.grupom2.wastemate.constant.Actions;
import com.grupom2.wastemate.constant.Constants;
import com.grupom2.wastemate.receiver.FilteredBroadcastReceiver;
import com.grupom2.wastemate.util.BroadcastUtil;

public class BluetoothDisabledActivity extends AppCompatActivity
{
    //region Fields

    //region Intent Launcher
    ActivityResultLauncher<Intent> enableBluetoothActivityLauncher;
    //endregion Intent Launcher

    //region Listeners
    private final View.OnClickListener btnEnableBluetoothOnClickListener;
    //endregion Listeners

    //region Broadcast Receivers
    private final BluetoothEnabledBroadcastReceiver bluetoothEnabledBroadcastReceiver;
    //endregion Broadcast Receivers

    //region Other Fields
    private boolean isBluetoothEnabled;
    BluetoothManager bluetoothManager;
    //endregion Other Fields

    //endregion Fields

    //region Constructor
    public BluetoothDisabledActivity()
    {
        btnEnableBluetoothOnClickListener = this::btnEnableBluetoothOnClickListener;
        bluetoothEnabledBroadcastReceiver = new BluetoothEnabledBroadcastReceiver();
    }
    //endregion Constructor

    //region Overrides
    //region Activity Life Cycle
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_disabled);

        Button btnEnableBluetooth = findViewById(R.id.button_activate_bluetooth);
        enableBluetoothActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::enableBluetoothCallback);
        btnEnableBluetooth.setOnClickListener(btnEnableBluetoothOnClickListener);
        BroadcastUtil.registerReceiver(this, bluetoothEnabledBroadcastReceiver);

        bluetoothManager = BluetoothService.getInstance();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (isBluetoothEnabled)
        {
            onBackPressed();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        BroadcastUtil.unregisterReceiver(this, bluetoothEnabledBroadcastReceiver);
    }

    //endregion Activity Life Cycle

    //region Other Overrides
    @Override
    public void onBackPressed()
    {
        if (isBluetoothEnabled)
        {
            super.onBackPressed();
        }
        else
        {
            finishAffinity();
            finish();
        }

    }
    //endregion Other Overrides
    //endregion Overrides

    //region Callbacks
    private void enableBluetoothCallback(ActivityResult result)
    {
        if (result.getResultCode() == Activity.RESULT_OK)
        {
            isBluetoothEnabled = true;
            onBackPressed();
        }
    }
    //endregion Callbacks

    //region Broadcast Receive
    private class BluetoothEnabledBroadcastReceiver extends FilteredBroadcastReceiver
    {

        public BluetoothEnabledBroadcastReceiver()
        {
            setFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
            {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON)
                {
                    isBluetoothEnabled = true;
                }
            }
        }
    }
    //endregion Broadcast Receivers

    //region Listeners
    private void btnEnableBluetoothOnClickListener(View v)
    {
        Intent intent = new Intent(Actions.ANDROID_ACTION_REQUEST_ENABLE_BLUETOOTH);
        enableBluetoothActivityLauncher.launch(intent);
    }
    //endregion Listeners
}
