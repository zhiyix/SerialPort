package com.game.serialport;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;

import android_serialport_api.SerialUtilOld;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private static final int READ_BUFFER_SIZE = 1024;

    private TextView mReceiveTextView;
    private Button mReceiveButton;
    private EditText mSendEditText;
    private Button mSendButton;
    private Button mStopButton;
    private ReadThread mReadThread;
    private ByteBuffer mReadBuffer;
    
    private SerialUtilOld mSerialUtilOld;
    private String path = "/dev/ttyO2";
    private int baudrate = 115200;
    private int flags = 0;
    private int size = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void init() {
        mReceiveTextView = (TextView) findViewById(R.id.main_recive_tv);
        mReceiveButton = (Button) findViewById(R.id.main_recive_b);
        mSendEditText = (EditText) findViewById(R.id.main_send_et);
        mSendButton = (Button) findViewById(R.id.main_send_b);
        mStopButton = (Button) findViewById(R.id.main_stop_b);
        mReceiveButton.setOnClickListener(this);
        mSendButton.setOnClickListener(this);
        mStopButton.setOnClickListener(this);
        try {
            //设置串口号、波特率，
            mSerialUtilOld = new SerialUtilOld(path, baudrate, 0);
        } catch (NullPointerException e) {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        mReadBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.main_recive_b: {
//              if(size!=-1){
                mReadThread = new ReadThread();
                mReadThread.start();
//              }
            }
            break;
            case R.id.main_send_b: {
                String context = mSendEditText.getText().toString();
                Log.d(TAG, "onClick: " + context);
                try {
                    mSerialUtilOld.setData(context.getBytes());
                } catch (NullPointerException e) {
                    Toast.makeText(MainActivity.this, "串口设置有误，无法发送", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
            break;
            case R.id.main_stop_b: {
                //停止接收
                mReadThread.interrupt();
                mReceiveTextView.setText("");
            }
            break;
        }
    }


    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!Thread.currentThread().isInterrupted()) {
                int size;
                try {
                    size = mSerialUtilOld.read(mReadBuffer);
                    if (size > 0) {
                        byte[] data = new byte[size];
                        mReadBuffer.get(data, 0, size);
                        Log.d(TAG, "read size is : " + size + " data : " + new String(data));
                        mReadBuffer.clear();
                        String rcv = SerialUtilOld.bytesToHexString(data, data.length);
                        onDataReceived(rcv);
                    }
                } catch (NullPointerException e) {
                    onDataReceived("-1");
                    e.printStackTrace();
                    mReadThread.interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                    onDataReceived("-1");
                    mReadThread.interrupt();
                }
            }
        }
    }

    protected void onDataReceived(final String data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //显示出来
                if ("-1".equals(data)) {
                    Toast.makeText(MainActivity.this, "串口设置有误，无法接收", Toast.LENGTH_SHORT).show();
                } else {
                    mReceiveTextView.append(data);
                }
            }
        });
    }
}
