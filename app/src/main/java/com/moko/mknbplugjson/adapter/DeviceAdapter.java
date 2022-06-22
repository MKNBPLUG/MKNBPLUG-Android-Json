package com.moko.mknbplugjson.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.entity.MokoDevice;

import androidx.core.content.ContextCompat;


public class DeviceAdapter extends BaseQuickAdapter<MokoDevice, BaseViewHolder> {

    public DeviceAdapter() {
        super(R.layout.device_item);
    }

    @Override
    protected void convert(BaseViewHolder holder, MokoDevice device) {
        if (!device.isOnline) {
            holder.setImageResource(R.id.iv_switch, R.drawable.checkbox_close);
            holder.setText(R.id.tv_device_switch, R.string.device_state_offline);
            holder.setTextColor(R.id.tv_device_switch, ContextCompat.getColor(mContext, R.color.grey_cccccc));
        } else {
            if (!device.isOverload && !device.isOverCurrent && !device.isOverVoltage && !device.isUnderVoltage) {
                holder.setImageResource(R.id.iv_switch, device.on_off ? R.drawable.checkbox_open : R.drawable.checkbox_close);
                holder.setText(R.id.tv_device_switch, device.on_off ? R.string.switch_on : R.string.switch_off);
                holder.setTextColor(R.id.tv_device_switch, ContextCompat.getColor(mContext, device.on_off ? R.color.blue_0188cc : R.color.grey_cccccc));
            } else {
                holder.setImageResource(R.id.iv_switch, R.drawable.checkbox_close);
                String overStatus = "";
                if (device.isOverload) {
                    overStatus = "Overload";
                }
                if (device.isOverCurrent) {
                    overStatus = "Overcurrent";
                }
                if (device.isOverVoltage) {
                    overStatus = "Overvoltage";
                }
                if (device.isUnderVoltage) {
                    overStatus = "Undervoltage";
                }
                holder.setText(R.id.tv_device_switch, overStatus);
                holder.setTextColor(R.id.tv_device_switch, ContextCompat.getColor(mContext, R.color.red_ff0000));
            }
        }
        holder.setText(R.id.tv_device_name, device.name);
        holder.addOnClickListener(R.id.iv_switch);
    }
}
