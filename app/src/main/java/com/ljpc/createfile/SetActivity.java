package com.ljpc.createfile;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ljpc.createfile.config.FtpConfig;

public class SetActivity extends AppCompatActivity {
    EditText mUserName, mPassoword, mAddress, mPort, mFtpNick;
    Spinner mProtol;
    String protol = null;
    public static final String TAG = SetActivity.class.getName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set);
        initView();
        initParam();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initView() {
        findViewById(R.id.return_main).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveParm();
                finish();
            }
        });

        mProtol = findViewById(R.id.protol);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.
                createFromResource(this, R.array.protrol, android.R.layout.simple_spinner_item);
        mProtol.setAdapter(adapter);
        mProtol.setVisibility(View.VISIBLE);
        mProtol.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                Object item = arg0.getItemAtPosition(arg2);
                if (item instanceof String) {
                    protol = (String) item;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        mFtpNick = findViewById(R.id.ftp_nick);
        mUserName = findViewById(R.id.ftp_user_name);
        mPassoword = findViewById(R.id.ftp_password);
        mAddress = findViewById(R.id.address);
        mPort = findViewById(R.id.port);
    }

    private void initParam() {
        FtpConfig config = FtpConfig.getFtpConfig();
        mFtpNick.setText(config.getName() == null ? "" : config.getName());
        mPort.setText(config.getPort() == 0 ? "21" : String.valueOf(config.getPort()));
        mAddress.setText(config.getAddress() == null ? "" : config.getAddress());
        mPassoword.setText(config.getFtpPassword() == null ? "" : config.getFtpPassword());
        mUserName.setText(config.getFtpUserName() == null ? "" : config.getFtpUserName());
    }

    private void saveParm() {
        FtpConfig config = FtpConfig.getFtpConfig();
        config.setAddress(mAddress.getText().toString().trim());
        config.setFtpPassword(mPassoword.getText().toString().trim());
        config.setFtpUserName(mUserName.getText().toString().trim());
        config.setName(mFtpNick.getText().toString().trim());
        config.setPort(Integer.valueOf(mPort.getText().toString().trim()));
        config.setAddress(mAddress.getText().toString().trim());
        config.setProtol(protol.equals("FTP") ? FtpConfig.FTP : FtpConfig.SFTP);
    }
}


