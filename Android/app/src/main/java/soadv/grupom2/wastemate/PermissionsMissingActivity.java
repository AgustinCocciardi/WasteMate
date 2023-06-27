package soadv.grupom2.wastemate;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class PermissionsMissingActivity extends AppCompatActivity {
    private final View.OnClickListener btnOpenConfigurationOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        }
    };

    @Override
    public void onBackPressed(){
        finishAffinity();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        //Se asigna un layout al activity para poder vincular los distintos componentes
        setContentView(R.layout.activity_permissions_missing);

        Button btnOpenConfiguration = findViewById(R.id.button);
        btnOpenConfiguration.setOnClickListener(btnOpenConfigurationOnClickListener);

        ListView listViewPermissionsMissing = findViewById(R.id.list_view_permissions_missing);
        //get the intent in the target activity
        Intent intent = getIntent();

        //get the attached bundle from the intent
        Bundle extras = intent.getExtras();

        //Extracting the stored data from the bundle
        ArrayList<String> permissionsDenied = extras.getStringArrayList("deniedPermissions");

        PackageManager packageManager = getApplicationContext().getPackageManager();

            ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);            for (String permission:permissionsDenied) {
            String permissionName;
            try {
                PermissionInfo permissionInfo = packageManager.getPermissionInfo(permission, 0);
                permissionName = permissionInfo.loadLabel(packageManager).toString();
            } catch (PackageManager.NameNotFoundException e) {
                permissionName = permission;
            }
            itemsAdapter.add(permissionName);
        }

        listViewPermissionsMissing.setAdapter(itemsAdapter);
    }
}
