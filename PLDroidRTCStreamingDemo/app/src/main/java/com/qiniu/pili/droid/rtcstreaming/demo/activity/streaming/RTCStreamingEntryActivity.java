package com.qiniu.pili.droid.rtcstreaming.demo.activity.streaming;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.multidex.MultiDex;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.pili.pldroid.player.PLNetworkManager;
import com.qiniu.pili.droid.rtcstreaming.demo.R;
import com.qiniu.pili.droid.rtcstreaming.demo.activity.playback.PlaybackActivity;
import com.qiniu.pili.droid.rtcstreaming.demo.core.QiniuAppServer;
import com.qiniu.pili.droid.rtcstreaming.demo.utils.StreamingSettings;

import java.net.UnknownHostException;

public class RTCStreamingEntryActivity extends AppCompatActivity {

    private static final String TAG = RTCStreamingEntryActivity.class.getSimpleName();

    private EditText mRoomEditText;
    private RadioGroup mCaptureRadioGroup;
    private RadioGroup mCodecRadioGroup;
    private RadioGroup mRTCModeRadioGroup;
    private RadioGroup mBitrateControlRadioGroup;
    private ProgressDialog mProgressDialog;
    private CheckBox mCheckBoxBeauty;
    private CheckBox mCheckBoxWatermark;
    private CheckBox mCheckBoxDebugMode;
    private CheckBox mCheckBoxCustomSetting;
    private CheckBox mCheckBoxAudioLevel;
    private CheckBox mCheckboxEnableStats;

