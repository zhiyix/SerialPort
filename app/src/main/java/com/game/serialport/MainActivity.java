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

import static com.game.serialport.R.string.device;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

public class MainActivity extends AppCompatActivity
        implements SerialView, View.OnClickListener {
    private static final String TAG = SerialPresensor.class.getSimpleName();

    private TextView mReceiveTextView;
    private Button mReceiveButton;
    private EditText mSendEditText;
    private Button mStopButton;
    private Button mSendButton;

    private Switch mSerialSwitch;
    //private String[] mDevices;
    //private String[] mDevicePaths;
    //private String[] mBaudrates;
    private Spinner mDevicesSpinner;
    private Spinner mBaudSpinner;
    private EditText mSendDataEditText;
    private TextView mSendTextView;
    private TextView mReceivedTextView;

    private SerialPresensor mSerialPresensor;
    //private Context mContext;
    //private SerialService mSerialService;
    private String path = "/dev/ttyO2";
    private int baudrate = 16; //115200;
    //private int flags = 0;
    //private int size = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);

        mSerialPresensor = new SerialPresensor(this, this);
        initViews();
        mSerialPresensor.doBindService();
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
        mSerialPresensor.doUnbindService();
        Log.d(TAG, "onDestroy()");
    }

    private void initViews() {
        mSerialSwitch = (Switch) findViewById(R.id.serial_switch);
        mDevicesSpinner = (Spinner) findViewById(R.id.devices);
        mBaudSpinner = (Spinner) findViewById(R.id.baudrates);
        mSerialSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isNullOrEmpty((String) mDevicesSpinner.getSelectedItem());
                    mSerialPresensor.doOpenSerial(mDevicesSpinner.getSelectedItemPosition(),
                            (String) mBaudSpinner.getSelectedItem());
                    Log.d(TAG, mDevicesSpinner.getSelectedItemPosition() + "," +
                            mDevicesSpinner.getSelectedItemId());
                } else {
                    mSerialPresensor.doCloseSerial();
                }
            }
        });
        mSendButton = (Button) findViewById(R.id.send_bt);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence data = mSendDataEditText.getText();
                if ((null == data) || !(data.length() > 0)) {
                    return;
                }
                if (!mSerialPresensor.doSentSerial(data)) {
                    Toast.makeText(MainActivity.this, "Please open serial!", Toast.LENGTH_SHORT).show();
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
                /*try {
                    byte[] data = SerialUtilOld
                            .hexStringToBytes(mSendEditText.getText().toString().trim());
                    mSerialUtilOld.setData(data);
                } catch (NullPointerException e) {
                    Toast.makeText(MainActivity.this, "串口设置有误，无法发送", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }*/
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

    @Override
    public void onSentTextViewChanged(String data) {
        CharSequence oldData = mSendTextView.getText();
        StringBuffer sb = new StringBuffer();
        if(oldData != null && oldData.length() > 0){
            sb.append(oldData);
            sb.append("\n");
        }
        sb.append(data);
        mSendTextView.setText(sb.toString());
        Log.d(TAG, "SerialService.onDataSend()");
    }

    @Override
    public void onReceivedTextViewChanged(String data) {
        CharSequence oldData = mReceivedTextView.getText();
        StringBuffer sb = new StringBuffer();
        if(oldData != null && oldData.length() > 0){
            sb.append(oldData);
            sb.append("\n");
        }
        sb.append(data);
        mReceivedTextView.setText(sb.toString());
        Log.d(TAG, "SerialService.onDataReceived()");
    }

    @Override
    public void onSerialStatusChanged(boolean isOpend) {
        mSerialSwitch.setSelected(isOpend);
    }

    @Override
    public void updateDeviceList(String[] devices) {
        mDevicesSpinner.setAdapter(new CustomApdater(this,
                android.R.layout.simple_dropdown_item_1line, devices));
        mDevicesSpinner.setSelection(devices.length - 1);
    }

    @Override
    public void updateBaudrateList(String[] baudrates) {
        mBaudSpinner.setAdapter(new CustomApdater(MainActivity.this,
                android.R.layout.simple_dropdown_item_1line, baudrates));
        mBaudSpinner.setSelection(baudrate);
    }

    private class CustomApdater extends ArrayAdapter<String> implements SpinnerAdapter {

        public CustomApdater(Context context, int resource, String[] datas) {
            super(context, resource, datas);
        }
    }
}
