package com.grupom2.wastemate.model;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class BluetoothMessageResponse implements Serializable
{
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

    public int getContainerSize()
    {
        return containerSize;
    }

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
    @SerializedName("cs")
    int containerSize;
    @SerializedName("p")
    private double currentPercentage;

    public static BluetoothMessageResponse fromJson(String serializedData)
    {
        Gson gson = new Gson();
        BluetoothMessageResponse response = gson.fromJson(serializedData, BluetoothMessageResponse.class);
        return response;
    }

    public double getCurrentPercentage()
    {
        return currentPercentage > 0 ? currentPercentage * 100 : 0;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }
}
