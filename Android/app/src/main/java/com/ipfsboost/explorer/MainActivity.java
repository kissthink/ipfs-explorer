package com.ipfsboost.explorer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ipfsboost.explorer.activity.WebActivity;
import com.ipfsboost.explorer.event.CmdIntentServiceDaemonEvent;
import com.ipfsboost.explorer.services.CmdIntentService;
import com.ipfsboost.explorer.utils.DialogUtil;
import com.ipfsboost.explorer.utils.LogUtils;
import com.ipfsboost.explorer.utils.ToastUtils;
import com.umeng.analytics.MobclickAgent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import com.ipfsboost.explorer.activity.BaseActivity;
import com.ipfsboost.library.IpfsBox;
import com.ipfsboost.library.entity.Add;
import com.ipfsboost.library.entity.Stats_bw;

import java.io.IOException;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import droidninja.filepicker.FilePickerBuilder;
import droidninja.filepicker.FilePickerConst;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseActivity implements View.OnClickListener {
    @BindView(R.id.btn_play)
    Button btn_play;
    @BindView(R.id.btn_scan)
    Button btn_scan;
    @BindView(R.id.start)
    Button start;
    @BindView(R.id.stop)
    Button stop;
    @BindView(R.id.edt_search)
    EditText edt_search;
    @BindView(R.id.tv_upload)
    TextView tv_upload;
    @BindView(R.id.icon_group)
    TextView icon_group;
    private static int WRITE_EXTERNAL_STORAGE = 144;

    public Dialog dialog;


    ActionBar supportActionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        supportActionBar = getSupportActionBar();
        supportActionBar.setTitle("↑");
        supportActionBar.setSubtitle("↓");
        supportActionBar.setDisplayShowHomeEnabled(true);
        supportActionBar.setIcon(R.drawable.icon_status_dot_green);

        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        btn_play.setOnClickListener(this);
        btn_scan.setOnClickListener(this);
        tv_upload.setOnClickListener(this);
        icon_group.setOnClickListener(this);
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.btn_play:
//                playVideo();
                play();
                break;
            case R.id.btn_scan:
                MobclickAgent.onEvent(this, "MainActivity_scan");
                new AlertDialog.Builder(MainActivity.this).setMessage("TODO!").create().show();
                break;
            case R.id.tv_upload:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    if (ContextCompat.checkSelfPermission(MainActivity.this, FilePickerConst.PERMISSIONS_FILE_PICKER)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                WRITE_EXTERNAL_STORAGE);
                    } else {
                        MobclickAgent.onEvent(this, "MainActivity_add");
                        FilePickerBuilder.getInstance().setMaxCount(1)
                                .setActivityTheme(R.style.LibAppTheme)
                                .enableVideoPicker(true).pickPhoto(this);
                    }
                break;
            case R.id.start:
                CmdIntentService.startActionDaemon(this);
                break;
            case R.id.stop:
                CmdIntentService.startActionShutdown(this);
                break;
            case R.id.icon_group:
                MobclickAgent.onEvent(this, "MainActivity_jumpGroup");
                startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://t.me/joinchat/HjRVuhJL9AcrSAXvwhBjYw")));
                break;
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == FilePickerConst.REQUEST_CODE_PHOTO) {
                ArrayList<String> stringArrayListExtra = data.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_MEDIA);
                LogUtils.e("stringArrayListExtra.get(0)" + stringArrayListExtra.get(0));
//                CmdIntentService.startActionAdd(MainActivity.this, stringArrayListExtra.get(0));
                IpfsBox ipfsBox = new IpfsBox();
                ipfsBox.add(new Callback<Add>() {
                    @Override
                    public void onResponse(Call<Add> call, Response<Add> response) {
                        Add add = response.body();
                        if (add != null) {
                            final String hashCode = add.getHash();
                            dialog = DialogUtil.createGeneral(MainActivity.this, "HASH: " + hashCode);
                            dialog.setCanceledOnTouchOutside(false);
                            DialogUtil.addOnClickListener(dialog, R.id.Positive, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                    // 将文本内容放到系统剪贴板里。
                                    cm.setText(hashCode + "");
                                    ToastUtils.show("copied: " + hashCode);
                                }
                            });
                            DialogUtil.addOnClickListener(dialog, R.id.Negative, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    dialog.dismiss();
                                }
                            });
                            dialog.show();
                        } else {
                            try {
                                LogUtils.e(response.errorBody().string());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
//                        try {Utils.show("copied: " + body);

                    }

                    @Override
                    public void onFailure(Call<Add> call, Throwable t) {
                        LogUtils.e(t.getMessage());
                    }
                }, stringArrayListExtra.get(0));

            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void MessageEvent(Object event) {
        if (event instanceof CmdIntentServiceDaemonEvent) {
//            runTimer();
        } else if (event instanceof Stats_bw) {
            supportActionBar.setTitle("↑ " + ((Stats_bw)event).getRateOut() / 1024f + "kb/s");
            supportActionBar.setSubtitle("↓ " + ((Stats_bw)event).getRateIn() / 1024f + "kb/s");
        }
    }



    /**
     * play
     */
    private void play() {
        MobclickAgent.onEvent(this, "MainActivity_play");
        if (!TextUtils.isEmpty(edt_search.getText().toString())) {
            Intent intent = new Intent(MainActivity.this, WebActivity.class);
            intent.putExtra("hash", edt_search.getText().toString());
            startActivity(intent);
        } else {
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setMessage("Hash can not be empty!!!").create();
            alertDialog.show();
        }
    }

}
