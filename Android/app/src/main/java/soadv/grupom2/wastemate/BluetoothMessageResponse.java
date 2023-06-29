package soadv.grupom2.wastemate;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class BluetoothMessageResponse
{
    @SerializedName("c")
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
}
