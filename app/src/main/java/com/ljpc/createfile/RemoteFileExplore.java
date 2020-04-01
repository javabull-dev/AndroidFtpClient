package com.ljpc.createfile;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ljpc.createfile.component.MainActivityComponentManager;

public class RemoteFileExplore extends AppCompatActivity {

    private static final String TAG = RemoteFileExplore.class.getName();
    TextView tv_location;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_file_explore);

        //ftp服务器 当前路径
        tv_location = findViewById(R.id.fp_tv_location);
        listView = findViewById(R.id.file_listView);

        //初始化按键点击事件
        initButtons();

        MainActivityComponentManager.put("tv_location",tv_location);
        MainActivityComponentManager.put("listView",listView);
    }

    private void initButtons() {
        findViewById(R.id.delete_dir).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        findViewById(R.id.delete_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        findViewById(R.id.return_main_activity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        findViewById(R.id.download_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

//    public void newFolderDialog(View v) {
//        AlertDialog dialog = new AlertDialog.Builder(this).create();
//        dialog.setTitle("Enter Folder Name");
//
//        final EditText et = new EditText(this);
//        dialog.setView(et);
//
//        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Create",
//                new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface arg0, int arg1) {
//                        createNewFolder(et.getText().toString());
//                    }
//                });
//        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
//                new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface arg0, int arg1) {
//
//                    }
//                });
//
//        dialog.show();
//
//    }
}
