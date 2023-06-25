package soadv.grupom2.wastemate;

import com.google.gson.annotations.SerializedName;

public class BluetoothData {
    @SerializedName(value ="mw")
    int maximumWeight;
    @SerializedName(value ="md")
    int minimumDistance;
    @SerializedName(value ="cd")
    int criticalDistance;

    public BluetoothData(int maximumWeight, int minimumDistance, int criticalDistance) {
        this.maximumWeight = maximumWeight;
        this.minimumDistance = minimumDistance;
        this.criticalDistance = criticalDistance;
    }
}
