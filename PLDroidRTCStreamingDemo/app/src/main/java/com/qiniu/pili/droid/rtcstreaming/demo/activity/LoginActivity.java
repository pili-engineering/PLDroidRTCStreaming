package com.qiniu.pili.droid.rtcstreaming.demo.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.qiniu.pili.droid.rtcstreaming.demo.R;
import com.qiniu.pili.droid.rtcstreaming.demo.core.QiniuAppServer;

public class LoginActivity extends AppCompatActivity {

    private EditText mUserNameText;
    private EditText mPasswordText;

    private String mUserName;
    private String mPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mUserNameText = (EditText) findViewById(R.id.user_name);
        mPasswordText = (EditText) findViewById(R.id.password);

        SharedPreferences sharedPreferences = this.getSharedPreferences("loginInfo", MODE_PRIVATE);
        mUserNameText.setText(sharedPreferences.getString("userName", ""));
        mPasswordText.setText(sharedPreferences.getString("password", ""));
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences sharedPreferences = getSharedPreferences("loginInfo", MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userName", mUserName);
        editor.putString("password", mPassword);
        editor.commit();
    }

    public void onClickLogin(View v) {
        if (!QiniuAppServer.isNetworkAvailable(this)) {
            Toast.makeText(LoginActivity.this, "network is unavailable!!!", Toast.LENGTH_SHORT).show();
            return;
        }
        mUserName = mUserNameText.getText().toString().trim();
        mPassword = mPasswordText.getText().toString().trim();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (QiniuAppServer.getInstance().login(mUserName, mPassword)) {
                    QiniuAppServer.doAuthorization(mUserName, mPassword);
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LoginActivity.this, "login failed!!!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }
}
