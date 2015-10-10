package com.mayo.bluetoothle;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.ArrayMap;
import android.util.Log;

import com.mayo.bluetoothle.protocol.IRBLProtocol;
import com.mayo.bluetoothle.protocol.RBLProtocol;

import java.util.ArrayList;
import java.util.List;

public class BlueService extends Service implements IRBLProtocol{
    private static final String TAG = "BlueService";

    public ArrayMap<String,BluetoothDevice> blueDevices;

    private IBluetooth mIBluetooth;
    private RBLProtocol mProtocol;

    private BluetoothGatt mBluetoothGatt = null;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBLEScanner;
    private BluetoothGattCharacteristic txCharc;

    private boolean isConnected;
    private boolean isOn;
    private boolean isBluetoothOn;

    private final IBinder mBinder = new MyBinder();

    public BlueService( ) {
        Log.i(TAG, "BlueService - Constructor");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "BlueService - onCreate");
        PackageManager pm = getPackageManager();
        blueDevices = new ArrayMap<>();
        boolean hasBLE = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);

        if (hasBLE)
            Log.i(TAG, "Yes");
        else
            Log.i(TAG, "No");

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
            mBluetoothAdapter = bluetoothManager.getAdapter();
        else
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "BlueService - onStartCommand");
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "BlueService - onBind");
        return mBinder;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "BlueService - onDestroy");
    }

    @Override
    public void protocolDidReceiveCustomData(int[] data, int length) {

    }

    @Override
    public void protocolDidReceiveProtocolVersion(int major, int minor, int bugfix) {

    }

    @Override
    public void protocolDidReceiveTotalPinCount(int count) {

    }

    @Override
    public void protocolDidReceivePinCapability(int pin, int value) {

    }

    @Override
    public void protocolDidReceivePinMode(int pin, int mode) {

    }

    @Override
    public void protocolDidReceivePinData(int pin, int mode, int value) {

    }

    @Override
    public void writeValue(String deviceAddress, char[] data) {

    }

    public class MyBinder extends Binder {
        BlueService getService() {
            return BlueService.this;
        }

        /*public List<String> getWordList() {
            return list;
        }*/
    }

    public String getName() {
        return "mayo";
    }

    public void startBluetooth() {
        /*
         * We need to enforce that Bluetooth is first enabled, and take the
         * user to settings to enable it if they have not done so.
         */

        if (!mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled

        }
    }

    public void stopBluetooth() {
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
//            mStartScan.setEnabled(false);
//            mBluetooth.setText("Start Bluetooth");
            isBluetoothOn = false;
        }
    }

    public void setProtocol(BluetoothDevice device){
        mProtocol = new RBLProtocol(device.getAddress());
        mProtocol.setIRBLProtocol(this);

    }

    public void startScan() {
        if (blueDevices != null)
            blueDevices.clear();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();

            List<ScanFilter> filters = new ArrayList<>();
            mBLEScanner.startScan(filters,settings,mScanCallback);
//            mBLEScanner.startScan(mScanCallback);
        } else
            mBluetoothAdapter.startLeScan(mLeScanCallback);

    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             byte[] scanRecord) {
            Log.d(TAG, "onScanResult (device : " + device.getName() + ")");
            blueDevices.put(device.getAddress(), device);
        }
    };

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG, "onScanResult (device : " + result.getDevice().getName() + ")");
            blueDevices.put(result.getDevice().getAddress(), result.getDevice());
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


}
