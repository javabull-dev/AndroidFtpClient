package com.ljpc.createfile.handler;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.ljpc.createfile.FilePojo;
import com.ljpc.createfile.FolderAdapter;
import com.ljpc.createfile.component.MainActivityComponentManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import it.sauronsoftware.ftp4j.FTPFile;

public class RemoteFileHandler extends Handler {
    public static final String TAG = RemoteFileHandler.class.getName();
    private final Activity content;
    public static final int OBTAIN_FIAL = 0, OBTAIN_SUCCESS = 1;
    ArrayList<FilePojo> folderAndFileList;

    public RemoteFileHandler(Activity content) {
        this.content = content;
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case OBTAIN_FIAL:
                showToast(msg.obj);
                break;
            case OBTAIN_SUCCESS:
                loadLists(msg.obj);
                break;
            default:
                break;
        }
    }

    void showToast(@NonNull Object object) {
        if (object instanceof String) {
            String msg = (String) object;
            Toast.makeText(content, msg, Toast.LENGTH_SHORT).show();
        }
    }

    void loadLists(@NonNull Object object) {
        FTPFile[] files = (FTPFile[]) object;
        TextView tv_location = (TextView) MainActivityComponentManager.get("tv_location");

        try {
            ArrayList<FilePojo> foldersList;
            ArrayList<FilePojo> filesList;
            //tv_location.setText("当前目录: " + folder.getAbsolutePath());

            foldersList = new ArrayList<>();
            filesList = new ArrayList<>();

            for (FTPFile currentFile : files) {
                if (currentFile.getType() == FTPFile.TYPE_DIRECTORY) {
                    FilePojo filePojo = new FilePojo(currentFile.getName(), true);
                    foldersList.add(filePojo);
                } else if (currentFile.getType() == FTPFile.TYPE_FILE) {
                    FilePojo filePojo = new FilePojo(currentFile.getName(), false);
                    filesList.add(filePojo);
                }else {
                    //todo when currentFile is link
                }
            }

            Comparator<FilePojo> comparatorAscending = new Comparator<FilePojo>() {
                @Override
                public int compare(FilePojo f1, FilePojo f2) {
                    return f1.getName().compareTo(f2.getName());
                }
            };

            // sort & add to final List - as we show folders first add folders first to the final list
            Collections.sort(foldersList, comparatorAscending);
            folderAndFileList = new ArrayList<>();
            folderAndFileList.addAll(foldersList);

            showList();

        } catch (Exception e) {
            e.printStackTrace();
        }

    } // load List

    void showList() {
        try {
            FolderAdapter FolderAdapter = new FolderAdapter(content, folderAndFileList);
            ListView listView = (ListView) MainActivityComponentManager.get("listView");
            listView.setAdapter(FolderAdapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    //todo file click
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