    private Spinner mPreviewSizeRatioSpinner;
    private Spinner mPreviewSizeLevelSpinner;
    private Spinner mEncodingSizeRatioSpinner;
    private Spinner mEncodingSizeLevelSpinner;
    private Spinner mEncodingConfigSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtc_streaming_config);

        setTitle(R.string.rtc_rtmp);

        mCaptureRadioGroup = (RadioGroup) findViewById(R.id.CaptureRadioGroup);
        mCodecRadioGroup = (RadioGroup) findViewById(R.id.CodecRadioGroup);
        mRTCModeRadioGroup = (RadioGroup) findViewById(R.id.RTCModeGroup);
        mBitrateControlRadioGroup = (RadioGroup) findViewById(R.id.bitrate_control_group);

        mCheckBoxBeauty = (CheckBox) findViewById(R.id.CheckboxBeauty);
        mCheckBoxWatermark = (CheckBox) findViewById(R.id.CheckboxWatermark);
        mCheckBoxDebugMode = (CheckBox) findViewById(R.id.CheckboxDebugMode);
        mCheckBoxCustomSetting = (CheckBox) findViewById(R.id.CheckboxCustomSetting);
        mCheckBoxAudioLevel = (CheckBox) findViewById(R.id.CheckboxAudioLevel);
        mCheckboxEnableStats = (CheckBox) findViewById(R.id.CheckboxEnableStats);

        mPreviewSizeRatioSpinner = (Spinner) findViewById(R.id.PreviewSizeRatioSpinner);
        mPreviewSizeLevelSpinner = (Spinner) findViewById(R.id.PreviewSizeLevelSpinner);
        mEncodingSizeRatioSpinner = (Spinner) findViewById(R.id.EncodingSizeRatioSpinner);
        mEncodingSizeLevelSpinner = (Spinner) findViewById(R.id.EncodingSizeLevelSpinner);
        mEncodingConfigSpinner = (Spinner) findViewById(R.id.EncodingConfig);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, StreamingSettings.PREVIEW_SIZE_RATIO_TIPS_ARRAY);
        mPreviewSizeRatioSpinner.setAdapter(adapter);
        mPreviewSizeRatioSpinner.setSelection(1);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, StreamingSettings.PREVIEW_SIZE_LEVEL_TIPS_ARRAY);
        mPreviewSizeLevelSpinner.setAdapter(adapter);
        mPreviewSizeLevelSpinner.setSelection(1);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, StreamingSettings.ENCODING_SIZE_RATIO_TIPS_ARRAY);
        mEncodingSizeRatioSpinner.setAdapter(adapter);
        mEncodingSizeRatioSpinner.setSelection(1);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, StreamingSettings.ENCODING_SIZE_LEVEL_TIPS_ARRAY);
        mEncodingSizeLevelSpinner.setAdapter(adapter);
        mEncodingSizeLevelSpinner.setSelection(1);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, StreamingSettings.ENCODING_TIPS_ARRAY);
        mEncodingConfigSpinner.setAdapter(adapter);
        mEncodingConfigSpinner.setSelection(0);

        mRoomEditText = (EditText) findViewById(R.id.RoomNameEditView);
        SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        mRoomEditText.setText(preferences.getString("roomName", ""));

        MultiDex.install(this);
        try {
            PLNetworkManager.getInstance().startDnsCacheService(this);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        mProgressDialog = new ProgressDialog(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).edit();
        editor.putString("roomName", mRoomEditText.getText().toString());
        editor.apply();

        PLNetworkManager.getInstance().stopDnsCacheService(this);
    }

    public void onClickAnchor(View v) {
        if (mRTCModeRadioGroup.getCheckedRadioButtonId() == R.id.RadioLandscapePK) {
            jumpToPKActivity(PKAnchorActivity.class);
            return;
        }
        switch (mCaptureRadioGroup.getCheckedRadioButtonId()) {
            case R.id.RadioInnerCap:
                jumpToStreamingActivity(QiniuAppServer.RTC_ROLE_ANCHOR, RTCStreamingActivity.class);
                break;
            case R.id.RadioExtCap:
                jumpToStreamingActivity(QiniuAppServer.RTC_ROLE_ANCHOR, ExtCapStreamingActivity.class);
                break;
            case R.id.RadioAudioOnly:
                jumpToStreamingActivity(QiniuAppServer.RTC_ROLE_ANCHOR, RTCAudioStreamingActivity.class);
                break;
            default:
                break;
        }
    }

    public void onClickViceAnchor(View v) {
        if (mRTCModeRadioGroup.getCheckedRadioButtonId() == R.id.RadioLandscapePK) {
            jumpToPKActivity(PKViceAnchorActivity.class);
            return;
        }
        switch (mCaptureRadioGroup.getCheckedRadioButtonId()) {
            case R.id.RadioInnerCap:
                jumpToStreamingActivity(QiniuAppServer.RTC_ROLE_VICE_ANCHOR, RTCStreamingActivity.class);
                break;
            case R.id.RadioExtCap:
                jumpToStreamingActivity(QiniuAppServer.RTC_ROLE_VICE_ANCHOR, ExtCapStreamingActivity.class);
                break;
            case R.id.RadioAudioOnly:
                jumpToStreamingActivity(QiniuAppServer.RTC_ROLE_VICE_ANCHOR, RTCAudioStreamingActivity.class);
                break;
            default:
                break;
        }
    }

    public void onClickAudience(View v) {
        final String roomName = mRoomEditText.getText().toString();
        if ("".equals(roomName)) {
            showToastTips("请输入房间名称 !");
            return;
        }
        mProgressDialog.setMessage("正在获取播放地址..");
        mProgressDialog.show();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                String playURL = QiniuAppServer.getInstance().requestPlayURL(roomName);
                if (playURL == null) {
                    dismissProgressDialog();
                    showToastTips("无法获取播放地址或者房间信息 !");
                    return;
                }
                dismissProgressDialog();
                Log.d(TAG,"Playback: " + playURL);
                Intent intent = new Intent(RTCStreamingEntryActivity.this, PlaybackActivity.class);
                intent.putExtra("videoPath", playURL);
                intent.putExtra("roomName", roomName);
                boolean isExtCapture = mCaptureRadioGroup.getCheckedRadioButtonId() == R.id.RadioExtCap;
                boolean isAudioOnly = mCaptureRadioGroup.getCheckedRadioButtonId() == R.id.RadioAudioOnly;
                intent.putExtra("extCapture", isExtCapture);
                intent.putExtra("audioOnly", isAudioOnly);
                intent.putExtra("swcodec", mCodecRadioGroup.getCheckedRadioButtonId() == R.id.RadioSWCodec);
                intent.putExtra("orientation", mRTCModeRadioGroup.getCheckedRadioButtonId() != R.id.RadioPortrait);
                intent.putExtra("pkmode", mRTCModeRadioGroup.getCheckedRadioButtonId() == R.id.RadioLandscapePK);
                intent.putExtra("beauty", mCheckBoxBeauty.isChecked());
                startActivity(intent);
            }
        });
    }

    private void jumpToPKActivity(Class<?> cls) {
        final String roomName = mRoomEditText.getText().toString();
        if ("".equals(roomName)) {
            showToastTips("请输入房间名称 !");
            return;
        }
        Intent intent = new Intent(this, cls);
        intent.putExtra("roomName", roomName);
        intent.putExtra("swcodec", mCodecRadioGroup.getCheckedRadioButtonId() == R.id.RadioSWCodec);
        intent.putExtra("beauty", mCheckBoxBeauty.isChecked());
        intent.putExtra("watermark", mCheckBoxWatermark.isChecked());
        intent.putExtra("debugMode", mCheckBoxDebugMode.isChecked());
        intent.putExtra("customSetting", mCheckBoxCustomSetting.isChecked());
        intent.putExtra("enableStats", mCheckboxEnableStats.isChecked());
        intent.putExtra("bitrateControl",
                mBitrateControlRadioGroup.getCheckedRadioButtonId() == R.id.bitrate_auto ? "auto"
                        : (mBitrateControlRadioGroup.getCheckedRadioButtonId() == R.id.bitrate_manual ? "manual" : "diable"));
        startActivity(intent);
    }

    private void jumpToStreamingActivity(int role, Class<?> cls) {
        final String roomName = mRoomEditText.getText().toString();
        if ("".equals(roomName)) {
            showToastTips("请输入房间名称 !");
            return;
        }
        Intent intent = new Intent(this, cls);
        intent.putExtra("role", role);
        intent.putExtra("roomName", roomName.trim());
        intent.putExtra("swcodec", mCodecRadioGroup.getCheckedRadioButtonId() == R.id.RadioSWCodec);
        intent.putExtra("orientation", mRTCModeRadioGroup.getCheckedRadioButtonId() != R.id.RadioPortrait);
        intent.putExtra("beauty", mCheckBoxBeauty.isChecked());
        intent.putExtra("watermark", mCheckBoxWatermark.isChecked());
        intent.putExtra("debugMode", mCheckBoxDebugMode.isChecked());
        intent.putExtra("enableStats", mCheckboxEnableStats.isChecked());
        intent.putExtra("customSetting", mCheckBoxCustomSetting.isChecked());
        intent.putExtra("audioLevelCallback", mCheckBoxAudioLevel.isChecked());
        intent.putExtra("bitrateControl",
                mBitrateControlRadioGroup.getCheckedRadioButtonId() == R.id.bitrate_auto ? "auto"
                        : (mBitrateControlRadioGroup.getCheckedRadioButtonId() == R.id.bitrate_manual ? "manual" : "diable"));
        intent.putExtra(RTCStreamingActivity.PREVIEW_SIZE_RATIO, mPreviewSizeRatioSpinner.getSelectedItemPosition());
        intent.putExtra(RTCStreamingActivity.PREVIEW_SIZE_LEVEL, mPreviewSizeLevelSpinner.getSelectedItemPosition());
        intent.putExtra(RTCStreamingActivity.ENCODING_SIZE_RATIO, mEncodingSizeRatioSpinner.getSelectedItemPosition());
        intent.putExtra(RTCStreamingActivity.ENCODING_SIZE_LEVEL, mEncodingSizeLevelSpinner.getSelectedItemPosition());
        intent.putExtra(RTCStreamingActivity.ENCODING_CONFIG, mEncodingConfigSpinner.getSelectedItemPosition());
        startActivity(intent);
    }

    private void showToastTips(final String tips) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RTCStreamingEntryActivity.this, tips, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void dismissProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.dismiss();
            }
        });
    }
}
