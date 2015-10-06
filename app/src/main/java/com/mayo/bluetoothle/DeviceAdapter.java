package com.mayo.bluetoothle;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by mayo on 6/10/15.
 */
public class DeviceAdapter extends ArrayAdapter<BluetoothDevice> {
    private ArrayList<BluetoothDevice> mDevices;

    public DeviceAdapter(Context context) {
        super(context, -1);
        mDevices = new ArrayList<BluetoothDevice>();
    }

    public void setDevices(ArrayMap<String,BluetoothDevice> bluemap){
        for(String key : bluemap.keySet()){
            mDevices.add(bluemap.get(key));
        }
    }

    @Override
    public int getCount() {
        return mDevices.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.r_device_details,parent,false);
        ((TextView)v.findViewById(R.id.device_name)).setText(mDevices.get(position).getName());
        ((TextView)v.findViewById(R.id.device_address)).setText(mDevices.get(position).getAddress());
        return v;
    }
}
