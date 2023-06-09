package com.grupom2.wastemate.activity;

import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.grupom2.wastemate.R;
import com.grupom2.wastemate.constant.Constants;
import com.grupom2.wastemate.util.PermissionHelper;

import java.util.ArrayList;

public class PermissionsMissingActivity extends AppCompatActivity
{
    //region Fields
    //region Controls
    ArrayAdapter<String> itemsAdapter;
    //endregion Controls

    //region Listeners
    private final View.OnClickListener btnOpenConfigurationOnClickListener;
    //endregion Listeners

    //endregion Other Fields
    private boolean hasAllPermissions;
    //endregion Other Fields
    //endregion Fields

    //region Constructor
    public PermissionsMissingActivity()
    {
        btnOpenConfigurationOnClickListener = this::btnOpenConfigurationOnClickListener;
    }
    //endregion Constructor

    //region Overrides
    //region Activity Life Cycle
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //Se asigna un layout al activity para poder vincular los distintos componentes
        setContentView(R.layout.activity_permissions_missing);

        //Se configura el comportamiento del botón de abrir configuración.
        Button btnOpenConfiguration = findViewById(R.id.button);
        btnOpenConfiguration.setOnClickListener(btnOpenConfigurationOnClickListener);

        //Se obtienen los permisos faltantes del intent
        ArrayList<String> missingPermissions = getIntent().getExtras().getStringArrayList(Constants.MISSING_PERMISSIONS_KEY);

        //Se cargan los permisos faltantes en el list view
        ListView listViewPermissionsMissing = findViewById(R.id.list_view_permissions_missing);
        itemsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        ArrayList<String> missingPermissionsName = getMissingPermissionsName(missingPermissions);
        itemsAdapter.addAll(missingPermissionsName);
        listViewPermissionsMissing.setAdapter(itemsAdapter);
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();

        ArrayList<String> missingPermissions = PermissionHelper.getPermissionsMissing(this);
        hasAllPermissions = missingPermissions.isEmpty();

        if (hasAllPermissions)
        {
            onBackPressed();
        }
        else
        {
            itemsAdapter.clear();
            ArrayList<String> missingPermissionsName = getMissingPermissionsName(missingPermissions);
            itemsAdapter.addAll(missingPermissionsName);
            itemsAdapter.notifyDataSetChanged();
        }
    }
    //endregion Activity Life Cycle

    //region Other Overrides
    @Override
    public void onBackPressed()
    {
        ArrayList<String> missingPermissions = PermissionHelper.getPermissionsMissing(this);
        hasAllPermissions = missingPermissions.isEmpty();
        if (hasAllPermissions)
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

    //region Listeners
    private void btnOpenConfigurationOnClickListener(View v)
    {
        PermissionHelper.openAppSettings(PermissionsMissingActivity.this);
    }
    //endregion Listeners

    //region Other Methods
    private ArrayList<String> getMissingPermissionsName(ArrayList<String> permissionsDenied)
    {
        ArrayList<String> missingPermissions = new ArrayList<>();
        PackageManager packageManager = getApplicationContext().getPackageManager();
        for (String permission : permissionsDenied)
        {
            String permissionName;
            try
            {
                //Se obtiene el nombre para mostrar del permiso
                PermissionInfo permissionInfo = packageManager.getPermissionInfo(permission, 0);
                permissionName = permissionInfo.loadLabel(packageManager).toString();
            }
            catch (PackageManager.NameNotFoundException e)
            {
                //Si no se encuentra, se muestra el nombre por default
                permissionName = permission;
            }
            missingPermissions.add(permissionName);
        }
        return missingPermissions;
    }
    //endregion Other Methods
}