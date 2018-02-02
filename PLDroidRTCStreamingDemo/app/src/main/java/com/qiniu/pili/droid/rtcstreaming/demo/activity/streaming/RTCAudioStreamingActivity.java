package com.qiniu.pili.droid.rtcstreaming.demo.activity.streaming;

import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.qiniu.pili.droid.rtcstreaming.RTCAudioSource;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceOptions;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceState;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceStateChangedListener;
import com.qiniu.pili.droid.rtcstreaming.RTCMediaStreamingManager;
import com.qiniu.pili.droid.rtcstreaming.RTCStartConferenceCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCSurfaceView;
import com.qiniu.pili.droid.rtcstreaming.RTCUserEventListener;
import com.qiniu.pili.droid.rtcstreaming.RTCVideoWindow;
import com.qiniu.pili.droid.rtcstreaming.demo.R;
import com.qiniu.pili.droid.rtcstreaming.demo.core.QiniuAppServer;
import com.qiniu.pili.droid.streaming.AVCodecType;
import com.qiniu.pili.droid.streaming.MicrophoneStreamingSetting;
import com.qiniu.pili.droid.streaming.StreamStatusCallback;
import com.qiniu.pili.droid.streaming.StreamingProfile;
import com.qiniu.pili.droid.streaming.StreamingSessionListener;
import com.qiniu.pili.droid.streaming.StreamingState;
import com.qiniu.pili.droid.streaming.StreamingStateChangedListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.List;

/**
 *  演示使用 SDK 内部的 Audio 采集，实现纯音频连麦 & 推流
 */
public class RTCAudioStreamingActivity extends AppCompatActivity {

    private static final String TAG = "RTCStreamingActivity";
    private static final int MESSAGE_ID_RECONNECTING = 0x01;

    private TextView mStatusTextView;
    private TextView mStatTextView;
    private Button mControlButton;
    private CheckBox mMuteCheckBox;
    private CheckBox mConferenceCheckBox;

    private Toast mToast = null;
    private ProgressDialog mProgressDialog;

    private RTCMediaStreamingManager mRTCStreamingManager;

    private StreamingProfile mStreamingProfile;

    private boolean mIsActivityPaused = true;
    private boolean mIsPublishStreamStarted = false;
    private boolean mIsConferenceStarted = false;
    private boolean mIsInReadyState = false;

