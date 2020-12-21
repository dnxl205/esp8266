package com.example.smartsocket;

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

public class DelayAdapter extends RecyclerView.Adapter<DelayAdapter.ViewHolder>{
    private List<Delay> mDelayList;

    public DelayAdapter(List<Delay>delayList){
        mDelayList = delayList;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        View view_delay;
        TextView textView_delayStatus;
        TextView textView_delayDate;
        TextView textView_delayTime;
        Switch switch_isEffective;

        public ViewHolder(@NonNull View view) {
            super(view);
            view_delay = view;
            textView_delayStatus = (TextView)view.findViewById(R.id.delay_status);
            textView_delayDate = (TextView)view.findViewById(R.id.delay_date);
            textView_delayTime = (TextView) view.findViewById(R.id.delay_time);
            switch_isEffective = view.findViewById(R.id.delayStatus_switch);
        }
    }

    //定义一个点击接口并暴露给外部使用
    public interface OnItemClickListener{
        void onClick(CompoundButton view, int position,boolean b) throws JSONException;
    }
    private OnItemClickListener listener;
    public void setOnItemClickListener(OnItemClickListener listener){
        this.listener = listener;
    }

    //1
    private OnItemLongClickListener mOnItemLongClickListener;
    public interface OnItemLongClickListener{
        void onItemLongClick(View view,int position);
    }
    public void setOnItemLongClickListener(OnItemLongClickListener mOnItemLongClickListener) {
        this.mOnItemLongClickListener = mOnItemLongClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.delay,parent,false);
        final ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final Delay delay = mDelayList.get(position);
        holder.textView_delayStatus.setText(delay.isDelay_status()?"开启设备":"关闭设备");
        holder.textView_delayDate.setText(delay.getDelayDate());
        holder.textView_delayTime.setText(delay.getDelayTime());
        holder.switch_isEffective.setChecked(delay.isEffective());

        //设置switch监听器
        holder.switch_isEffective.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton view, boolean b) {
                if(listener != null){
                    try {
                        listener.onClick(view,position,b);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        //2
        holder.view_delay.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                int position = holder.getLayoutPosition();
                mOnItemLongClickListener.onItemLongClick(holder.view_delay,position);
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDelayList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    //添加数据
    public void addItem(int position, Delay delay){
        mDelayList.add(position,delay);
        notifyItemInserted(position);
        notifyDataSetChanged();
    }

    //删除数据
    public void removeData(int position) {
        mDelayList.remove(position);
        //删除动画
        notifyItemRemoved(position);
        notifyDataSetChanged();
    }
}
