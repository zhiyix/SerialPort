package com.game.serialport;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

public class MainActivity extends SingleFragmentActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String EXTRA_TITLE =
            "com.game.serialport.title";

    //! 1.静态构造传参函数
    public static Intent newIntent(Context packageContext, String title) {
        Intent intent = new Intent(packageContext, MainActivity.class);

        intent.putExtra(EXTRA_TITLE, title);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        String title= ((String) getIntent().getSerializableExtra(EXTRA_TITLE));

        return SerialFragment.newInstance(title);
    }
}
