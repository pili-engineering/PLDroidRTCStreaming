package com.qiniu.pili.droid.rtcstreaming.demo.activity.streaming;

import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.qiniu.pili.droid.rtcstreaming.RTCAudioSource;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceOptions;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceState;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceStateChangedListener;
import com.qiniu.pili.droid.rtcstreaming.RTCMediaStreamingManager;
import com.qiniu.pili.droid.rtcstreaming.RTCRemoteWindowEventListener;
import com.qiniu.pili.droid.rtcstreaming.RTCStartConferenceCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCSurfaceView;
import com.qiniu.pili.droid.rtcstreaming.RTCVideoWindow;
import com.qiniu.pili.droid.rtcstreaming.demo.R;
import com.qiniu.pili.droid.rtcstreaming.demo.core.QiniuAppServer;
import com.qiniu.pili.droid.streaming.AVCodecType;
import com.qiniu.pili.droid.streaming.CameraStreamingSetting;
import com.qiniu.pili.droid.streaming.MicrophoneStreamingSetting;
import com.qiniu.pili.droid.streaming.StreamingSessionListener;

import java.util.List;

/**
 *  演示横屏 PK 模式下的副主播端代码
 *  PK 模式只支持一个主播和一个副主播进行连麦，副主播的布局配置方法如下：
 *  左右两个 GLSurfaceView，左边显示主播的画面，右边显示副主播的预览
 */
public class PKViceAnchorActivity extends AppCompatActivity {
    private static final String TAG = "PKViceAnchorActivity";

    private Button mControlButton;
    private CheckBox mMuteCheckBox;

    private ProgressDialog mProgressDialog;

    private RTCMediaStreamingManager mRTCStreamingManager;

    private boolean mIsActivityPaused = true;
    private boolean mIsConferenceStarted = false;
    private int mCurrentCamFacingIndex;

