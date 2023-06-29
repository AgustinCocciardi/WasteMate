package com.grupom2.wastemate.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesManager
{
    private final SharedPreferences sharedPreferences;
    private static SharedPreferencesManager instance;

    private SharedPreferencesManager(Context context, String prefsName)
    {
        sharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
    }

    public static synchronized SharedPreferencesManager getInstance(Context context, String prefsName)
    {
        if (instance == null)
        {
            instance = new SharedPreferencesManager(context, prefsName);
        }
        return instance;
    }

    public void saveString(String key, String value)
    {
        synchronized (sharedPreferences)
        {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(key, value);
            editor.apply();
        }

    }

    public String loadString(String key, String defaultValue)
    {
        synchronized (sharedPreferences)
        {
            return sharedPreferences.getString(key, defaultValue);
        }
    }

    public void removeKey(String key)
    {
        synchronized (sharedPreferences)
        {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(key);
            editor.apply();
        }
    }
}
