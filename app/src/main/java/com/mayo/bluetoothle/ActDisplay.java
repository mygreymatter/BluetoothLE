package com.mayo.bluetoothle;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mayo.bluetoothle.protocol.IRBLProtocol;
import com.mayo.bluetoothle.protocol.RBLProtocol;

import java.util.ArrayList;
import java.util.List;

public class ActDisplay extends AppCompatActivity implements View.OnClickListener, IRBLProtocol {
    private static final String TAG = "RedBear";

    private BluetoothDevice mDevice;
    private TextView mDeviceName;
    private TextView mControlInput;
    private Button mConnect;
    private Button mControlOutput;

    private BluetoothGatt mBluetoothGatt = null;
    private boolean isConnected;
    private boolean isOn;

    private RBLProtocol mProtocol;
    private BluetoothGattCharacteristic txCharc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_display);

        mDevice = Blue.getInstance().blueDevices.get(getIntent().getStringExtra("key"));
        mDeviceName = (TextView) findViewById(R.id.device_name);
        mConnect = (Button) findViewById(R.id.connect);
        mControlOutput = (Button) findViewById(R.id.control_output);
        mControlInput = (TextView) findViewById(R.id.control_input);

        mConnect.setText("Connect");
        mControlOutput.setText("On");

        mConnect.setOnClickListener(this);
        mControlOutput.setOnClickListener(this);
