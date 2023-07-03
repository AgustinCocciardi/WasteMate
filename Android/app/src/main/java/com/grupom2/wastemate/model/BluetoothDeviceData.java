package com.grupom2.wastemate.model;

import java.io.Serializable;

public class BluetoothDeviceData implements Serializable
{
    private String status;
    private double currentPercentage;
    private double fullPercentage;
    private double criticalPercentage;
    private double maxAllowedWeight;

    public void setData(String status, double criticalPercentage, double fullPercentage, double currentPercentage, int maximumWeight)
    {
        this.status = status;
        this.criticalPercentage = criticalPercentage;
        this.fullPercentage = fullPercentage;
        this.currentPercentage = currentPercentage;
        this.maxAllowedWeight = maximumWeight;
    }

    public String getStatus()
    {
        return status;
    }

    public double getCurrentPercentage()
    {
        return currentPercentage;
    }

    public double getFullPercentage()
    {
        return fullPercentage;
    }

    public double getCriticalPercentage()
    {
        return criticalPercentage;
    }

    public double getMaxAllowedWeight()
    {
        return maxAllowedWeight;
    }
}
