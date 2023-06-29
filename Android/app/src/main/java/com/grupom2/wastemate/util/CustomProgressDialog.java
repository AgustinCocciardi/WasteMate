package com.grupom2.wastemate.util;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.grupom2.wastemate.R;


public class CustomProgressDialog extends Dialog
{

    public CustomProgressDialog(@NonNull Context context)
    {
        super(context);
        init();
    }

    private void init()
    {
        setContentView(R.layout.custom_dialog_progress);
        setCancelable(false); // Optional: Set whether the dialog can be canceled with the back button

        // Adjust the dialog's dimensions to create a square shape
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(layoutParams);
    }

    // You can add additional methods to update the progress, handle specific cases, etc.
}