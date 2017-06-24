package com.game.serialport;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SerialFragment extends Fragment
        implements SerialView, View.OnClickListener {
    private static final String TAG = SerialFragment.class.getSimpleName();
    private static final String EXTRA_TITLE =
            "com.game.serialport.SerialFragment.title";

    private TextView mReceiveTextView;
    private Button mReceiveButton;
    private EditText mSendEditText;
    private Button mStopButton;
    private Button mSendButton;

    private Switch mSerialSwitch;
    private String mTitle;
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

    public static Fragment newInstance(String param1) {
        SerialFragment fragment = new SerialFragment();
        Bundle args = new Bundle();

        args.putString(EXTRA_TITLE, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* Initialize the contents of the Activity's standaed options menu.
         * You should place your menu items in to menu. For this method to be called,
         * you must have first called setHasOptionsMenu(boolean) */
        setHasOptionsMenu(true);

        Bundle bundle = getArguments();
        mTitle = bundle.getString(EXTRA_TITLE);
        mSerialPresensor = new SerialPresensor(getActivity(), this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_view, container, false);

        /*
        if (savedInstanceState != null) {
            mSubtitleVisible = savedInstanceState.getBoolean(SAVED_SUBTITLE_VISIBLE);
        }*/
        initViews(v);
        mSerialPresensor.doBindService();
        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (mSerialPresensor.isServiceAlarmOn()) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }
        Log.d(TAG, "选项菜单布置完成");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSerialPresensor.doUnbindService();
        Log.d(TAG, "onDestroy()");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_toggle_polling:
                mSerialPresensor.doToggleService();
                getActivity().invalidateOptionsMenu();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViews(View view) {
        mSerialSwitch = (Switch) view.findViewById(R.id.serial_switch);
        mDevicesSpinner = (Spinner) view.findViewById(R.id.devices);
        mBaudSpinner = (Spinner) view.findViewById(R.id.baudrates);
        mSerialSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isNullOrEmpty((String) mDevicesSpinner.getSelectedItem());
                    mSerialPresensor.doOpenSerial(mDevicesSpinner.getSelectedItemPosition(),
                            (String) mBaudSpinner.getSelectedItem());
                    /*Log.d(TAG, mDevicesSpinner.getSelectedItemPosition() + "," +
                            mDevicesSpinner.getSelectedItemId());*/
                } else {
                    mSerialPresensor.doCloseSerial();
                }
            }
        });
        mSendButton = (Button) view.findViewById(R.id.send_bt);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence data = mSendDataEditText.getText();
                if ((null == data) || !(data.length() > 0)) {
                    return;
                }
                if (!mSerialPresensor.doSentSerial(data)) {
                    Toast.makeText(getActivity(), "Please open serial!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        mSendDataEditText = (EditText) view.findViewById(R.id.send_ed);
        mSendDataEditText.setText("010300000037041C");
        mSendTextView = (TextView) view.findViewById(R.id.send_data);
        mReceivedTextView = (TextView) view.findViewById(R.id.received_data);
    }

    @Override
    public String getTitle() {
        return mTitle;
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
        mDevicesSpinner.setAdapter(new CustomApdater(getActivity(),
                android.R.layout.simple_dropdown_item_1line, devices));
        mDevicesSpinner.setSelection(devices.length - 1);
    }

    @Override
    public void updateBaudrateList(String[] baudrates) {
        mBaudSpinner.setAdapter(new CustomApdater(getActivity(),
                android.R.layout.simple_dropdown_item_1line, baudrates));
        mBaudSpinner.setSelection(baudrate);
    }

    private class CustomApdater extends ArrayAdapter<String> implements SpinnerAdapter {

        public CustomApdater(Context context, int resource, String[] datas) {
            super(context, resource, datas);
        }
    }
}
