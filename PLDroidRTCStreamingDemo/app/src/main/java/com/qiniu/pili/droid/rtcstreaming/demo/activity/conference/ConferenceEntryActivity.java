package com.qiniu.pili.droid.rtcstreaming.demo.activity.conference;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.multidex.MultiDex;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.qiniu.pili.droid.rtcstreaming.demo.R;

public class ConferenceEntryActivity extends AppCompatActivity {

    private EditText mRoomEditText;
    private RadioGroup mCodecRadioGroup;
    private RadioGroup mRTCModeRadioGroup;
    private CheckBox mCheckBoxBeauty;
    private CheckBox mCheckBoxDebugMode;
    private CheckBox mCheckBoxAudioLevel;
    private CheckBox mCheckboxEnableStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtc_conference_config);

        setTitle(R.string.rtc_only);

        mCodecRadioGroup = (RadioGroup) findViewById(R.id.CodecRadioGroup);
        mRTCModeRadioGroup = (RadioGroup) findViewById(R.id.RTCModeGroup);

        mCheckBoxBeauty = (CheckBox) findViewById(R.id.CheckboxBeauty);
        mCheckBoxDebugMode = (CheckBox) findViewById(R.id.CheckboxDebugMode);
        mCheckBoxAudioLevel = (CheckBox) findViewById(R.id.CheckboxAudioLevel);
        mCheckboxEnableStats = (CheckBox) findViewById(R.id.CheckboxEnableStats);

        mRoomEditText = (EditText) findViewById(R.id.RoomNameEditView);
        SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        mRoomEditText.setText(preferences.getString("roomName", ""));

        MultiDex.install(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).edit();
        editor.putString("roomName", mRoomEditText.getText().toString());
        editor.apply();
    }

    public void onClickConference(View v) {
        final String roomName = mRoomEditText.getText().toString();
        if ("".equals(roomName)) {
            showToastTips("请输入房间名称 !");
            return;
        }
        Intent intent = new Intent(this, ConferenceActivity.class);
        intent.putExtra("roomName", roomName.trim());
        intent.putExtra("swcodec", mCodecRadioGroup.getCheckedRadioButtonId() == R.id.RadioSWCodec);
        intent.putExtra("orientation", mRTCModeRadioGroup.getCheckedRadioButtonId() != R.id.RadioPortrait);
        intent.putExtra("beauty", mCheckBoxBeauty.isChecked());
        intent.putExtra("debugMode", mCheckBoxDebugMode.isChecked());
        intent.putExtra("audioLevelCallback", mCheckBoxAudioLevel.isChecked());
        intent.putExtra("enableStats", mCheckboxEnableStats.isChecked());
        startActivity(intent);
    }

    public void onClickConferenceAudience(View v) {
        final String roomName = mRoomEditText.getText().toString();
        Intent intent = new Intent(this, ConferenceAudienceActivity.class);
        intent.putExtra("roomName", roomName.trim());
        intent.putExtra("audioLevelCallback", mCheckBoxAudioLevel.isChecked());
        startActivity(intent);
    }

    private void showToastTips(final String tips) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ConferenceEntryActivity.this, tips, Toast.LENGTH_SHORT).show();
            }
        });
    }
}