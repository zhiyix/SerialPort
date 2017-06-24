package com.game.serialport;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;

import android_serialport_api.SerialUtilOld;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

public class MainActivity extends AppCompatActivity
        implements ServiceConnection, View.OnClickListener {
    private static final String TAG = "MainActivity";
    private static final int READ_BUFFER_SIZE = 1024;
    private static final int READ_TIMEOUT_MSEC = 50;

    private TextView mReceiveTextView;
    private Button mReceiveButton;
    private EditText mSendEditText;
    private Button mSendButton;
    private Button mStopButton;

    private Switch mSerialSwitch;
    private String[] mDevices;
    private String[] mDevicePaths;
    private String[] mBaudrates;
    private Spinner mDevicesSpinner;
    private Spinner mBaudSpinner;
    private EditText mSendDataEditText;
    private TextView mSendTextView;
    private TextView mReceivedTextView;

    private Context mContext;
    private SerialService mSerialService;
    private SerialUtilOld mSerialUtilOld;
    private String path = "/dev/ttyO2";
    private int baudrate = 115200;
    private int flags = 0;
    private int size = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_view);

        try {
            //设置串口号、波特率，
            mSerialUtilOld = new SerialUtilOld("/dev/ttyO2", 115200, 0);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } finally {
            if (mSerialUtilOld.isSerialOpend()) {
                Log.d(TAG, "open serial successful!");
                mSerialUtilOld.close();
            }
        }
        initViews();
        doBindService();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
        Log.d(TAG, "onDestroy()");
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mSerialService = ((SerialService.SerialBinder) service).getService();
        mSerialService.registerCallback(mCallback);
        mSerialSwitch.setSelected(mSerialService.isSerialOpend());
        mDevices = mSerialService.getSerialPorts();
        mDevicePaths = mSerialService.getSerialPaths();
        checkArgument(mDevicePaths.length == mDevicePaths.length);
        mDevicesSpinner.setAdapter(new CustomApdater(mContext,
                android.R.layout.simple_dropdown_item_1line, mDevices));
        Log.d(TAG, "onServiceConnected()");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mSerialService.unregisterCallback(mCallback);
        mSerialService = null;
        Log.e(TAG, "onServiceDisconnected()");
    }

    private void doBindService() {
        Intent intent = new Intent(this, SerialService.class);
        mContext.startService(intent);
        mContext.bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    private void doUnbindService() {
        if(mSerialService!=null && mSerialService.isSerialOpend()){
            mSerialService.closeSerial();
        }
        mContext.unbindService(this);
    }

    private void initViews() {
        mSerialSwitch = (Switch) findViewById(R.id.serial_switch);
        mDevicesSpinner = (Spinner) findViewById(R.id.devices);
        mBaudSpinner = (Spinner) findViewById(R.id.baudrates);
        mBaudrates = getResources().getStringArray(R.array.baudrates);
        mBaudSpinner.setAdapter(new CustomApdater(mContext,
                android.R.layout.simple_dropdown_item_1line, mBaudrates));
        mSerialSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isNullOrEmpty((String) mDevicesSpinner.getSelectedItem());
                    Log.d(TAG, mDevicesSpinner.getSelectedItemPosition() + "," +
                            mDevicesSpinner.getSelectedItemId());
                    String device = (String) mDevicePaths[mDevicesSpinner.getSelectedItemPosition()];
                    String baudrate = (String) mBaudSpinner.getSelectedItem();
                    if (isNullOrEmpty(device)) {
                        Toast.makeText(mContext, "Device is null! Please selected device first!",
                                Toast.LENGTH_SHORT).show();
                    } else if (isNullOrEmpty(baudrate)) {
                        Toast.makeText(mContext, "Baudrate is null! Please selected baudrate first!",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        mSerialService.openSerial(device, baudrate);
                    }
                } else {
                    mSerialService.closeSerial();
                }
            }
        });
        mSendButton = (Button) findViewById(R.id.send_bt);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSerialService != null && mSerialService.isSerialOpend()) {
                    CharSequence data = mSendDataEditText.getText();
                    if ((null == data) || !(data.length() > 0)) {
                        return;
                    }
                    mSerialService.sendMessage(data.toString());
                } else {
                    Toast.makeText(mContext, "Please open serial!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        mSendDataEditText = (EditText) findViewById(R.id.send_ed);
        mSendDataEditText.setText("010300000037041C");
        mSendTextView = (TextView) findViewById(R.id.send_data);
        mReceivedTextView = (TextView) findViewById(R.id.received_data);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.main_recive_b: {
                /*
//              if(size!=-1){
                mReadThread = new ReadThread();
                mReadThread.start();
//              }
                */
            }
            break;
            case R.id.main_send_b: {
                String context = mSendEditText.getText().toString().trim();
                Log.d(TAG, "onClick: " + context);
                try {
                    byte[] data = SerialUtilOld
                            .hexStringToBytes(mSendEditText.getText().toString().trim());
                    mSerialUtilOld.setData(data);
                } catch (NullPointerException e) {
                    Toast.makeText(MainActivity.this, "串口设置有误，无法发送", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
            break;
            case R.id.main_stop_b: {
                //停止接收
                //mReadThread.interrupt();
                mReceiveTextView.setText("");
            }
            break;
        }
    }

    public void onSendClear(View view) {
        mSendTextView.setText("");
    }

    public void onReceiveClear(View view) {
        mReceivedTextView.setText("");
    }




    private SerialService.Callback mCallback = new SerialService.Callback() {

        @Override
        public void onDataSend(String data) {
            CharSequence oldData = mSendTextView.getText();
            StringBuffer sb = new StringBuffer();
            if(oldData != null && oldData.length() > 0){
                sb.append(oldData);
                sb.append("\n");
            }
            sb.append(data);
            mSendTextView.setText(sb.toString());
            Log.d(TAG, "SerialService.Callback.onDataSend()");
        }

        @Override
        public void onDataReceived(String data) {
            CharSequence oldData = mReceivedTextView.getText();
            StringBuffer sb = new StringBuffer();
            if(oldData != null && oldData.length() > 0){
                sb.append(oldData);
                sb.append("\n");
            }
            sb.append(data);
            mReceivedTextView.setText(sb.toString());
            Log.d(TAG, "SerialService.Callback.onDataReceived()");
        }
    };

    private class CustomApdater extends ArrayAdapter<String> implements SpinnerAdapter {

        public CustomApdater(Context context, int resource, String[] datas) {
            super(context, resource, datas);
        }
    }
}