//        mControlOutput.setEnabled(false);

        mDeviceName.setText(mDevice.getName());
    }

    private void toggleConnection() {
        if (!isConnected)
            mBluetoothGatt = mDevice.connectGatt(this, true, mGattCallback);
        else if (mBluetoothGatt != null)
            mBluetoothGatt.disconnect();
    }

    private void setConnectionState() {
        if (!isConnected) {
            isConnected = true;
            setTextOnUiThread(mConnect, "Disconnect", false);
//            mControlOutput.setEnabled(true);
        } else {
            isConnected = false;
            setTextOnUiThread(mConnect, "Connect", false);
//            mControlOutput.setEnabled(false);
            setTextOnUiThread(mControlOutput,"On",true);
            isOn = false;
        }
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "onCharacteristicRead");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "onCharacteristicWrite");
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.i(TAG, "onDescriptorWrite");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onDescriptorWrite Success");
                mProtocol.queryProtocolVersion();
            }


        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.i(TAG, "onReliableWriteCompleted");
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.i(TAG, "onReadRemoteRssi");
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.i(TAG, "onMtuChanged");
            super.onMtuChanged(gatt, mtu, status);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, "onConnectionStateChange");

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                setConnectionState();
                mBluetoothGatt.discoverServices();
                mBluetoothGatt.readRemoteRssi();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED)
                setConnectionState();

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, "onServicesDiscovered");
            BluetoothGattService rblService = mBluetoothGatt
                    .getService(RedBear.RBL_SERVICE);

            if (rblService == null) {
                Log.e(TAG, "RBL service not found!");
                return;
            }

            List<BluetoothGattCharacteristic> Characteristic = rblService
                    .getCharacteristics();

            for (BluetoothGattCharacteristic a : Characteristic) {
                Log.e(TAG, " a =  uuid : " + a.getUuid() + "");
            }

            BluetoothGattCharacteristic rxCharc = rblService
                    .getCharacteristic(RedBear.RBL_DEVICE_RX_UUID);
            if (rxCharc == null) {
                Log.i(TAG, "RBL RX Characteristic not found!");
                return;
            }

            txCharc = rblService.getCharacteristic(RedBear.RBL_DEVICE_TX_UUID);
            if (txCharc == null) {
                Log.i(TAG, "RBL RX Characteristic not found!");
                return;
            }

            enableNotification(true, rxCharc);
            setRBLProtocol();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.i(TAG, "onCharacteristicChanged: " + characteristic);

            int i = 0;
            Integer temp = characteristic.getIntValue(
                    BluetoothGattCharacteristic.FORMAT_UINT8, i++);
            ArrayList<Integer> values = new ArrayList<Integer>();
            while (temp != null) {
                Log.i(TAG, "temp: " + temp);
                values.add(temp);
                temp = characteristic.getIntValue(
                        BluetoothGattCharacteristic.FORMAT_UINT8, i++);
            }

            int[] received = new int[i];
            i = 0;
            for (Integer integer : values) {
                received[i++] = integer.intValue();
            }

            mProtocol.parseData(received);
        }

    };


    private boolean enableNotification(boolean enable,
                                       BluetoothGattCharacteristic characteristic) {
        if (mBluetoothGatt == null) {
            return false;
        }
        if (!mBluetoothGatt.setCharacteristicNotification(characteristic,
                enable)) {
            return false;
        }

        BluetoothGattDescriptor clientConfig = characteristic
                .getDescriptor(RedBear.CCC);
        if (clientConfig == null) {
            return false;
        }

        if (enable) {
            Log.i(TAG, "enable notification");
            clientConfig
                    .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            Log.i(TAG, "disable notification");
            clientConfig
                    .setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }

        return mBluetoothGatt.writeDescriptor(clientConfig);
    }

    private void setRBLProtocol() {
        mProtocol = new RBLProtocol(mDevice.getAddress());
        mProtocol.setIRBLProtocol(ActDisplay.this);
    }

    @Override
    protected void onDestroy() {
        if (isConnected)
            toggleConnection();

        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect:
                toggleConnection();
                break;
            case R.id.control_output:
                if (mBluetoothGatt != null && isConnected)
                    writeBLE();
                break;
        }
    }

    private void readBLE() {
        if (mProtocol == null) return;

    }

    private void writeBLE() {
        if (mProtocol == null) return;
        String text = "On";
        int value = 0;

        if (!isOn) {
            text = "Off";
            value = 1;
        }

        mControlOutput.setText(text);
        mProtocol.digitalWrite(3, value);
    }

    @Override
    public void protocolDidReceiveCustomData(int[] data, int length) {
        Log.i(TAG, "CustomData: " + getString(data));
    }

    private static String getString(int[] data) {
        int len = data.length;

        StringBuilder str = new StringBuilder(len - 2);

        for (int index = 0; index < len - 2; index++)
            str.append(Character.toString((char) data[index]));

        return str.toString();
    }

    @Override
    public void protocolDidReceiveProtocolVersion(int major, int minor, int bugfix) {
        Log.i(TAG, "ProtocolVersion : " + major + "." + minor + "." + bugfix);
        if (mProtocol != null) {
            int[] data = {'B', 'L', 'E'};
            mProtocol.queryTotalPinCount();
        }
    }

    @Override
    public void protocolDidReceiveTotalPinCount(int count) {
        Log.i(TAG, "TotalPinCount: " + count);
        if (mProtocol != null)
            mProtocol.queryPinAll();
    }

    @Override
    public void protocolDidReceivePinCapability(int pin, int value) {
        Log.i(TAG, "PinCapability - pin: " + pin + " ,Value: " + value);
    }

    @Override
    public void protocolDidReceivePinMode(int pin, int mode) {
        Log.i(TAG, "PinMode - Pin: " + pin + " ,mode: " + mode);
    }

    @Override
    public void protocolDidReceivePinData(int pin, int mode, final int value) {
        Log.i(TAG, "PinData - Pin: " + pin + " ,mode: " + mode + " ,value: " + value);
        if (pin == 3) {
            if (value == 0) {
                isOn = false;
                setTextOnUiThread(mControlOutput, "On", true);
            } else {
                isOn = true;
                setTextOnUiThread(mControlOutput, "Off", true);
            }
        }else if(pin == 2){
            if (value == 0) {
                setTextOnUiThread(mControlInput, "Low", false);
            } else {
                setTextOnUiThread(mControlInput, "High", false);
            }
        }
    }

    private void setTextOnUiThread(final View v, final String text, final boolean isButtonView) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isButtonView)
                    ((Button) v).setText(text);
                else
                    ((TextView) v).setText(text);
            }
        });
    }

    @Override
    public void writeValue(String deviceAddress, char[] data) {
        if (txCharc != null) {
            String value = new String(data);

            if (txCharc.setValue(value)) {
                if (!mBluetoothGatt.writeCharacteristic(txCharc)) {
                    Log.i(TAG, "Error: writeCharacteristic value: " + value);
                }
            } else {
                Log.i(TAG, "Error: setValue!");
            }
        }
    }
}
