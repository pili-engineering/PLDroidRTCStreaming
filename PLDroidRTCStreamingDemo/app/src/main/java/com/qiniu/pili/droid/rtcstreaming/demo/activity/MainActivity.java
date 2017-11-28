package com.qiniu.pili.droid.rtcstreaming.demo.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.qiniu.pili.droid.rtcstreaming.RTCMediaStreamingManager;
import com.qiniu.pili.droid.rtcstreaming.RTCServerRegion;
import com.qiniu.pili.droid.rtcstreaming.demo.BuildConfig;
import com.qiniu.pili.droid.rtcstreaming.demo.R;
import com.qiniu.pili.droid.rtcstreaming.demo.activity.conference.ConferenceEntryActivity;
import com.qiniu.pili.droid.rtcstreaming.demo.activity.streaming.RTCStreamingEntryActivity;
import com.qiniu.pili.droid.rtcstreaming.demo.utils.PermissionChecker;
import com.squareup.leakcanary.LeakCanary;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LeakCanary.install(getApplication());
        RTCMediaStreamingManager.init(getApplicationContext(), RTCServerRegion.RTC_CN_SERVER);

        TextView versionInfoTextView = (TextView) findViewById(R.id.version_info_textview);
        String info = "版本号：" + getVersionDescription() + "，编译时间：" + getBuildTimeDescription();
        versionInfoTextView.setText(info);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RTCMediaStreamingManager.deinit();
    }

    public void onClickRTCStreaming(View v) {
        if (isPermissionOK()) {
            Intent intent = new Intent(MainActivity.this, RTCStreamingEntryActivity.class);
            startActivity(intent);
        }
    }

    public void onClickRTCConference(View v) {
        if (isPermissionOK()) {
            Intent intent = new Intent(MainActivity.this, ConferenceEntryActivity.class);
            startActivity(intent);
        }
    }

    private boolean isPermissionOK() {
        PermissionChecker checker = new PermissionChecker(this);
        boolean isPermissionOK = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checker.checkPermission();
        if (!isPermissionOK) {
            Toast.makeText(this, "Some permissions is not approved !!!", Toast.LENGTH_SHORT).show();
        }
        return isPermissionOK;
    }

    private String getVersionDescription() {
        PackageManager packageManager = getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "未知";
    }

    protected String getBuildTimeDescription() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(BuildConfig.BUILD_TIMESTAMP);
    }
}
