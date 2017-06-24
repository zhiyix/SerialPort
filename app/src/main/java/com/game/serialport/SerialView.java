package com.game.serialport;

/**
 * Created by imetr on 2017/6/24.
 */

public interface SerialView {
    void onSentTextViewChanged(String data);

    void onReceivedTextViewChanged(String data);

    void updateDeviceList(String[] devices);

    void onSerialStatusChanged(boolean isOpend);

    void updateBaudrateList(String[] baudrates);
}
