package com.ljpc.createfile;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ljpc.createfile.component.ExitTipDialog;
import com.ljpc.createfile.component.MainActivityComponentManager;
import com.ljpc.createfile.config.FtpConfig;
import com.ljpc.createfile.thread.UploadContentRunnable;
import com.ljpc.createfile.thread.UploadUriFileRunnable;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getName();
    private static final int SDCARD_PERMISSION = 1;
    private static final int CHOOSE_FILE_CODE = 1;
    EditText mContendBoard;
    //线程池
    ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initButton();
        initEditText();
        checkStoragePermission();
        registerComponent();
    }

    private void registerComponent() {
        MainActivityComponentManager.put("contentBoard", mContendBoard);
    }

    private void initButton() {
        findViewById(R.id.delete_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mContendBoard.setText("");
            }
        });

        findViewById(R.id.file_select).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(intent.EXTRA_ALLOW_MULTIPLE, true);
                try {
                    startActivityForResult(Intent.createChooser(intent, "选择文件"), CHOOSE_FILE_CODE);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getApplication(), "未找到文件管理器", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.exit_app).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ExitTipDialog dialog = new ExitTipDialog(MainActivity.this);
                dialog.show();
            }
        });

        findViewById(R.id.upload_content).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = mContendBoard.getText().toString();
                if (Objects.nonNull(content) && !content.isEmpty()){
                    executorService.execute(new UploadContentRunnable(getApplication(),content));
                }else {
                    Toast.makeText(getApplication(), "content无内容", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void initEditText() {
        mContendBoard = findViewById(R.id.content_board);
    }

    public void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        SDCARD_PERMISSION);
            }
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        SDCARD_PERMISSION);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Activity.RESULT_OK == resultCode) {
            if (requestCode == CHOOSE_FILE_CODE) {
                final Uri uri = data.getData();
                if (Objects.isNull(uri)) {
                    ClipData clipData = data.getClipData();
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        Uri tmp = item.getUri();
                        executorService.execute(new UploadUriFileRunnable(getApplication(), tmp));
                    }
                } else {
                    executorService.execute(new UploadUriFileRunnable(getApplication(), uri));
                }
            }
        }
    }

//    private void showProgressDialog() {
//        ProgressDialog dialog = new ProgressDialog(this);
//        dialog.setTitle("提示");
//        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//        dialog.setMax(10);
//        dialog.setMessage("上传中");
//        dialog.setProgress(0);
//        dialog.setIndeterminate(false);
//        dialog.setCanceledOnTouchOutside(false);
//        dialog.show();
//        MainActivityComponentManager.put("dialog", dialog);
//    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            ExitTipDialog dialog = new ExitTipDialog(MainActivity.this);
            dialog.show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void finish() {
        FtpConfig.getFtpConfig().saveParam(getApplicationContext());
        super.finish();
        executorService.shutdown();
    }

    @Override
    protected void onStart() {
        FtpConfig.getFtpConfig().loadParam(getApplicationContext());
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        Intent intent = null;
        switch (item.getItemId()){
            case R.id.remote_file:
                intent = new Intent(MainActivity.this, RemoteFileExplore.class);
                startActivity(intent);
                Log.e(TAG, "跳转到远程文件activity");
                break;
            case R.id.ftp_set:
                intent = new Intent(MainActivity.this, SetActivity.class);
                startActivity(intent);
                Log.e(TAG, "跳转到ftp设置activity");
                break;
            default:
                break;
        }
        return true;
    }
}