    private int mRole;
    private String mRoomName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_audio_streaming);

        mRole = getIntent().getIntExtra("role", QiniuAppServer.RTC_ROLE_VICE_ANCHOR);
        mRoomName = getIntent().getStringExtra("roomName");
        boolean isSwCodec = getIntent().getBooleanExtra("swcodec", true);
        boolean isLandscape = getIntent().getBooleanExtra("orientation", false);
        setRequestedOrientation(isLandscape ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mControlButton = (Button) findViewById(R.id.ControlButton);
        mStatusTextView = (TextView) findViewById(R.id.StatusTextView);
        mStatTextView = (TextView) findViewById(R.id.StatTextView);
        mMuteCheckBox = (CheckBox) findViewById(R.id.MuteCheckBox);
        mMuteCheckBox.setOnClickListener(mMuteButtonClickListener);
        mConferenceCheckBox = (CheckBox) findViewById(R.id.ConferenceCheckBox);
        mConferenceCheckBox.setOnClickListener(mConferenceButtonClickListener);

        if (mRole == QiniuAppServer.RTC_ROLE_ANCHOR) {
            mConferenceCheckBox.setVisibility(View.VISIBLE);
        }

        AVCodecType codecType = isSwCodec ? AVCodecType.SW_AUDIO_CODEC : AVCodecType.HW_AUDIO_CODEC;
        mRTCStreamingManager = new RTCMediaStreamingManager(getApplicationContext(), codecType);
        mRTCStreamingManager.setConferenceStateListener(mRTCStreamingStateChangedListener);
        mRTCStreamingManager.setUserEventListener(mRTCUserEventListener);
        mRTCStreamingManager.setDebugLoggingEnabled(false);

        RTCConferenceOptions options = new RTCConferenceOptions();
        options.setHWCodecEnabled(!isSwCodec);
        mRTCStreamingManager.setConferenceOptions(options);

        MicrophoneStreamingSetting setting = new MicrophoneStreamingSetting();
        setting.setBluetoothSCOEnabled(false);
        setting.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);

        // the anchor must configure the `StreamingProfile`
        if (mRole == QiniuAppServer.RTC_ROLE_ANCHOR) {
            mRTCStreamingManager.setStreamStatusCallback(mStreamStatusCallback);
            mRTCStreamingManager.setStreamingStateListener(mStreamingStateChangedListener);
            mRTCStreamingManager.setStreamingSessionListener(mStreamingSessionListener);

            mStreamingProfile = new StreamingProfile();
            mStreamingProfile.setAudioQuality(StreamingProfile.AUDIO_QUALITY_MEDIUM2)
                    .setEncoderRCMode(StreamingProfile.EncoderRCModes.QUALITY_PRIORITY);
            mRTCStreamingManager.prepare(setting,  mStreamingProfile);
        } else {
            /**
             * The RTCVideoWindow is used to show the anchor's video
             * This code is not required when the anchor is publishing audio streaming only.
             */
            RTCVideoWindow remoteAnchorView = new RTCVideoWindow((RTCSurfaceView) findViewById(R.id.RemoteAnchorView));
            mRTCStreamingManager.addRemoteWindow(remoteAnchorView);

            mControlButton.setText("开始连麦");
            mRTCStreamingManager.prepare(setting);
        }

        mProgressDialog = new ProgressDialog(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsActivityPaused = false;
        mRTCStreamingManager.startCapture();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsActivityPaused = true;
        stopConference();
        mRTCStreamingManager.stopCapture();
        stopPublishStreaming();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRTCStreamingManager.destroy();
    }

    public void onClickExit(View v) {
        finish();
    }

    private boolean startConference() {
        if (!QiniuAppServer.isNetworkAvailable(this)) {
            Toast.makeText(RTCAudioStreamingActivity.this, "network is unavailable!!!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (mIsConferenceStarted) {
            return true;
        }
        mProgressDialog.setMessage("正在加入连麦 ... ");
        mProgressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                startConferenceInternal();
            }
        }).start();
        return true;
    }

    private boolean startConferenceInternal() {
        String roomToken = QiniuAppServer.getInstance().requestRoomToken(QiniuAppServer.getTestUserId(this), mRoomName);
        if (roomToken == null) {
            dismissProgressDialog();
            showToast("无法获取房间信息 !", Toast.LENGTH_SHORT);
            return false;
        }
        mRTCStreamingManager.startConference(QiniuAppServer.getTestUserId(this), mRoomName, roomToken, new RTCStartConferenceCallback() {
            @Override
            public void onStartConferenceSuccess() {
                dismissProgressDialog();
                showToast(getString(R.string.start_conference), Toast.LENGTH_SHORT);
                updateControlButtonText();
                mIsConferenceStarted = true;
                /**
                 * Because `startConference` is called in child thread
                 * So we should check if the activity paused.
                 */
                if (mIsActivityPaused) {
                    stopConference();
                }
            }

            @Override
            public void onStartConferenceFailed(int errorCode) {
                setConferenceBoxChecked(false);
                dismissProgressDialog();
                showToast(getString(R.string.failed_to_start_conference) + errorCode, Toast.LENGTH_SHORT);
            }
        });
        return true;
    }

    private boolean stopConference() {
        if (!mIsConferenceStarted) {
            return true;
        }
        mRTCStreamingManager.stopConference();
        mIsConferenceStarted = false;
        setConferenceBoxChecked(false);
        showToast(getString(R.string.stop_conference), Toast.LENGTH_SHORT);
        updateControlButtonText();
        return true;
    }

    private boolean startPublishStreaming() {
        if (!QiniuAppServer.isNetworkAvailable(this)) {
            Toast.makeText(RTCAudioStreamingActivity.this, "network is unavailable!!!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (mIsPublishStreamStarted) {
            return true;
        }
        if (!mIsInReadyState) {
            showToast(getString(R.string.stream_state_not_ready), Toast.LENGTH_SHORT);
            return false;
        }
        mProgressDialog.setMessage("正在准备推流... ");
        mProgressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                startPublishStreamingInternal();
            }
        }).start();
        return true;
    }

    private boolean startPublishStreamingInternal() {
        String publishAddr = QiniuAppServer.getInstance().requestPublishAddress(mRoomName);
        if (publishAddr == null) {
            dismissProgressDialog();
            showToast("无法获取房间信息/推流地址 !", Toast.LENGTH_SHORT);
            return false;
        }

        try {
            mStreamingProfile.setPublishUrl(publishAddr);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            dismissProgressDialog();
            showToast("无效的推流地址 !", Toast.LENGTH_SHORT);
            return false;
        }

        mRTCStreamingManager.setStreamingProfile(mStreamingProfile);
        if (!mRTCStreamingManager.startStreaming()) {
            dismissProgressDialog();
            showToast(getString(R.string.failed_to_start_streaming), Toast.LENGTH_SHORT);
            return false;
        }
        dismissProgressDialog();
        showToast(getString(R.string.start_streaming), Toast.LENGTH_SHORT);
        updateControlButtonText();
        mIsPublishStreamStarted = true;
        /**
         * Because `startPublishStreaming` need a long time in some weak network
         * So we should check if the activity paused.
         */
        if (mIsActivityPaused) {
            stopPublishStreaming();
        }
        return true;
    }

    private boolean stopPublishStreaming() {
        if (!mIsPublishStreamStarted) {
            return true;
        }
        mRTCStreamingManager.stopStreaming();
        mIsPublishStreamStarted = false;
        showToast(getString(R.string.stop_streaming), Toast.LENGTH_SHORT);
        updateControlButtonText();
        return false;
    }

    private StreamingStateChangedListener mStreamingStateChangedListener = new StreamingStateChangedListener() {
        @Override
        public void onStateChanged(final StreamingState state, Object o) {
            switch (state) {
                case PREPARING:
                    setStatusText(getString(R.string.preparing));
                    Log.d(TAG, "onStateChanged state:" + "preparing");
                    break;
                case READY:
                    mIsInReadyState = true;
                    setStatusText(getString(R.string.ready));
                    Log.d(TAG, "onStateChanged state:" + "ready");
                    break;
                case CONNECTING:
                    Log.d(TAG, "onStateChanged state:" + "connecting");
                    break;
                case STREAMING:
                    setStatusText(getString(R.string.streaming));
                    Log.d(TAG, "onStateChanged state:" + "streaming");
                    break;
                case SHUTDOWN:
                    mIsInReadyState = true;
                    setStatusText(getString(R.string.ready));
                    Log.d(TAG, "onStateChanged state:" + "shutdown");
                    break;
                case UNKNOWN:
                    Log.d(TAG, "onStateChanged state:" + "unknown");
                    break;
                case SENDING_BUFFER_EMPTY:
                    Log.d(TAG, "onStateChanged state:" + "sending buffer empty");
                    break;
                case SENDING_BUFFER_FULL:
                    Log.d(TAG, "onStateChanged state:" + "sending buffer full");
                    break;
                case AUDIO_RECORDING_FAIL:
                    Log.d(TAG, "onStateChanged state:" + "audio recording failed");
                    showToast(getString(R.string.failed_open_microphone), Toast.LENGTH_SHORT);
                    break;
                case IOERROR:
                    /**
                     * Network-connection is unavailable when `startStreaming`.
                     * You can do reconnecting or just finish the streaming
                     */
                    Log.d(TAG, "onStateChanged state:" + "io error");
                    showToast(getString(R.string.io_error), Toast.LENGTH_SHORT);
                    sendReconnectMessage();
                    // stopPublishStreaming();
                    break;
                case DISCONNECTED:
                    /**
                     * Network-connection is broken after `startStreaming`.
                     * You can do reconnecting in `onRestartStreamingHandled`
                     */
                    Log.d(TAG, "onStateChanged state:" + "disconnected");
                    setStatusText(getString(R.string.disconnected));
                    // we will process this state in `onRestartStreamingHandled`
                    break;
            }
        }
    };

    private StreamingSessionListener mStreamingSessionListener = new StreamingSessionListener() {
        @Override
        public boolean onRecordAudioFailedHandled(int code) {
            return false;
        }

        /**
         * When the network-connection is broken, StreamingState#DISCONNECTED will notified first,
         * and then invoked this method if the environment of restart streaming is ready.
         *
         * @return true means you handled the event; otherwise, given up and then StreamingState#SHUTDOWN
         * will be notified.
         */
        @Override
        public boolean onRestartStreamingHandled(int code) {
            Log.d(TAG, "onRestartStreamingHandled, reconnect ...");
            return mRTCStreamingManager.startStreaming();
        }

        @Override
        public Camera.Size onPreviewSizeSelected(List<Camera.Size> list) {
            return null;
        }

        @Override
        public int onPreviewFpsSelected(List<int[]> list) {
            return -1;
        }
    };

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what != MESSAGE_ID_RECONNECTING || mIsActivityPaused || !mIsPublishStreamStarted) {
                return;
            }
            if (!QiniuAppServer.isNetworkAvailable(RTCAudioStreamingActivity.this)) {
                sendReconnectMessage();
                return;
            }
            Log.d(TAG, "do reconnecting ...");
            mRTCStreamingManager.startStreaming();
        }
    };

    private void sendReconnectMessage() {
        showToast("正在重连...", Toast.LENGTH_SHORT);
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MESSAGE_ID_RECONNECTING), 500);
    }

    private RTCConferenceStateChangedListener mRTCStreamingStateChangedListener = new RTCConferenceStateChangedListener() {
        @Override
        public void onConferenceStateChanged(RTCConferenceState state, int extra) {
            switch (state) {
                case READY:
                    // You must `StartConference` after `Ready`
                    showToast(getString(R.string.ready), Toast.LENGTH_SHORT);
                    break;
                case RECONNECTING:
                    showToast(getString(R.string.reconnecting), Toast.LENGTH_SHORT);
                    break;
                case RECONNECTED:
                    showToast(getString(R.string.reconnected), Toast.LENGTH_SHORT);
                    break;
                case RECONNECT_FAIL:
                    showToast(getString(R.string.reconnect_failed), Toast.LENGTH_SHORT);
                    break;
                case AUDIO_PUBLISH_FAILED:
                    showToast(getString(R.string.failed_to_publish_av_to_rtc) + extra, Toast.LENGTH_SHORT);
                    finish();
                    break;
                case AUDIO_PUBLISH_SUCCESS:
                    showToast(getString(R.string.success_publish_audio_to_rtc), Toast.LENGTH_SHORT);
                    break;
                case USER_JOINED_AGAIN:
                    showToast(getString(R.string.user_join_other_where), Toast.LENGTH_SHORT);
                    finish();
                    break;
                case USER_KICKOUT_BY_HOST:
                    showToast(getString(R.string.user_kickout_by_host), Toast.LENGTH_SHORT);
                    finish();
                    break;
                case AUDIO_RECORDING_FAIL:
                    showToast(getString(R.string.failed_open_microphone), Toast.LENGTH_SHORT);
                    break;
                default:
                    return;
            }
        }
    };

    private RTCUserEventListener mRTCUserEventListener = new RTCUserEventListener() {
        @Override
        public void onUserJoinConference(String remoteUserId) {
            Log.d(TAG, "onUserJoinConference: " + remoteUserId);
            showToast(getString(R.string.user_join_conference), Toast.LENGTH_SHORT);
        }

        @Override
        public void onUserLeaveConference(String remoteUserId) {
            Log.d(TAG, "onUserLeaveConference: " + remoteUserId);
            showToast(getString(R.string.user_leave_conference), Toast.LENGTH_SHORT);
        }
    };

    private View.OnClickListener mMuteButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mIsConferenceStarted && !mIsPublishStreamStarted) {
                mMuteCheckBox.setChecked(!mMuteCheckBox.isChecked());
                showToast(getString(R.string.mute_must_in_streaming_status), Toast.LENGTH_SHORT);
                return;
            }
            if (mMuteCheckBox.isChecked()) {
                mRTCStreamingManager.mute(RTCAudioSource.MIC);
            } else {
                mRTCStreamingManager.unMute(RTCAudioSource.MIC);
            }
        }
    };

    private View.OnClickListener mConferenceButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mConferenceCheckBox.isChecked()) {
                startConference();
            } else {
                stopConference();
            }
        }
    };
    
    public void onClickStreaming(View v) {
        if (mRole == QiniuAppServer.RTC_ROLE_ANCHOR) {
            if (!mIsPublishStreamStarted) {
                startPublishStreaming();
            } else {
                stopPublishStreaming();
            }
        } else {
            if (!mIsConferenceStarted) {
                startConference();
            } else {
                stopConference();
            }
        }
    }

    private void setStatusText(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStatusTextView.setText(status);
            }
        });
    }

    private void updateControlButtonText() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mRole == QiniuAppServer.RTC_ROLE_ANCHOR) {
                    if (mIsPublishStreamStarted) {
                        mControlButton.setText(getString(R.string.stop_streaming));
                    } else {
                        mControlButton.setText(getString(R.string.start_streaming));
                    }
                } else {
                    if (mIsConferenceStarted) {
                        mControlButton.setText(getString(R.string.stop_conference));
                    } else {
                        mControlButton.setText(getString(R.string.start_conference));
                    }
                }
            }
        });
    }

    private void setConferenceBoxChecked(final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConferenceCheckBox.setChecked(enabled);
            }
        });
    }

    private StreamStatusCallback mStreamStatusCallback = new StreamStatusCallback() {
        @Override
        public void notifyStreamStatusChanged(final StreamingProfile.StreamStatus streamStatus) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String stat = "Bitrate: " + streamStatus.totalAVBitrate / 1024 + " kbps"
                            + "\nAudio: " + streamStatus.audioFps + " fps";
                    mStatTextView.setText(stat);
                }
            });
        }
    };

    private void dismissProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.dismiss();
            }
        });
    }

    private void showToast(final String text, final int duration) {
        if (mIsActivityPaused) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mToast != null) {
                    mToast.cancel();
                }
                mToast = Toast.makeText(RTCAudioStreamingActivity.this, text, duration);
                mToast.show();
            }
        });
    }
}
