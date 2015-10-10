package com.mayo.bluetoothle;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;


import com.mayo.bluetoothle.ServiceBlue.IServiceBlue;
import com.mayo.bluetoothle.ServiceBlue.MeBinder;

public class ActMain extends AppCompatActivity implements View.OnClickListener,IServiceBlue{
    private static final int REQUEST_CODE_BLUETOOTH_ON = 100;
    private static final int REQUEST_CODE_BLUETOOTH_OFF = 101;
    private static final String TAG = "BluetoothLE";
    private Intent intent;
    private ServiceBlue mService;
    private boolean isBound;

    private Button mBluetooth;
    private Button mStartScan;

    private ListView mBlueDevicesList;
    private DeviceAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_main);

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

        if (hasBluetoothLe()) {
            intent = new Intent(this, ServiceBlue.class);
            bindService(intent, mConnection, BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (hasBluetoothLe() && isBound)
            unbindService(mConnection);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder iBinder) {
            MeBinder binder = (ServiceBlue.MeBinder) iBinder;
            
            isBound = true;

            mService = binder.getService();
            mService.setContext(ActMain.this);
            mService.initBluetooth();

            if(mService.hasBluetoothLeEnabled())
                toggelBlueUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            Log.i(TAG, "Disconnected");
            isBound = false;
        }
    };

    private boolean hasBluetoothLe() {
        PackageManager pm = getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    private void enableBluetooth(){
        if (!mService.hasBluetoothLeEnabled()) {
            mService.initBluetooth();
            //Bluetooth is disabled
            Intent enableBtIntent =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_CODE_BLUETOOTH_ON);
        }
    }

    private void toggelBlueUI() {
        if(mService.hasBluetoothLeEnabled()){
            mStartScan.setEnabled(true);
            mBluetooth.setText("Stop Bluetooth");
        }else{
            mStartScan.setEnabled(false);
            mBluetooth.setText("Start Bluetooth");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_BLUETOOTH_ON:
                toggelBlueUI();
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bluetooth:
                if (!mService.hasBluetoothLeEnabled()) {
                    enableBluetooth();
                }else {
                    mService.disableBluetooth();
                    toggelBlueUI();
                    mStartScan.setEnabled(false);
                }

                break;
            case R.id.scan_bluetooth:
                mService.startScan();
                break;
        }
    }

    @Override
    public void displayDevices() {
        mAdapter.setDevices(mService.getDevices());
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void setConnectionState(boolean connectionState) {

    }
}