    private String mRoomName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_pk_vice_anchor);

        /**
         * Step 1: find & init views
         */
        GLSurfaceView cameraPreviewFrameView = (GLSurfaceView) findViewById(R.id.cameraPreview_surfaceView);

        boolean isSwCodec = getIntent().getBooleanExtra("swcodec", true);
        mRoomName = getIntent().getStringExtra("roomName");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        boolean isBeautyEnabled = getIntent().getBooleanExtra("beauty", false);
        boolean isDebugModeEnabled = getIntent().getBooleanExtra("debugMode", false);

        mControlButton = (Button) findViewById(R.id.ControlButton);
        mMuteCheckBox = (CheckBox) findViewById(R.id.MuteCheckBox);
        mMuteCheckBox.setOnClickListener(mMuteButtonClickListener);
        mControlButton.setText("开始连麦");

        CameraStreamingSetting.CAMERA_FACING_ID facingId = chooseCameraFacingId();
        mCurrentCamFacingIndex = facingId.ordinal();

        /**
         * Step 2: config camera & microphone settings
         */
        CameraStreamingSetting cameraStreamingSetting = new CameraStreamingSetting();
        cameraStreamingSetting.setCameraFacingId(facingId)
                .setContinuousFocusModeEnabled(true)
                .setRecordingHint(false)
                .setResetTouchFocusDelayInMs(3000)
                .setFocusMode(CameraStreamingSetting.FOCUS_MODE_CONTINUOUS_PICTURE)
                .setCameraPrvSizeLevel(CameraStreamingSetting.PREVIEW_SIZE_LEVEL.MEDIUM)
                .setCameraPrvSizeRatio(CameraStreamingSetting.PREVIEW_SIZE_RATIO.RATIO_4_3);

        if (isBeautyEnabled) {
            cameraStreamingSetting.setBuiltInFaceBeautyEnabled(true); // Using sdk built in face beauty algorithm
            cameraStreamingSetting.setFaceBeautySetting(new CameraStreamingSetting.FaceBeautySetting(0.8f, 0.8f, 0.6f)); // sdk built in face beauty settings
            cameraStreamingSetting.setVideoFilter(CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_BEAUTY); // set the beauty on/off
        }

        MicrophoneStreamingSetting microphoneStreamingSetting = new MicrophoneStreamingSetting();
        microphoneStreamingSetting.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);

        /**
         * Step 3: create streaming manager and set listeners
         */
        AVCodecType codecType = isSwCodec ? AVCodecType.SW_VIDEO_WITH_SW_AUDIO_CODEC : AVCodecType.HW_VIDEO_YUV_AS_INPUT_WITH_HW_AUDIO_CODEC;
        mRTCStreamingManager = new RTCMediaStreamingManager(getApplicationContext(), cameraPreviewFrameView, codecType);
        mRTCStreamingManager.setConferenceStateListener(mRTCStreamingStateChangedListener);
        mRTCStreamingManager.setRemoteWindowEventListener(mRTCRemoteWindowEventListener);
        mRTCStreamingManager.setStreamingSessionListener(mStreamingSessionListener);
        mRTCStreamingManager.setDebugLoggingEnabled(isDebugModeEnabled);

        /**
         * Step 4: set conference options
         */
        RTCConferenceOptions options = new RTCConferenceOptions();
        // RATIO_4_3 & VIDEO_ENCODING_SIZE_HEIGHT_240 means the output size is 640 x 480
        options.setVideoEncodingSizeRatio(RTCConferenceOptions.VIDEO_ENCODING_SIZE_RATIO.RATIO_4_3);
        options.setVideoEncodingSizeLevel(RTCConferenceOptions.VIDEO_ENCODING_SIZE_HEIGHT_480);
        options.setVideoEncodingOrientation(RTCConferenceOptions.VIDEO_ENCODING_ORIENTATION.LAND);
        options.setVideoBitrateRange(300 * 1024, 800 * 1024);
        // 15 fps is enough
        options.setVideoEncodingFps(15);
        mRTCStreamingManager.setConferenceOptions(options);

        /**
         * Step 5: create the remote windows
         */
        RTCVideoWindow windowA = new RTCVideoWindow((RTCSurfaceView) findViewById(R.id.RemoteGLSurfaceViewA));

        /**
         * Step 6: add the remote windows
         */
        mRTCStreamingManager.addRemoteWindow(windowA);

        /**
         * Step 7: do prepare
         */
        mRTCStreamingManager.prepare(cameraStreamingSetting, microphoneStreamingSetting, null, null);

        mProgressDialog = new ProgressDialog(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsActivityPaused = false;
        /**
         * Step 8: You must start capture before conference or streaming
         * You will receive `Ready` state callback when capture started success
         */
        mRTCStreamingManager.startCapture();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsActivityPaused = true;
        /**
         * Step 9: You must stop capture, stop conference, stop streaming when activity paused
         */
        mRTCStreamingManager.stopCapture();
        stopConference();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /**
         * Step 10: You must call destroy to release some resources when activity destroyed
         */
        mRTCStreamingManager.destroy();
    }

    public void onClickSwitchCamera(View v) {
        mCurrentCamFacingIndex = (mCurrentCamFacingIndex + 1) % CameraStreamingSetting.getNumberOfCameras();
        CameraStreamingSetting.CAMERA_FACING_ID facingId;
        if (mCurrentCamFacingIndex == CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_BACK.ordinal()) {
            facingId = CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_BACK;
        } else if (mCurrentCamFacingIndex == CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT.ordinal()) {
            facingId = CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT;
        } else {
            facingId = CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_3RD;
        }
        Log.i(TAG, "switchCamera:" + facingId);
        mRTCStreamingManager.switchCamera(facingId);
    }

    public void onClickExit(View v) {
        finish();
    }

    private boolean startConference() {
        if (!QiniuAppServer.isNetworkAvailable(this)) {
            Toast.makeText(PKViceAnchorActivity.this, "network is unavailable!!!", Toast.LENGTH_SHORT).show();
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
        showToast(getString(R.string.stop_conference), Toast.LENGTH_SHORT);
        updateControlButtonText();
        return true;
    }

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
            return false;
        }

        @Override
        public Camera.Size onPreviewSizeSelected(List<Camera.Size> list) {
            for (Camera.Size size : list) {
                if (size.height >= 480) {
                    return size;
                }
            }
            return null;
        }

        @Override
        public int onPreviewFpsSelected(List<int[]> list) {
            return -1;
        }
    };

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
                case VIDEO_PUBLISH_FAILED:
                case AUDIO_PUBLISH_FAILED:
                    showToast(getString(R.string.failed_to_publish_av_to_rtc) + extra, Toast.LENGTH_SHORT);
                    finish();
                    break;
                case VIDEO_PUBLISH_SUCCESS:
                    showToast(getString(R.string.success_publish_video_to_rtc), Toast.LENGTH_SHORT);
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
                case OPEN_CAMERA_FAIL:
                    showToast(getString(R.string.failed_open_camera), Toast.LENGTH_SHORT);
                    break;
                case AUDIO_RECORDING_FAIL:
                    showToast(getString(R.string.failed_open_microphone), Toast.LENGTH_SHORT);
                    break;
                default:
                    return;
            }
        }
    };

    private RTCRemoteWindowEventListener mRTCRemoteWindowEventListener = new RTCRemoteWindowEventListener() {
        @Override
        public void onRemoteWindowAttached(RTCVideoWindow window, String remoteUserId) {
            Log.d(TAG, "onRemoteWindowAttached: " + remoteUserId);
        }

        @Override
        public void onRemoteWindowDetached(RTCVideoWindow window, String remoteUserId) {
            Log.d(TAG, "onRemoteWindowDetached: " + remoteUserId);
        }

        @Override
        public void onFirstRemoteFrameArrived(String remoteUserId) {
            Log.d(TAG, "onFirstRemoteFrameArrived: " + remoteUserId);
        }
    };

    private View.OnClickListener mMuteButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mMuteCheckBox.isChecked()) {
                mRTCStreamingManager.mute(RTCAudioSource.MIC);
            } else {
                mRTCStreamingManager.unMute(RTCAudioSource.MIC);
            }
        }
    };
    
    public void onClickStreaming(View v) {
        if (!mIsConferenceStarted) {
            startConference();
        } else {
            stopConference();
        }
    }

    private void updateControlButtonText() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mIsConferenceStarted) {
                    mControlButton.setText(getString(R.string.stop_conference));
                } else {
                    mControlButton.setText(getString(R.string.start_conference));
                }
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

    private void showToast(final String text, final int duration) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(this, text, duration).show();
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PKViceAnchorActivity.this, text, duration).show();
            }
        });
    }

    private CameraStreamingSetting.CAMERA_FACING_ID chooseCameraFacingId() {
        if (CameraStreamingSetting.hasCameraFacing(CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_3RD)) {
            return CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_3RD;
        } else if (CameraStreamingSetting.hasCameraFacing(CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT)) {
            return CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT;
        } else {
            return CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_BACK;
        }
    }
}
