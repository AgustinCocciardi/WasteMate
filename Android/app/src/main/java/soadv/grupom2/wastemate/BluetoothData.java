package soadv.grupom2.wastemate;

import com.google.gson.annotations.SerializedName;

public class BluetoothData {
    @SerializedName(value ="mw")
    int maximumWeight;
    @SerializedName(value ="fp")
    double fullPercentage;
    @SerializedName(value ="cp")
    double criticalPercentage;

    public BluetoothData(int maximumWeight, double fullPercentage, double criticalPercentage) {
        this.maximumWeight = maximumWeight;
        this.fullPercentage = fullPercentage;
        this.criticalPercentage = criticalPercentage;
    }
}
