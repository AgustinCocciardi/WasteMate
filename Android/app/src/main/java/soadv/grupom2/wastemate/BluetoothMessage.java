package soadv.grupom2.wastemate;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class BluetoothMessage {
    @SerializedName(value ="c")
    int code;
    @SerializedName(value ="d")
    BluetoothData data;

    public BluetoothMessage(int code, int maximumWeight, int minimumDistance, int criticalDistance) {
        this.code = code;
        this.data = new BluetoothData(maximumWeight,minimumDistance,criticalDistance);
    }

    public BluetoothMessage(int code) {
        this.code = code;
    }

    public String Serialize(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}


