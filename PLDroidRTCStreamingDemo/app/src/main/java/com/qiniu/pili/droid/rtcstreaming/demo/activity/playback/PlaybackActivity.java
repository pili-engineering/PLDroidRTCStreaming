package com.qiniu.pili.droid.rtcstreaming.demo.activity.playback;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.pili.pldroid.player.AVOptions;
import com.pili.pldroid.player.PLMediaPlayer;
import com.pili.pldroid.player.widget.PLVideoView;
import com.qiniu.pili.droid.rtcstreaming.demo.activity.streaming.ExtCapStreamingActivity;
import com.qiniu.pili.droid.rtcstreaming.demo.activity.streaming.PKViceAnchorActivity;
import com.qiniu.pili.droid.rtcstreaming.demo.core.QiniuAppServer;
import com.qiniu.pili.droid.rtcstreaming.demo.activity.streaming.RTCAudioStreamingActivity;
import com.qiniu.pili.droid.rtcstreaming.demo.activity.streaming.RTCStreamingActivity;

public class PlaybackActivity extends AppCompatActivity {

    private static final String TAG = PlaybackActivity.class.getSimpleName();

    private static final int MESSAGE_ID_RECONNECTING = 0x01;
    private static final int DEFAULT_PREVIEW_SIZE_RATIO = 1;
    private static final int DEFAULT_PREVIEW_SIZE_LEVEL = 1;

    private View mLoadingView;
    private PLVideoView mVideoView;
    private Toast mToast = null;
    private String mVideoPath = null;
    private String mRoomName;
    private boolean mIsExtCapture = false;
    private boolean mIsLandscape = false;
    private boolean mIsAudioOnly = false;
    private boolean mIsSWCodec = true;
    private boolean mIsActivityPaused = true;
    private boolean mIsPKMode = false;
    private boolean mIsFaceBeautyEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(com.qiniu.pili.droid.rtcstreaming.demo.R.layout.activity_playback);
        mVideoView = (PLVideoView) findViewById(com.qiniu.pili.droid.rtcstreaming.demo.R.id.VideoView);

        mLoadingView = findViewById(com.qiniu.pili.droid.rtcstreaming.demo.R.id.LoadingView);
        mVideoView.setBufferingIndicator(mLoadingView);

        mVideoPath = getIntent().getStringExtra("videoPath");
        mRoomName  = getIntent().getStringExtra("roomName");
        mIsExtCapture = getIntent().getBooleanExtra("extCapture", false);
        mIsPKMode = getIntent().getBooleanExtra("pkmode", false);
        mIsLandscape = getIntent().getBooleanExtra("orientation", false);
        mIsAudioOnly = getIntent().getBooleanExtra("audioOnly", false);
        mIsSWCodec = getIntent().getBooleanExtra("swcodec", true);
        mIsFaceBeautyEnabled = getIntent().getBooleanExtra("beauty", true);
        setRequestedOrientation(mIsLandscape ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        AVOptions options = new AVOptions();
        options.setInteger(AVOptions.KEY_PREPARE_TIMEOUT, 10 * 1000);
        options.setInteger(AVOptions.KEY_GET_AV_FRAME_TIMEOUT, 10 * 1000);
        options.setInteger(AVOptions.KEY_LIVE_STREAMING, 1);
        options.setInteger(AVOptions.KEY_DELAY_OPTIMIZATION, 1);

        // 1 -> hw codec enable, 0 -> disable [recommended]
        options.setInteger(AVOptions.KEY_MEDIACODEC, 0);

        // whether start play automatically after prepared, default value is 1
        options.setInteger(AVOptions.KEY_START_ON_PREPARED, 0);

        mVideoView.setAVOptions(options);
        mVideoView.setDisplayAspectRatio(PLVideoView.ASPECT_RATIO_PAVED_PARENT);

        // Set some listeners
        mVideoView.setOnInfoListener(mOnInfoListener);
        mVideoView.setOnCompletionListener(mOnCompletionListener);
        mVideoView.setOnErrorListener(mOnErrorListener);

        mVideoView.setVideoPath(mVideoPath);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoView.start();
        mIsActivityPaused = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoView.pause();
        mIsActivityPaused = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoView.stopPlayback();
    }

    public void onClickExit(View v) {
        finish();
    }

