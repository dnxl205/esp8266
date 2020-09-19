package com.example.smartsocket;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder>{

    private List<Device> mDviceList;

    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView deviceName;
        TextView deviceStatus_text;
        Switch deviceStatus_switch;

        public ViewHolder(View view){
            super(view);
            deviceName = (TextView) view.findViewById(R.id.deviceName);
            deviceStatus_text = (TextView) view.findViewById(R.id.deviceStatus_text);
            deviceStatus_switch = (Switch) view.findViewById(R.id.deviceStatus_switch);
        }
    }

    public DeviceAdapter(List<Device> dviceList){
        mDviceList = dviceList;
    }

    @Override
    public ViewHolder onCreateViewHolder( ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.device,parent,false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Device device = mDviceList.get(position);
        holder.deviceName.setText(device.getName());
        holder.deviceStatus_text.setText(device.getDeviceStatus_text());
        holder.deviceStatus_switch.setChecked(device.getDeviceStatus_switch());
    }

    @Override
    public int getItemCount() {
        return mDviceList.size();
    }
}
