package soadv.grupom2.wastemate;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {
    private List<BluetoothDevice> dataList;
    private OnClickListener onClickListener2;
    private boolean show;
//    OnPairButtonClickListener mListener;

    public DeviceListAdapter() {
        //this.dataList = dataList;
    }

    public DeviceListAdapter(boolean b) {
        show = b;
    }

    public void setData(ArrayList<BluetoothDevice> availableDevices) {
        dataList = availableDevices;
    }

    public void setConnected(BluetoothDevice device) {

    }

//    public void setListener(OnPairButtonClickListener listenerBotonEmparejar) {
//        mListener = listenerBotonEmparejar;
//    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        Button btn;

        View vw;
        public ViewHolder(View itemView, boolean show) {
            super(itemView);
            textView = itemView.findViewById(R.id.tv_name);
            btn = itemView.findViewById(R.id.button_desc);
            vw = itemView.findViewById(R.id.view_connected_indicator);

            if(!show)
                btn.setVisibility(View.GONE);

        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_device, parent, false);
        return new ViewHolder(itemView, show);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        BluetoothDevice item = dataList.get(position);
        String itemText = item.getName();
        holder.textView.setText(itemText);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onClickListener != null) {
                    onClickListener.onClick(position, item);
                }
            }
        });
        holder.btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(onClickListener2 != null){
                    onClickListener2.onClick(position, item);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }
    private OnClickListener onClickListener;

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }
    public void setOnClickListener2(OnClickListener onClickListener) {
        this.onClickListener2 = onClickListener;
    }

    public interface OnClickListener {
        void onClick(int position, BluetoothDevice model);
    }
}