    // 当用户点击连麦以后，建议向 App Server 申请向主播连麦，主播同意后，方可进入房间，完成连麦
    public void onClickConference(View v) {
        Toast.makeText(this, "申请连麦... 主播已同意 !", Toast.LENGTH_SHORT).show();
        if (mIsAudioOnly) {
            jumpToStreamingActivity(QiniuAppServer.RTC_ROLE_VICE_ANCHOR, RTCAudioStreamingActivity.class);
        } else if (mIsPKMode) {
            Intent intent = new Intent(this, PKViceAnchorActivity.class);
            intent.putExtra("roomName", mRoomName);
            intent.putExtra("beauty", mIsFaceBeautyEnabled);
            startActivity(intent);
        } else {
            if (!mIsExtCapture) {
                jumpToStreamingActivity(QiniuAppServer.RTC_ROLE_VICE_ANCHOR, RTCStreamingActivity.class);
            } else {
                jumpToStreamingActivity(QiniuAppServer.RTC_ROLE_VICE_ANCHOR, ExtCapStreamingActivity.class);
            }
        }
    }

    private void jumpToStreamingActivity(int role, Class<?> cls) {
        Intent intent = new Intent(this, cls);
        intent.putExtra("role", role);
        intent.putExtra("roomName", mRoomName);
        intent.putExtra("swcodec", mIsSWCodec);
        intent.putExtra("orientation", mIsLandscape);
        intent.putExtra("beauty", mIsFaceBeautyEnabled);
        intent.putExtra("PreviewSizeRatio", DEFAULT_PREVIEW_SIZE_RATIO);
        intent.putExtra("PreviewSizeLevel", DEFAULT_PREVIEW_SIZE_LEVEL);
        startActivity(intent);
    }

    private PLMediaPlayer.OnInfoListener mOnInfoListener = new PLMediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(PLMediaPlayer plMediaPlayer, int what, int extra) {
            Log.d(TAG, "onInfo: " + what + ", " + extra);
            return false;
        }
    };

    private PLMediaPlayer.OnErrorListener mOnErrorListener = new PLMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(PLMediaPlayer plMediaPlayer, int errorCode) {
            boolean isNeedReconnect = false;
            Log.e(TAG, "Error happened, errorCode = " + errorCode);
            switch (errorCode) {
                case PLMediaPlayer.ERROR_CODE_INVALID_URI:
                    showToastTips("Invalid URL !");
                    break;
                case PLMediaPlayer.ERROR_CODE_404_NOT_FOUND:
                    showToastTips("404 resource not found !");
                    break;
                case PLMediaPlayer.ERROR_CODE_CONNECTION_REFUSED:
                    showToastTips("Connection refused !");
                    break;
                case PLMediaPlayer.ERROR_CODE_CONNECTION_TIMEOUT:
                    showToastTips("Connection timeout !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.ERROR_CODE_EMPTY_PLAYLIST:
                    showToastTips("Empty playlist !");
                    break;
                case PLMediaPlayer.ERROR_CODE_STREAM_DISCONNECTED:
                    showToastTips("Stream disconnected !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.ERROR_CODE_IO_ERROR:
                    showToastTips("Network IO Error !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.ERROR_CODE_UNAUTHORIZED:
                    showToastTips("Unauthorized Error !");
                    break;
                case PLMediaPlayer.ERROR_CODE_PREPARE_TIMEOUT:
                    showToastTips("Prepare timeout !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.ERROR_CODE_READ_FRAME_TIMEOUT:
                    showToastTips("Read frame timeout !");
                    isNeedReconnect = true;
                    break;
                case PLMediaPlayer.MEDIA_ERROR_UNKNOWN:
                    break;
                default:
                    showToastTips("unknown error !");
                    break;
            }
            // Todo pls handle the error status here, reconnect or call finish()
            if (isNeedReconnect) {
                sendReconnectMessage();
            } else {
                finish();
            }
            // Return true means the error has been handled
            // If return false, then `onCompletion` will be called
            return true;
        }
    };

    private PLMediaPlayer.OnCompletionListener mOnCompletionListener = new PLMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(PLMediaPlayer plMediaPlayer) {
            Log.d(TAG, "Play Completed !");
            showToastTips("Play Completed !");
            finish();
        }
    };

    private void showToastTips(final String tips) {
        if (mIsActivityPaused) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mToast != null) {
                    mToast.cancel();
                }
                mToast = Toast.makeText(PlaybackActivity.this, tips, Toast.LENGTH_SHORT);
                mToast.show();
            }
        });
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what != MESSAGE_ID_RECONNECTING || mIsActivityPaused) {
                return;
            }
            if (!QiniuAppServer.isNetworkAvailable(PlaybackActivity.this)) {
                sendReconnectMessage();
                return;
            }
            mVideoView.setVideoPath(mVideoPath);
            mVideoView.start();
        }
    };

    private void sendReconnectMessage() {
        showToastTips("正在重连...");
        mLoadingView.setVisibility(View.VISIBLE);
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MESSAGE_ID_RECONNECTING), 500);
    }
}
