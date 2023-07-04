package com.grupom2.wastemate.model;

import java.io.Serializable;

public class BluetoothDeviceData implements Serializable, Cloneable
{
    private String status;
    private double currentPercentage;
    private double fullPercentage;
    private double criticalPercentage;
    private double maxAllowedWeight;
    private boolean isCalibrating;

    public BluetoothDeviceData()
    {
    }

    public BluetoothDeviceData(BluetoothDeviceData source)
    {
        this.status = source.status;
        this.currentPercentage = source.currentPercentage;
        this.maxAllowedWeight = source.maxAllowedWeight;
        this.fullPercentage = source.fullPercentage;
        this.isCalibrating = source.isCalibrating;
        this.criticalPercentage = source.criticalPercentage;
    }

    public void setData(String status, double criticalPercentage, double fullPercentage, double currentPercentage, int maximumWeight, boolean isCalibrating)
    {
        this.status = status;
        this.criticalPercentage = criticalPercentage;
        this.fullPercentage = fullPercentage;
        this.currentPercentage = currentPercentage;
        this.maxAllowedWeight = maximumWeight;
        this.isCalibrating = isCalibrating;
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

    public boolean getIsCalibrating()
    {
        return isCalibrating;
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }

    public void setIsCalibrating(boolean isCalibrating)
    {
        this.isCalibrating = isCalibrating;
    }
}
