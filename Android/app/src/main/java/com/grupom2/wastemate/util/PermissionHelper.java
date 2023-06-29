package com.grupom2.wastemate.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import java.util.ArrayList;

public class PermissionHelper
{
    private static final String TAG = "PermissionHelper";

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
            if(result != PackageManager.PERMISSION_GRANTED){
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
}
