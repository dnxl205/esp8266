package com.example.smartsocket;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder>{

    private List<Device> mDviceList;
    private final Activity mActivity;


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
            deviceStatus_switch = view.findViewById(R.id.deviceStatus_switch);
        }
    }

    public DeviceAdapter(Activity mActivity, List<Device> dviceList){
        this.mActivity = mActivity;
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

                //跳转到插座控制页面
                Intent intent=new Intent(mActivity, DeviceActivity.class);
                intent.putExtra("imei",device.getImei());//设备唯一标识
                intent.putExtra("name",device.getName());//插座名称
                intent.putExtra("switch",device.getDeviceStatus_switch());//开关状态
                mActivity.startActivityForResult(intent,1);
            }
        });

        //2
        holder.deviceView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                int position = holder.getLayoutPosition();
                mOnItemLongClickListener.onItemLongClick(holder.deviceView,position);
                return true;
            }
        });

        return holder;
    }

    //定义一个单击接口并暴露给外部使用
    public interface OnItemClickListener{
        void onClick(CompoundButton view, String deviceName, boolean deviceStatus, int position) throws JSONException;
    }
    private OnItemClickListener listener;
    public void setOnItemClickListener(OnItemClickListener listener){
        this.listener = listener;
    }

    //1
    private DeviceAdapter.OnItemLongClickListener mOnItemLongClickListener;
    public interface OnItemLongClickListener{
        void onItemLongClick(View view,int position);
    }
    public void setOnItemLongClickListener(DeviceAdapter.OnItemLongClickListener mOnItemLongClickListener) {
        this.mOnItemLongClickListener = mOnItemLongClickListener;
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
            public void onCheckedChanged(CompoundButton view, boolean b) {
                if(listener != null){
                    try {
                        listener.onClick(view,device.getName(),b,position);
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
        notifyDataSetChanged();
    }

    //删除数据
    public void removeData(int position) {
        mDviceList.remove(position);
        //删除动画
        notifyItemRemoved(position);
        notifyDataSetChanged();
    }
}
