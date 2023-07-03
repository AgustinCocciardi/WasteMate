package com.grupom2.wastemate.util;

import com.grupom2.wastemate.constant.Constants;

import java.util.HashMap;
import java.util.Map;

public class CalibrationHelpers {
    private static final String pirString  = "PIR";
    private static final String flexString  = "Flex";
    private static final String ultrasonidoString  = "Ultrasonido";


    public static Map<String,Integer> sensorsDictionary = new HashMap<String,Integer>()
    {
        {
        put(pirString, Constants.CODE_CALIBRATE_PIR);
        put(flexString, Constants.CODE_CALIBRATE_WEIGHT);
        put(ultrasonidoString, Constants.CODE_CALIBRATE_MAXIMUM_CAPACITY);
        }
    };


}
