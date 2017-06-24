package com.game.serialport;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

import android_serialport_api.SerialPort;
import android_serialport_api.SerialUtilOld;

import static com.google.common.base.Strings.*;

/**
 * Created by imetr on 2017/6/24.
 */

public class SerialService extends IntentService {
    private static final String TAG = SerialService.class.getSimpleName();
    private static final int READ_BUFFER_SIZE = 1024;

    private static final int EVENT_OPEN_SERIAL = 1;
    private static final int EVENT_CLOASE_SERAIL = 2;
    private static final int EVENT_SEND_MESSAGES = 3;

    public static final int EVENT_MESSAGE_RECEIVED = 1;
    public static final int EVENT_MESSAGE_SENDED = 2;

    private final UUID id;

    private SerialBinder mSerialBinder;
    private SerialManager mSerialManager = new SerialManager();
    private volatile SerialPort mSerialPort = null;
    private volatile boolean mSerialAbortRequest = false;

    private Object lock = new Object();

    private HandlerThread mHandlerThread;
    private BgHandler mBgHandler;
    private ByteBuffer mReadBuffer;
    private Thread mReadThread;
    private Callback mCallback;

    public SerialService() {
        super(TAG);
        // Generate unique identifier
        this.id = UUID.randomUUID();
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, SerialService.class);
    }

    public static Intent setServiceAlarm(Context context, int interval, boolean isOn) {
        Intent intent = newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (isOn) {
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime(), interval, pendingIntent);
        }  else {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
        return intent;
    }

    public static boolean isServiceAlarmOn(Context context) {
        Intent intent = SerialService.newIntent(context);
        PendingIntent pendingIntent =
                PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);
        return pendingIntent != null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //mSerialManager = (SerialManager) getSystemService(Context.SERIAL_SERVICE);
        mHandlerThread = new HandlerThread("serial_thread");
        mHandlerThread.start();
        mBgHandler = new BgHandler(mHandlerThread.getLooper());
        mReadBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
        Log.d(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandlerThread.quit();
        Log.d(TAG, "onDestroy");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (mSerialBinder == null) {
            mSerialBinder = new SerialBinder();
        }
        return mSerialBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        sendMessage("010300000037041C");
        if(mCallback != null) {
            mCallback.onHandle("010300000037041C");
        }
        Log.d(TAG, "Received an new Intent " + intent + ", ID : " + id);
    }


    public UUID getId() { return this.id; }

    public String[] getSerialPorts() {
        Log.d(TAG, "getSerialPorts  : " + Arrays.toString(mSerialManager.getSerialPorts()));
        return mSerialManager.getSerialPorts();
    }

    public String[] getSerialPaths() {
        Log.d(TAG, "getSerialPaths  : " + Arrays.toString(mSerialManager.getSerialPaths()));
        return mSerialManager.getSerialPaths();
    }

    public boolean openSerial(String name, String speed) {
        Log.d(TAG, "openSerial name : " + name + " speed : " + speed);
        if (isSerialOpend() || isNullOrEmpty(name)) {
            return false;
        }
        mBgHandler.obtainMessage(EVENT_OPEN_SERIAL, Integer.valueOf(speed), 0, name).sendToTarget();
        return true;
    }

    public boolean isSerialOpend() {
        return mSerialPort != null;
    }

    public void closeSerial() {
        if (isSerialOpend()) {
            mBgHandler.sendEmptyMessage(EVENT_CLOASE_SERAIL);
        }
    }

    public void sendMessage(String text) {
        mBgHandler.obtainMessage(EVENT_SEND_MESSAGES, text).sendToTarget();
    }

    private void doCloseSerial() {
        if (isSerialOpend()) {
            try {
                mSerialAbortRequest = true;
                Thread.sleep(1);
                mSerialPort.close();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            } finally {
                mSerialPort = null;
                mReadThread.interrupt();
                mReadThread = null;
                Log.d(TAG, "close serial successful!");
            }
        }
    }

    private void doOpenSerial(String name, int speed) {
        try {
            //设置串口号、波特率，
            mSerialPort = mSerialManager.openSerialPort(name, speed, 0);
        } catch (SecurityException e) {
            Log.e(TAG, e.getMessage());
            mSerialPort = null;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            mSerialPort = null;
        } finally {
            if (isSerialOpend()) {
                mSerialAbortRequest = false;
                mReadThread = new ReadThread();
                mReadThread.start();
                Log.d(TAG, "open serial successful!");
            }
        }
    }

    private void doSendMessage(String data) {
        try {
            if (isSerialOpend() && !mSerialAbortRequest) {
                byte[] snd = SerialUtilOld.hexStringToBytes(data);
                mSerialManager.write(ByteBuffer.wrap(snd), snd.length);
                StringBuilder sb = new StringBuilder(data);
                Log.d(TAG, "send data :" + sb.toString() + " array : " + Arrays.toString(snd));
                mHandler.obtainMessage(EVENT_MESSAGE_SENDED, data).sendToTarget();
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public class SerialBinder extends Binder {

        public SerialService getService() {
            return SerialService.this;
        }
    }



    private class ReadThread extends Thread {

        public ReadThread() {
            super("serial_read_thread");
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                if (isSerialOpend() && !mSerialAbortRequest) {
                    int size;
                    try {
                        byte [] data = mSerialManager.read(mReadBuffer);
                        if (null != data && data.length > 0) {
                            String rcv = SerialUtilOld.bytesToHexString(data, data.length);
                            Log.d(TAG, "read size is : " + data.length + ", data : " + rcv);
                            mHandler.obtainMessage(EVENT_MESSAGE_RECEIVED, data).sendToTarget();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage());
                        return;
                    }
                }
            }
        }
    }



    private class BgHandler extends Handler {
        public BgHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_OPEN_SERIAL: {
                    if (msg.obj != null) {
                        doOpenSerial((String) msg.obj, msg.arg1);
                    }
                }
                break;
                case EVENT_CLOASE_SERAIL: {
                    doCloseSerial();
                }
                break;
                case EVENT_SEND_MESSAGES: {
                    if (msg.obj != null) {
                        doSendMessage(String.valueOf(msg.obj));
                    }
                }
                break;
            }
        }
    }



    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_MESSAGE_RECEIVED:
                    if(msg.obj != null){
                        byte[] data = (byte[])msg.obj;
                        String rcv = SerialUtilOld.bytesToHexString(data, data.length);
                        if(mCallback != null){
                            mCallback.onDataReceived(rcv);
                        }
                    }
                    break;
                case EVENT_MESSAGE_SENDED:
                    if(msg.obj != null){
                        if(mCallback != null){
                            mCallback.onDataSend(String.valueOf(msg.obj));
                        }
                    }
                    break;
            }
        }
    };



    public void registerCallback(Callback callback) {
        mCallback = callback;
    }

    public void unregisterCallback(Callback callback) {
        if (mCallback == callback) {
            mCallback = null;
        }
    }

    public interface Callback {
        public void onHandle(String data);

        public void onDataSend(String data);

        public void onDataReceived(String data);
    }
}
