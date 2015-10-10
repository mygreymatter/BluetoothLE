package com.mayo.bluetoothle;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.mayo.bluetoothle.protocol.RBLProtocol;
import com.mayo.bluetoothle.ServiceBlue.IServiceBlue;

public class ActDisplay extends AppCompatActivity implements OnClickListener, IServiceBlue {
    private static final String TAG = "BluetoothLE";
    private String key;

    private BluetoothDevice mDevice;
    private TextView mDeviceName;
    private TextView mControlInput;
    private Button mConnect;
    private Button mControlOutput;

    private BluetoothGatt mBluetoothGatt = null;
    private boolean isOn;

    private RBLProtocol mProtocol;
    private BluetoothGattCharacteristic txCharc;

    private boolean isBound;
    private ServiceBlue mService;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_display);

        key = getIntent().getStringExtra("key");

        mDeviceName = (TextView) findViewById(R.id.device_name);
        mConnect = (Button) findViewById(R.id.connect);
        mControlOutput = (Button) findViewById(R.id.control_output);
        mControlInput = (TextView) findViewById(R.id.control_input);

        mConnect.setText("Connect");
        mControlOutput.setText("On");

        mConnect.setOnClickListener(this);
        mControlOutput.setOnClickListener(this);
//        mControlOutput.setEnabled(false);


        intent = new Intent(this, ServiceBlue.class);
        bindService(intent, mConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder iBinder) {
            ServiceBlue.MeBinder binder = (ServiceBlue.MeBinder) iBinder;

            isBound = true;

            mService = binder.getService();
            mService.setContext(ActDisplay.this);

            mDevice = mService.getDevice(key);
            mDeviceName.setText(mDevice.getName());
            Log.i(TAG, mDevice.getName());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            Log.i(TAG, "Disconnected");
            isBound = false;
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect:
                if (!mService.isConnected)
                    mService.connectDevice(key);
                else
                    mService.disConnectDevice();

                break;
            case R.id.control_output:
                if (mBluetoothGatt != null && mService.isConnected)
                    mService.writeBLE(0);
                break;
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
    public void displayDevices() {

    }

    @Override
    public void setConnectionState(boolean connectionState) {
        if (connectionState) {
            setTextOnUiThread(mConnect, "Disconnect", false);
//            mControlOutput.setEnabled(true);
        } else {
            setTextOnUiThread(mConnect, "Connect", false);
//            mControlOutput.setEnabled(false);
            setTextOnUiThread(mControlOutput, "On", true);
            isOn = false;
        }
    }
}
