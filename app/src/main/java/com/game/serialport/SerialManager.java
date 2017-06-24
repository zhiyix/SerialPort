package com.game.serialport;

import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android_serialport_api.SerialPort;
import android_serialport_api.SerialPortFinder;
import android_serialport_api.SerialUtilOld;

/**
 * Created by imetr on 2017/6/24.
 */

public class SerialManager {
    private SerialPortFinder mSerialPortFinder = new SerialPortFinder();
    private static final int READ_BUFFER_SIZE = 1024;

    private SerialPort mSerialPort;
    private InputStream mInputStream;
    private OutputStream mOutputStream;

    public String[] getSerialPorts() {
        String[] entries = mSerialPortFinder.getAllDevices();

        return entries;
    }

    public String[] getSerialPaths() {
        String[] entryValues = mSerialPortFinder.getAllDevicesPath();

        return entryValues;
    }

    public SerialPort openSerialPort(String path, int baudrate, int flags)
            throws SecurityException, IOException, InterruptedException {

        if (path.startsWith("tty")) {
            path = "/dev/" + path;
        }
        mSerialPort = new SerialPort(new File(path), baudrate, flags);

        if (mSerialPort != null) {
            //设置读、写
            mInputStream = mSerialPort.getInputStream();
            mOutputStream = mSerialPort.getOutputStream();
        } else {
            throw new NullPointerException("串口设置有误");
        }
        return mSerialPort;
    }

    public void write(ByteBuffer buffer, int length) throws IOException {
        if (mOutputStream == null) {
            throw new NullPointerException("mOutputStream is null");
        }
        mOutputStream.write(buffer.array());
    }

    public byte[] read(ByteBuffer readBuffer) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[READ_BUFFER_SIZE];
        if (mInputStream == null) {
            throw new NullPointerException("mInputStream is null");
        }
        if (mInputStream.available() > 0) {
            int size = -1;
            if ((size = mInputStream.read(buffer, 0, READ_BUFFER_SIZE)) != -1) {
                out.write(buffer, 0, size);
            }
            return out.toByteArray();
        }
        return null;
    }
}
