package com.ljpc.createfile.component;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class ExitTipDialog {
    private AlertDialog.Builder builder;
    private Activity activity;
    public ExitTipDialog(Activity activity) {
        this.activity = activity;
        builder = new AlertDialog.Builder(activity);
        builder.setMessage("是否要退出应用?")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        exit();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setTitle("提示")
                .setCancelable(false);
    }

    public void show() {
        if (builder != null) {
            builder.show();
        }
    }

    private void exit() {
        activity.finish();
    }
}
