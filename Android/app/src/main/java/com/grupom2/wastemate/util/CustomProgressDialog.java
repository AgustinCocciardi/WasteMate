package com.grupom2.wastemate.util;

import android.app.Dialog;
import android.content.Context;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;

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
        setCancelable(false);
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(layoutParams);
    }
}