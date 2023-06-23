package soadv.grupom2.wastemate;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.appcompat.widget.SwitchCompat;

public class BluetoothSwitchCompat extends SwitchCompat {

    private String textOn;
    private String textOff;

    public BluetoothSwitchCompat(Context context) {
        super(context);
        init(context, null);
    }

    public BluetoothSwitchCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BluetoothSwitchCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BluetoothSwitchCompat);
            textOn = a.getString(R.styleable.BluetoothSwitchCompat_switchTextOn);
            textOff = a.getString(R.styleable.BluetoothSwitchCompat_switchTextOff);
            a.recycle();
        }

        updateSwitchLabel(isChecked());
    }

    public void setSwitchLabels(String textOn, String textOff) {
        this.textOn = textOn;
        this.textOff = textOff;
        updateSwitchLabel(isChecked());
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);
        updateSwitchLabel(checked);
    }

    private void updateSwitchLabel(boolean isChecked) {
        if (isChecked) {
            setText(textOn!= null? textOn:"");

        } else {
            setText(textOff!= null? textOff:"");
        }
    }
}

