package com.grupom2.wastemate.model;

import java.io.Serializable;

public class BluetoothDeviceData implements Serializable
{
    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public double getCurrentPercentage()
    {
        return currentPercentage;
    }

    public void setCurrentPercentage(double currentPercentage)
    {
        this.currentPercentage = currentPercentage;
    }

    public double getFullPercentage()
    {
        return fullPercentage;
    }

    public void setFullPercentage(double fullPercentage)
    {
        this.fullPercentage = fullPercentage;
    }

    public double getCriticalPercentage()
    {
        return criticalPercentage;
    }

    public void setCriticalPercentage(double criticalPercentage)
    {
        this.criticalPercentage = criticalPercentage;
    }

    public double getMaxAllowedWeight()
    {
        return maxAllowedWeight;
    }

    public void setMaxAllowedWeight(double maxAllowedWeight)
    {
        this.maxAllowedWeight = maxAllowedWeight;
    }

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
}
