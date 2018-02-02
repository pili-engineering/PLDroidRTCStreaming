package com.qiniu.pili.droid.rtcstreaming.demo.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class QiniuAppServer implements AppServerBase {

    // 区分主播和副主播
    public static final int RTC_ROLE_ANCHOR = 0x01;
    public static final int RTC_ROLE_VICE_ANCHOR = 0x02;

    private static final String APP_SERVER_ADDR = "https://api.pdex-service.com";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // 用户 ID 可以使用业务上的用户 ID
    private static String mUserId;
    private static String mAuthorization;

    private static class QiniuAppServerHolder {
        private static final QiniuAppServer INSTANCE = new QiniuAppServer();
    }

    private QiniuAppServer(){}

    public static final QiniuAppServer getInstance() {
        return QiniuAppServerHolder.INSTANCE;
    }

    public static void doAuthorization(String userId, String password) {
        mAuthorization = Base64.encodeToString((userId + ":" + password).getBytes(), Base64.DEFAULT).trim();
    }

    /**
     * 登录
     * 若想实现自己的服务器逻辑，可自行实现 AppServerBase 接口，并在登录时返回 true
     *
     * @param userName 登录用户名
     * @param password 登录密码
     * @return 成功或失败
     */
    @Override
    public boolean login(String userName, String password) {
        String url = APP_SERVER_ADDR + "/pili/v1/login";
        String requestBody = "{\"name\":\"" + userName + "\",\"password\":\"" + password + "\"}";
        String response = doPostRequest(url, requestBody, null);
        if (response == null) {
            return false;
        }
        try {
            JSONObject json = new JSONObject(response);
            return "200".equals(json.getString("code"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取推流地址
     * 若想实现自己的服务器逻辑，可自行实现 AppServerBase 接口
     *
     * @param roomName 房间号
     * @return 推流地址
     */
    @Override
    public String requestPublishAddress(String roomName) {
        String url = APP_SERVER_ADDR + "/pili/v1/stream/" + roomName;
        String response = doPostRequest(url, null, mAuthorization);
        if (response == null) {
            return null;
        }
        try {
            JSONObject json = new JSONObject(response);
            return json.getString("url");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取播放地址
     * 若想实现自己的服务器逻辑，可自行实现 AppServerBase 接口
     *
     * @param roomName 房间号
     * @return 播放地址
     */
    @Override
    public String requestPlayURL(String roomName) {
        String url = APP_SERVER_ADDR + "/pili/v1/stream/query/" + roomName;
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .header("Authorization", mAuthorization == null ? "" : mAuthorization)
                .url(url)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response == null) {
                return null;
            }
            JSONObject jsonObject = new JSONObject(response.body().string());
            if ("200".equals(jsonObject.getString("code"))) {
                return jsonObject.getString("rtmp");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取连麦 token
     * 若想实现自己的服务器逻辑，可自行实现 AppServerBase 接口
     *
     * @param userId 加入房间的 userId，可以和登录的 userName 不一样
     * @param roomName 房间号
     * @return 连麦 token
     */
    @Override
    public String requestRoomToken(String userId, String roomName) {
        String url = APP_SERVER_ADDR + "/pili/v1/room/token";
        String requestBody = "{\"room\":\"" + roomName + "\",\"user\":\"" + userId + "\",\"version\":\"2.0\"}";
        return doPostRequest(url, requestBody, mAuthorization);
    }

    private String doPostRequest(String url, String requestBody, String authorization) {
        try {
            OkHttpClient okHttpClient = new OkHttpClient();
            RequestBody body = RequestBody.create(JSON, requestBody == null ? "" : requestBody);
            Request request = new Request.Builder()
                    .header("Authorization", authorization == null ? "" : authorization)
                    .url(url)
                    .post(body)
                    .build();
            Response response =  okHttpClient.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static String getTestUserId(Context context) {
        if (mUserId != null) {
            return mUserId;
        }
        SharedPreferences preferences = context.getSharedPreferences("rtc", Context.MODE_PRIVATE);
        if (!preferences.contains("user_id")) {
            mUserId = "qiniu-" + UUID.randomUUID().toString();
            preferences.edit().putString("user_id", mUserId).apply();
        } else {
            mUserId = preferences.getString("user_id", "");
        }
        return mUserId;
    }
}
