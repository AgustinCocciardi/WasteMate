package com.grupom2.wastemate.model;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class BluetoothMessageResponse implements Serializable
{
    @SerializedName("c")
    private
    String code;
    @SerializedName("cp")
    double criticalPercentage;
    @SerializedName("fp")
    double fullPercentage;
    @SerializedName("mw")
    int maximumWeight;
    @SerializedName("d")
    String data;
    @SerializedName("p")
    private double currentPercentage;
    @SerializedName("ic")
    private boolean isCalibrating;

    public static BluetoothMessageResponse fromJson(String serializedData) throws IllegalStateException
    {
        try
        {
            Gson gson = new Gson();
            BluetoothMessageResponse response = gson.fromJson(serializedData, BluetoothMessageResponse.class);
            return response;
        }
        catch (Exception e)
        {
            Log.e("BluetoothMessageResponse.fromJson", serializedData);
            return null;
        }
    }

    public double getCurrentPercentage()
    {
        return currentPercentage > 0 ? currentPercentage * 100 : 0;
    }

    public String getCode()
    {
        return code;
    }

    public double getCriticalPercentage()
    {
        return criticalPercentage;
    }

    public double getFullPercentage()
    {
        return fullPercentage;
    }

    public int getMaximumWeight()
    {
        return maximumWeight;
    }

    public String getData()
    {
        return data;
    }

    public boolean getIsCalibrating()
    {
        return isCalibrating;
    }
}
