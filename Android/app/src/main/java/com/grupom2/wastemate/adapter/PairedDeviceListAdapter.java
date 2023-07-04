package com.grupom2.wastemate.adapter;

import static com.grupom2.wastemate.adapter.PairedDeviceListAdapter.PairedDeviceViewHolder;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.grupom2.wastemate.R;
import com.grupom2.wastemate.bluetooth.BluetoothService;

public class PairedDeviceListAdapter extends BaseDeviceListAdapter<PairedDeviceViewHolder>
{
    private OnClickListener onUnpairDeviceClickListener;
    private final Context context;

    public PairedDeviceListAdapter(Context context)
    {
        this.context = context;
    }

    @Override
    public void onBindViewHolder(@NonNull PairedDeviceViewHolder holder, int position)
    {
        BluetoothDevice item = doOnBindViewHolder(holder, position);
        if (BluetoothService.getInstance().getBluetoothConnection().isConnected(item))
        {
            holder.setConnectedIndicatorColor(context, true);
        }

        holder.btnUnpair.setOnClickListener(v ->
        {
            if (onUnpairDeviceClickListener != null)
            {
                onUnpairDeviceClickListener.onClick(position, item);
            }
        });
    }

    @NonNull
    @Override
    public PairedDeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_paired_device, parent, false);
        return new PairedDeviceViewHolder(itemView);
    }

    public void setOnUnpairDeviceClickListener(OnClickListener onClickListener)
    {
        this.onUnpairDeviceClickListener = onClickListener;
    }

    public static class PairedDeviceViewHolder extends BaseDeviceViewHolder
    {
        View btnUnpair;
        View vwConnectedIndicator;

        public PairedDeviceViewHolder(View itemView)
        {
            super(itemView);
            lblDeviceName = itemView.findViewById(R.id.label_device_name);
            btnUnpair = itemView.findViewById(R.id.frame_layout_button_unpair);
            vwConnectedIndicator = itemView.findViewById(R.id.view_connected_indicator);
        }

        public void setConnectedIndicatorColor(Context context, boolean connected)
        {
            vwConnectedIndicator.setBackgroundColor(context.getResources().getColor(connected ? R.color.purple_500 : R.color.grey, context.getTheme()));
            vwConnectedIndicator.invalidate();
        }
    }

}
