package com.grupom2.wastemate.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.grupom2.wastemate.constant.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PermissionHelper
{
    private static final List<String> permissionsNeeded;

    static
    {
        permissionsNeeded = Collections.unmodifiableList(new ArrayList<String>()
        {{
            add(Manifest.permission.ACCESS_COARSE_LOCATION);
            add(Manifest.permission.ACCESS_FINE_LOCATION);
            add(Manifest.permission.READ_PHONE_STATE);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            {
                add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            else
            {
                add(Manifest.permission.READ_MEDIA_AUDIO);
                add(Manifest.permission.READ_MEDIA_VIDEO);
                add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            {
                add(Manifest.permission.BLUETOOTH_SCAN);
                add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            else
            {
                add(Manifest.permission.BLUETOOTH);
                add(Manifest.permission.BLUETOOTH_ADMIN);
            }
        }});
    }

    @NonNull
    public static List<String> getPermissionsNeeded()
    {
        return permissionsNeeded;
    }

    // Check if a specific permission is granted
    public static boolean isPermissionGranted(Context context, String permission)
    {
        int result = context.checkCallingOrSelfPermission(permission);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    // Check if a specific permission is granted
    public static boolean isPermissionGranted(Context context, ArrayList<String> permissions, ArrayList<String> missingPermissions)
    {
        missingPermissions = new ArrayList<>();
        for (String permission : permissions)
        {
            int result = context.checkCallingOrSelfPermission(permission);
            if (result != PackageManager.PERMISSION_GRANTED)
            {
                missingPermissions.add(permission);
            }
        }

        return missingPermissions.isEmpty();
    }

    // Open app settings screen
    public static void openAppSettings(Context context)
    {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static boolean checkPermissions(Activity context)
    {
        List<String> permissionsMissing = getPermissionsMissing(context);

        boolean hasAllPermissions;
        if (!permissionsMissing.isEmpty())
        {
            // Intentar solicitar permisos faltantes al usuario.
            ActivityCompat.requestPermissions(context,
                    permissionsMissing.toArray(new String[permissionsMissing.size()]),
                    Constants.MULTIPLE_PERMISSIONS);

            // Como la operación es asíncrona, todavía no se sabe si se tienen todos los permisos.
            hasAllPermissions = false;
        }
        else
        {
            //Tiene todos los permisos necesarios.
            hasAllPermissions = true;
        }
        return hasAllPermissions;
    }

    @NonNull
    private static List<String> getPermissionsMissing(Context context)
    {
        List<String> permissionsMissing = new ArrayList<>();

        for (String p : getPermissionsNeeded())
        {
            int result = ContextCompat.checkSelfPermission(context, p);
            if (result != PackageManager.PERMISSION_GRANTED)
            {
                permissionsMissing.add(p);
            }
        }
        return permissionsMissing;
    }
}
