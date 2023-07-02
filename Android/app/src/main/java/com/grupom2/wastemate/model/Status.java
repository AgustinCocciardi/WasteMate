package com.grupom2.wastemate.model;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.grupom2.wastemate.R;

public enum Status
{
    NOT_CONNECTED("DESCONECTADO")
            {
                @Override
                public int getDisplayColor(Context context)
                {
                    return ContextCompat.getColor(context, android.R.color.darker_gray);
                }

                @Override
                public String getDisplayName(Context context)
                {
                    return context.getResources().getString(R.string.status_not_connected);
                }
            },
    UNFILLED("CAPACIDAD DISPONIBLE")
            {
                @Override
                public int getDisplayColor(Context context)
                {
                    return ContextCompat.getColor(context, android.R.color.holo_green_dark);
                }

                @Override
                public String getDisplayName(Context context)
                {
                    return context.getResources().getString(R.string.status_unfilled);
                }
            },
    CRITICAL_CAPACITY("CAPACIDAD CRITICA")
            {
                @Override
                public int getDisplayColor(Context context)
                {
                    return ContextCompat.getColor(context, android.R.color.holo_orange_dark);
                }

                @Override
                public String getDisplayName(Context context)
                {
                    return context.getResources().getString(R.string.status_critical_capacity);
                }
            },
    NO_CAPACITY("SIN CAPACIDAD")
            {
                @Override
                public int getDisplayColor(Context context)
                {
                    return ContextCompat.getColor(context, android.R.color.holo_red_dark);
                }

                @Override
                public String getDisplayName(Context context)
                {
                    return context.getResources().getString(R.string.status_no_capacity);
                }
            },
    IN_MAINTENANCE("EN MANTENIMIENTO")
            {
                @Override
                public int getDisplayColor(Context context)
                {
                    return ContextCompat.getColor(context, android.R.color.holo_blue_dark);
                }

                @Override
                public String getDisplayName(Context context)
                {
                    return context.getResources().getString(R.string.status_in_maintenance);
                }
            },
    ERROR("ERROR")
            {
                @Override
                public int getDisplayColor(Context context)
                {
                    return ContextCompat.getColor(context, android.R.color.holo_red_light);
                }

                @Override
                public String getDisplayName(Context context)
                {
                    return context.getResources().getString(R.string.status_error);
                }
            };

    private final String value;

    Status(String value)
    {
        this.value = value;
    }

    public abstract String getDisplayName(Context context);

    public abstract int getDisplayColor(Context context);

    public String getValue()
    {
        return value;
    }
}
