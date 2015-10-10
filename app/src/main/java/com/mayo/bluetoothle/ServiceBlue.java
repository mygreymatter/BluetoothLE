package com.mayo.bluetoothle;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.ArrayMap;
import android.util.Log;

import com.mayo.bluetoothle.protocol.IRBLProtocol;
import com.mayo.bluetoothle.protocol.RBLProtocol;

import java.util.ArrayList;
import java.util.List;

public class ServiceBlue extends Service implements IRBLProtocol{
    private static final String TAG = "BluetoothLE Service";
//    private Context mContext;
    private Blue mBlue;
    private BluetoothGatt mBluetoothGatt;
    private RBLProtocol mProtocol;
    private BluetoothDevice mDevice;
    public boolean isConnected;

    public interface IServiceBlue{
        void displayDevices();
        void setConnectionState(boolean connectionState);

    }

    private IServiceBlue mIServiceBlue;

    private final IBinder mBinder = new MeBinder();
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBLEScanner;
    private BluetoothGattCharacteristic txCharc;

    public ServiceBlue() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        mBlue = Blue.getInstance();
        return mBinder;
    }

    public class MeBinder extends Binder {
        ServiceBlue getService() {
            return ServiceBlue.this;
        }
    }

    public void setContext(Context context) {
//        mContext = context;
        mIServiceBlue = (IServiceBlue) context;
    }

    public void initBluetooth() {
        if (mBluetoothAdapter != null) return;

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
            mBluetoothAdapter = bluetoothManager.getAdapter();
        else
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mBlue.blueDevices = new ArrayMap<>();
    }

    public boolean hasBluetoothLeEnabled() {
        return mBluetoothAdapter != null ? mBluetoothAdapter.isEnabled() : false;
    }

    public void disableBluetooth() {
        mBluetoothAdapter.disable();
    }

    public void startScan() {
        if (mBlue.blueDevices != null)
            mBlue.blueDevices.clear();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();

            List<ScanFilter> filters = new ArrayList<>();
            mBLEScanner.startScan(filters, settings, mScanCallback);
        } else
            mBluetoothAdapter.startLeScan(mLeScanCallback);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScan();
                mIServiceBlue.displayDevices();
            }
        }, 1000);
    }

    public ArrayMap<String, BluetoothDevice> getDevices() {
        return mBlue.blueDevices;
    }

    public BluetoothDevice getDevice(String key){
        mDevice = mBlue.blueDevices.get(key);
        return mDevice;
    }

    public void connectDevice(String key) {
            mBluetoothGatt = getDevice(key).connectGatt(this, true, mGattCallback);
    }

    public void disConnectDevice(){
        if(mBluetoothGatt != null) mBluetoothGatt.disconnect();
    }

    public void close(){
        if(mBluetoothGatt != null) mBluetoothGatt.close();
    }

    private void stopScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            mBLEScanner.stopScan(mScanCallback);
         else
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             byte[] scanRecord) {
            Log.i(TAG, "onScanResult (device : " + device.getName() + ")");
            mBlue.blueDevices.put(device.getAddress(), device);
        }
    };

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
                isConnected = true;
                mIServiceBlue.setConnectionState(isConnected);
                mBluetoothGatt.discoverServices();
                mBluetoothGatt.readRemoteRssi();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnected = false;
                mIServiceBlue.setConnectionState(isConnected);
            }

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
        mProtocol.setIRBLProtocol(this);
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i(TAG, "onScanResult (device : " + result.getDevice().getName() + ")");
            mBlue.blueDevices.put(result.getDevice().getAddress(), result.getDevice());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    @Override
    public void protocolDidReceiveCustomData(int[] data, int length) {

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
    public void protocolDidReceivePinData(int pin, int mode, int value) {
        Log.i(TAG, "PinData - Pin: " + pin + " ,mode: " + mode + " ,value: " + value);
        if (pin == 3) {
            if (value == 0) {
                //
            } else {
                //
            }
        }else if(pin == 2){
            if (value == 0) {
                //
            } else {
                //
            }
        }
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

    public void writeBLE(int value) {
        if (mProtocol == null) return;
        String text = "On";
        /*if (!isOn) {
            text = "Off";
            value = 1;
        }*/

//        mControlOutput.setText(text);
        mProtocol.digitalWrite(3, value);
    }


}
