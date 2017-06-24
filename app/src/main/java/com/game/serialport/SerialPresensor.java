package com.game.serialport;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import android_serialport_api.SerialUtilOld;

import static com.game.serialport.R.string.device;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Created by imetr on 2017/6/24.
 */

public class SerialPresensor implements ServiceConnection {
    private static final String TAG = SerialPresensor.class.getSimpleName();
    private static final int POLL_INTERVAL = 1000 * 60; /* 60 seconds */

    private String[] mDevices;
    private String[] mDevicePaths;
    private String[] mBaudrates;

    private SerialService mSerialService;
    private Context mContext;
    private SerialView mSerialView;

    public SerialPresensor(Context context, SerialView view) {
        mContext = context;
        mSerialView = view;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mSerialService = ((SerialService.SerialBinder) service).getService();
        mSerialService.registerCallback(mCallback);

        mDevices = mSerialService.getSerialPorts();
        mDevicePaths = mSerialService.getSerialPaths();
        checkArgument(mDevicePaths.length == mDevicePaths.length);
        mSerialView.updateDeviceList(mDevices);

        mBaudrates = mContext.getResources().getStringArray(R.array.baudrates);
        mSerialView.updateBaudrateList(mBaudrates);

        mSerialView.onSerialStatusChanged(mSerialService.isSerialOpend());
        Log.d(TAG, "onServiceConnected," + mSerialService + ", ID : " + mSerialService.getId());
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mSerialService.unregisterCallback(mCallback);
        mSerialService = null;
        Log.e(TAG, "onServiceDisconnected()");
    }

    public void doBindService() {
        Intent intent = SerialService.setServiceAlarm(mContext, POLL_INTERVAL, true);
                /*SerialService.newIntent(mContext);
        mContext.startService(intent);*/
        mContext.bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    public void doUnbindService() {
        if(mSerialService != null && mSerialService.isSerialOpend()){
            mSerialService.closeSerial();
        }
        mContext.unbindService(this);
    }

    public boolean doOpenSerial(int nDevice, String baudrate) {
        String device = (String) mDevicePaths[nDevice];
        boolean bOpened = true;
        if (isNullOrEmpty(device)) {
            Toast.makeText(mContext, "Device is null! Please selected device first!",
                    Toast.LENGTH_SHORT).show();
        } else if (isNullOrEmpty(baudrate)) {
            Toast.makeText(mContext, "Baudrate is null! Please selected baudrate first!",
                    Toast.LENGTH_SHORT).show();
        } else {
            mSerialService.openSerial(device, baudrate);
        }
        mSerialView.onSerialStatusChanged(mSerialService.isSerialOpend());
        return bOpened;
    }

    public void doCloseSerial() {
        mSerialService.closeSerial();
        mSerialView.onSerialStatusChanged(mSerialService.isSerialOpend());
    }

    public boolean doSentSerial(CharSequence data) {
        if (mSerialService != null && mSerialService.isSerialOpend()) {
            mSerialService.sendMessage(data.toString());
        } else {
            return false;
        }
        return true;
    }

    public void doToggleService() {
        boolean shouldStartAlarm = !SerialService.isServiceAlarmOn(mContext);
        SerialService.setServiceAlarm(mContext, POLL_INTERVAL, shouldStartAlarm);
        Log.i(TAG, "doToggleService, STATUS : " + shouldStartAlarm);
    }

    private SerialService.Callback mCallback = new SerialService.Callback() {

        @Override
        public void onDataSend(String data) {
            mSerialView.onSentTextViewChanged(data);
        }

        @Override
        public void onDataReceived(String data) {
            mSerialView.onReceivedTextViewChanged(data);
        }
    };
}
