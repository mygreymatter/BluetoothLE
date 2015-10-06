package com.mayo.bluetoothle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class ActMain extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "BluetoothLE";
    private static final int REQUEST_CODE_BLUETOOTH_ON = 100;
    private static final int REQUEST_CODE_BLUETOOTH_OFF = 101;

    private Blue mBlue;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBLEScanner;

    private boolean isBluetoothOn;

    private Button mBluetooth;
    private Button mStartScan;

    private ListView mBlueDevicesList;
    private DeviceAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_main);

        mBlue = Blue.getInstance();
        mBlue.blueDevices = new ArrayMap<>();
        PackageManager pm = getPackageManager();
        boolean hasBLE = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);

        if (hasBLE)
            Log.i(TAG, "Yes");
        else
            Log.i(TAG, "No");

        mBluetooth = (Button) findViewById(R.id.bluetooth);
        mStartScan = (Button) findViewById(R.id.scan_bluetooth);
        mBlueDevicesList = (ListView) findViewById(R.id.bluedevices_list);

        mAdapter = new DeviceAdapter(this);
        mStartScan.setOnClickListener(this);
        mBluetooth.setOnClickListener(this);
        mBluetooth.setText("Start Bluetooth");

        mBlueDevicesList.setAdapter(mAdapter);

        mStartScan.setEnabled(false);

        mBlueDevicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                TextView t = (TextView) view.findViewById(R.id.device_address);
                Log.i(TAG, "Selected Device Address: " + t.getText());

                Intent intent = new Intent(ActMain.this, ActDisplay.class);
                intent.putExtra("key", t.getText().toString());

                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
            mBluetoothAdapter = bluetoothManager.getAdapter();
         else
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter.isEnabled()) {
            mBluetooth.setText("Stop Bluetooth");
            mStartScan.setEnabled(true);
            isBluetoothOn = true;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopBluetooth();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bluetooth:
                if (!isBluetoothOn)
                    startBluetooth();
                else
                    stopBluetooth();
                break;
            case R.id.scan_bluetooth:
                startScan();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            mBLEScanner.stopScan(mScanCallback);
                        } else*/
                            mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        displayDevicesList();
                    }
                }, 1000);
                break;
        }
    }

    private void displayDevicesList() {
        mAdapter.setDevices(mBlue.blueDevices);
        mAdapter.notifyDataSetChanged();
    }

    private void startBluetooth() {
        /*
         * We need to enforce that Bluetooth is first enabled, and take the
         * user to settings to enable it if they have not done so.
         */

        if (!mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled
            Intent enableBtIntent =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_CODE_BLUETOOTH_ON);
        }
    }

    private void stopBluetooth() {
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
            mStartScan.setEnabled(false);
            mBluetooth.setText("Start Bluetooth");
            isBluetoothOn = false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_BLUETOOTH_ON:
                mBluetooth.setText("Stop Bluetooth");
                mStartScan.setEnabled(false);
                isBluetoothOn = true;
                break;
        }
    }

    private void startScan() {
        if (mBlue.blueDevices != null)
            mBlue.blueDevices.clear();

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            *//*ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();

            List<ScanFilter> filters = new ArrayList<>();
            mBLEScanner.startScan(filters,settings,mScanCallback);*//*
            mBLEScanner.startScan(mScanCallback);
        } else*/
            mBluetoothAdapter.startLeScan(mLeScanCallback);

    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             byte[] scanRecord) {
            Log.d(TAG, "onScanResult (device : " + device.getName() + ")");
            mBlue.blueDevices.put(device.getAddress(), device);
        }
    };

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG, "onScanResult (device : " + result.getDevice().getName() + ")");
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
}