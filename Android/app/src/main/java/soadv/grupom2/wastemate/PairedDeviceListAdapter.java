package soadv.grupom2.wastemate;

import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

public class PairedDeviceListAdapter extends BaseDeviceListAdapter<PairedDeviceListAdapter.PairedDeviceViewHolder>
{
    private OnClickListener onUnpairDeviceClickListener;

    @Override
    public void onBindViewHolder(@NonNull PairedDeviceViewHolder holder, int position)
    {
        BluetoothDevice item = doOnBindViewHolder(holder, position);
        if (BluetoothService.getInstance().isDeviceConnected(item))
        {
            holder.setConnectedIndicatorColor(Color.rgb(98, 0, 238));
        }
        holder.btnUnpair.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (onUnpairDeviceClickListener != null)
                {
                    onUnpairDeviceClickListener.onClick(position, item);
                }
            }
        });
    }

    @NonNull
    @Override
    public PairedDeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_paired_device, parent, false);
        return new PairedDeviceViewHolder(itemView);
    }

    public void setOnUnpairDeviceClickListener(OnClickListener onClickListener)
    {
        this.onUnpairDeviceClickListener = onClickListener;
    }

    public static class PairedDeviceViewHolder extends BaseDeviceViewHolder
    {

        FrameLayout btnUnpair;
        View vwConnectedIndicator;

        public PairedDeviceViewHolder(View itemView)
        {
            super(itemView);
            lblDeviceName = itemView.findViewById(R.id.label_device_name);
            btnUnpair = itemView.findViewById(R.id.frame_layout_button_unpair);
            vwConnectedIndicator = itemView.findViewById(R.id.view_connected_indicator);
        }

        public void setConnectedIndicatorColor(int color)
        {
            vwConnectedIndicator.setBackgroundColor(color);
        }
    }

}
