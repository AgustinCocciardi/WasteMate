package com.grupom2.wastemate.util;

import android.content.Context;
import android.content.Intent;

import com.grupom2.wastemate.activity.BluetoothDisabledActivity;
import com.grupom2.wastemate.activity.PermissionsMissingActivity;
import com.grupom2.wastemate.activity.SettingsActivity;
import com.grupom2.wastemate.constant.Constants;

import java.util.ArrayList;

public class NavigationUtil
{
    public static void navigateToSettingsActivity(Context context)
    {
        Intent intent = new Intent(context, SettingsActivity.class);
        context.startActivity(intent);
    }

    public static void navigateToMissingPermissionsActivity(Context context, ArrayList<String> missingPermissions)
    {
        Intent intent = new Intent(context, PermissionsMissingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putStringArrayListExtra(Constants.MISSING_PERMISSIONS_KEY, missingPermissions);
        context.startActivity(intent);
    }

    public static void navigateToBluetoothRequiredActivity(Context context)
    {
        Intent intent = new Intent(context, BluetoothDisabledActivity.class);
        context.startActivity(intent);
    }
}
