package com.mayo.bluetoothle;

import android.bluetooth.BluetoothDevice;
import android.util.ArrayMap;

/**
 * Created by mayo on 6/10/15.
 */
public class Blue {
    private Blue(){}

    private static Blue blue = null;

    public static Blue getInstance(){
        if(null == blue)
            blue = new Blue();
        return blue;
    }

    public ArrayMap<String,BluetoothDevice> blueDevices;
}
