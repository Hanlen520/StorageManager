package com.iesqa.storagemanager.features.storage;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.iesqa.storagemanager.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by tongyao on 2018/1/15.
 */

public class StorageActivity extends AppCompatActivity implements View.OnClickListener {
    TextView internalInfoTv;
    TextView externalInfoTv;
    EditText fillStorageEt;
    Spinner unitSpinner;
    Handler handler = new Handler();
    File fillerDir;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage);
        fillerDir = getExternalFilesDir("filler");
        internalInfoTv = findViewById(R.id.internal_info);
        externalInfoTv = findViewById(R.id.external_info);
        unitSpinner = findViewById(R.id.support_units);
        // TODO 暂时不显示外部存储空间
        externalInfoTv.setVisibility(View.GONE);
        fillStorageEt = findViewById(R.id.fill_storage);
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setTitle("正在工作...");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                ) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
            }, 1);
        } else {
            init();
        }
    }
    private void init() {
        refreshDisplay();
        if (!fillerDir.isDirectory()) {
            if (fillerDir.isFile()) {
                removeDir(fillerDir);
            } else {
                fillerDir.mkdirs();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults != null && grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "权限已申请", Toast.LENGTH_LONG).show();
                    init();
                    break;
                } else {
                    Toast.makeText(this, "没有读写SD卡权限，请去系统设置给予权限", Toast.LENGTH_LONG).show();
                    finish();
                }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fill_submit:
                startFill();
                break;
            case R.id.clear_submit:
                clearFill();
                break;
        }
    }

    private void refreshDisplay() {
        internalInfoTv.setText("内部存储空间： 总大小:" + StorageHelper.getInternalMemorySize(this) +
                "  可用空间:" + StorageHelper.getAvailableInternalMemorySize(this));
        externalInfoTv.setText("外部存储空间： 总大小:" + StorageHelper.getExternalMemorySize(this) + "  可用空间:" + StorageHelper.getAvailableExternalMemorySize(this));
    }

    private void startFill() {
        final int totalCount = Integer.valueOf(fillStorageEt.getText().toString());
        final String unitValue = (String) unitSpinner.getSelectedItem();
        int weight = 1;
        switch (unitValue) {
            case "KB":
                weight = 1024;
                break;
            case "MB":
                weight = 1024 * 1024;
                break;
        }
        if (totalCount <= 0) {
            Toast.makeText(StorageActivity.this, "请输入合法的写入文件大小", Toast.LENGTH_LONG).show();
            return;
        }
        progressDialog.show();
        final int finalWeight = weight;
        new Thread() {
            @Override
            public void run() {
                super.run();
                OutputStream outputStream = null;
                byte[] data = new byte[finalWeight];
                File filePath = new File(fillerDir, UUID.randomUUID().toString() + ".data");
                int count = 0;
                try {
                    outputStream = new FileOutputStream(filePath, false);
                    for (int i = 0; i < totalCount; i++) {
                        outputStream.write(data);
                        count++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (outputStream != null) {
                            outputStream.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                final int finalCount = count;
                completeWork("写入存储:" + finalCount + unitValue);
            }
        }.start();
    }

    private void clearFill() {
        progressDialog.show();
        new Thread() {
            @Override
            public void run() {
                super.run();
                removeDir(fillerDir);
                completeWork("已清除!");
            }
        }.start();
    }

    private void completeWork(final String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                progressDialog.dismiss();
                refreshDisplay();
                Toast.makeText(StorageActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }


    /**
     * 删除文件夹
     *
     * @param fileOrDirectory
     */
    public static void removeDir(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files == null || files.length == 0) {
                return;
            }
            for (File child : files) {
                if (child.isDirectory()) {
                    removeDir(child);
                } else {
                    child.delete();
                }
            }

        }
    }
}
