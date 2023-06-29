package com.grupom2.wastemate.model;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class BluetoothMessage
{
    @SerializedName(value = "c")
    int code;
    @SerializedName(value = "d")
    BluetoothData data;

    public BluetoothMessage(int code, int maximumWeight, double fullPercentage, double criticalPercentage)
    {
        this.code = code;
        this.data = new BluetoothData(maximumWeight, fullPercentage, criticalPercentage);
    }

    public BluetoothMessage(int code)
    {
        this.code = code;
    }

    public String Serialize()
    {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}


