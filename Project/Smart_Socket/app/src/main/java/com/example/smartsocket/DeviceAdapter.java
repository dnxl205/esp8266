package com.example.smartsocket;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder>{

    private List<Device> mDviceList;

    static class ViewHolder extends RecyclerView.ViewHolder{
        View deviceView;
        TextView deviceName;
        TextView deviceStatus_text;
        Switch deviceStatus_switch;

        public ViewHolder(View view){
            super(view);
            deviceView = view;
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
        final ViewHolder holder = new ViewHolder(view);

        //注册View点击事件
        holder.deviceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = holder.getAdapterPosition();
                Device device = mDviceList.get(position);
                Toast.makeText(view.getContext(),"单击："+device.getName(),Toast.LENGTH_SHORT).show();
            }
        });

        return holder;
    }

    //定义一个接口并暴露给外部使用
    public interface OnItemClickListener{
        void onClick(String deviceName,boolean deviceStatus,int position) throws JSONException;
    }
    private OnItemClickListener listener;
    public void setOnItemClickListener(OnItemClickListener listener){
        this.listener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        final Device device = mDviceList.get(position);
        holder.deviceName.setText(device.getName());
        holder.deviceStatus_text.setText(device.getDeviceStatus_text());
        holder.deviceStatus_switch.setChecked(device.getDeviceStatus_switch());

        //设置switch监听器
        holder.deviceStatus_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(listener != null){
                    try {
                        listener.onClick(device.getName(),b,position);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDviceList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    public void addItem(int position, Device device){
        mDviceList.add(position,device);
        notifyItemInserted(position);
    }
}
