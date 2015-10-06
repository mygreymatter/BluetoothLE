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

import java.util.List;

public class ActDisplay extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "BluetoothLE";

    private BluetoothDevice mDevice;
    private TextView mDeviceName;
    private Button mConnect;

    private BluetoothGatt mBluetoothGatt = null;
    private boolean isConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_display);

        mDevice = Blue.getInstance().blueDevices.get(getIntent().getStringExtra("key"));
        mDeviceName = (TextView) findViewById(R.id.device_name);
        mConnect = (Button) findViewById(R.id.connect);
        mConnect.setText("Connect");

        mConnect.setOnClickListener(this);

        mDeviceName.setText(mDevice.getName());
    }

    private void connect(){
        Log.i(TAG, "-----------------------Connecting-----------------------------");
        mBluetoothGatt = mDevice.connectGatt(this,true,mGattCallback);
    }

    private void setConnectedState(){
        isConnected = true;
        mConnect.setText("Disconnect");
    }

    private void disconnect(){
        Log.i(TAG, "-----------------------Disconnecting-----------------------------");
        if(mBluetoothGatt != null){
            mBluetoothGatt.disconnect();
//            mBluetoothGatt.close();
        }
    }

    private void setDisconnectedState(){
        isConnected = false;
        mConnect.setText("Connect");
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, "onConnectionStateChange");

            /*if(status == BluetoothGatt.GATT_SUCCESS){

            }else
                return;
*/
            if(newState == BluetoothProfile.STATE_CONNECTED){
                ActDisplay.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setConnectedState();
                    }
                });
                mBluetoothGatt.discoverServices();
                mBluetoothGatt.readRemoteRssi();
            }else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                ActDisplay.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setDisconnectedState();
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, "onServicesDiscovered");
            BluetoothGattService rblService = mBluetoothGatt
                    .getService(ReadBear.RBL_SERVICE);

            if (rblService == null) {
                Log.e(TAG, "RBL service not found!");
                return;
            }

            List<BluetoothGattCharacteristic> Characteristic = rblService
                    .getCharacteristics();

            for (BluetoothGattCharacteristic a : Characteristic) {
                Log.e(TAG, " a =  uuid : " + a.getUuid() + "");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "onCharacteristicRead");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.i(TAG, "onCharacteristicChanged");
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
            Log.i(TAG, "onDescriptorWrite");
            super.onDescriptorWrite(gatt, descriptor, status);
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
    };

    @Override
    protected void onDestroy() {
        if(isConnected)
            disconnect();

        mBluetoothGatt.close();
        mBluetoothGatt = null;
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.connect:
                if(!isConnected)
                    connect();
                else
                    disconnect();
                break;
        }
    }
}
