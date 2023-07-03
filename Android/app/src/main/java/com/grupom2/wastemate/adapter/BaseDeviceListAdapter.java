package com.grupom2.wastemate.adapter;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import com.grupom2.wastemate.constant.Constants;
import com.grupom2.wastemate.R;


public class BaseDeviceListAdapter<T extends BaseDeviceListAdapter.BaseDeviceViewHolder> extends RecyclerView.Adapter<T>
{
    protected List<BluetoothDevice> dataList;
    private OnClickListener onItemClickedListener;

    public void setData(ArrayList<BluetoothDevice> availableDevices)
    {
        dataList = availableDevices;
    }

    @NonNull
    @Override
    public T onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_device, parent, false);
        return (T) new BaseDeviceViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseDeviceViewHolder holder, int position)
    {
        doOnBindViewHolder(holder, position);
    }

    protected BluetoothDevice doOnBindViewHolder(@NonNull BaseDeviceViewHolder holder, int position)
    {
        BluetoothDevice device = dataList.get(position);
        String deviceName;
        String deviceAddress;
        try
        {
            deviceName = device.getName();
            deviceAddress = device.getAddress();
        }
        catch (SecurityException e)
        {
            deviceName = Constants.UNIDENTIFIED;
            deviceAddress = Constants.UNIDENTIFIED;
        }
        holder.lblDeviceName.setText(deviceName == null || deviceName.isEmpty() ? deviceAddress : deviceName);
        holder.lblDeviceAddress.setText(deviceAddress);
        holder.itemView.setOnClickListener(view ->
        {
            if (onItemClickedListener != null)
            {
                onItemClickedListener.onClick(position, device);
            }
        });
        return device;
    }

    @Override
    public int getItemCount()
    {
        return dataList.size();
    }

    public void setOnItemClickedListener(OnClickListener onItemClickedListener)
    {
        this.onItemClickedListener = onItemClickedListener;
    }

    public interface OnClickListener
    {
        void onClick(int position, BluetoothDevice model);
    }

    public static class BaseDeviceViewHolder extends RecyclerView.ViewHolder
    {
        TextView lblDeviceAddress;
        TextView lblDeviceName;

        public BaseDeviceViewHolder(View itemView)
        {
            super(itemView);
            lblDeviceName = itemView.findViewById(R.id.label_device_name);
            lblDeviceAddress = itemView.findViewById(R.id.label_device_address);
        }
    }
}