package com.qiniu.pili.droid.rtcstreaming.demo.activity.conference;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.qiniu.pili.droid.rtcstreaming.RTCAudioLevelCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCAudioSource;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceManager;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceOptions;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceState;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceStateChangedListener;
import com.qiniu.pili.droid.rtcstreaming.RTCFrameCapturedCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCMediaSubscribeCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCRemoteWindowEventListener;
import com.qiniu.pili.droid.rtcstreaming.RTCStartConferenceCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCStreamStatsCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCSurfaceView;
import com.qiniu.pili.droid.rtcstreaming.RTCUserEventListener;
import com.qiniu.pili.droid.rtcstreaming.RTCVideoWindow;
import com.qiniu.pili.droid.rtcstreaming.demo.R;
import com.qiniu.pili.droid.rtcstreaming.demo.core.QiniuAppServer;
import com.qiniu.pili.droid.rtcstreaming.demo.ui.CameraPreviewFrameView;
import com.qiniu.pili.droid.rtcstreaming.demo.ui.RotateLayout;
import com.qiniu.pili.droid.streaming.AVCodecType;
import com.qiniu.pili.droid.streaming.CameraStreamingSetting;
import com.qiniu.pili.droid.streaming.MicrophoneStreamingSetting;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 *  演示使用 SDK 互动专版 API，不带推流，但有更加丰富的接口和回调
 */
public class ConferenceActivity extends AppCompatActivity {

    private static final String TAG = "ConferenceActivity";

    private Button mControlButton;
    private CheckBox mMuteCheckBox;

    private FloatingActionButton mMuteSpeakerButton;
    private FloatingActionButton mVideoCaptureButton;
    private FloatingActionButton mAudioCaptureButton;
    private FloatingActionButton mVideoPublishButton;
    private FloatingActionButton mAudioPublishButton;

    private ProgressDialog mProgressDialog;

    private RTCConferenceManager mRTCConferenceManager;

    private int mCurrentCamFacingIndex;

    private CameraPreviewFrameView mCameraPreviewFrameView;
    private RotateLayout mRotateLayout;
    private int mCurrentZoom = 0;
    private int mMaxZoom = 0;

    private RTCVideoWindow mRTCVideoWindowA;
    private RTCVideoWindow mRTCVideoWindowB;

    private String mRoomName;

    private boolean mIsSpeakerMuted = false;
    private boolean mIsAudioLevelCallbackEnabled = false;

    private boolean mIsVideoCaptureStarted = false;
    private boolean mIsAudioCaptureStarted = false;
    private boolean mIsVideoPublishStarted = false;
    private boolean mIsAudioPublishStarted = false;
    private boolean mIsInReadyState = false;

    private boolean mIsPreviewOnTop = false;
    private boolean mIsWindowAOnBottom = false;

    private String mUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_conference);

        /**
         * Step 1: find & init views
         */
        mCameraPreviewFrameView = (CameraPreviewFrameView) findViewById(R.id.cameraPreview_surfaceView);
        mCameraPreviewFrameView.setListener(mCameraPreviewListener);

        mRoomName = getIntent().getStringExtra("roomName");

        boolean isSwCodec = getIntent().getBooleanExtra("swcodec", true);
        boolean isLandscape = getIntent().getBooleanExtra("orientation", false);
        setRequestedOrientation(isLandscape ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        boolean isBeautyEnabled = getIntent().getBooleanExtra("beauty", false);
        boolean isDebugModeEnabled = getIntent().getBooleanExtra("debugMode", false);
        boolean isStatsEnabled = getIntent().getBooleanExtra("enableStats", false);
        mIsAudioLevelCallbackEnabled = getIntent().getBooleanExtra("audioLevelCallback", false);

        mControlButton = (Button) findViewById(R.id.ControlButton);
        mMuteCheckBox = (CheckBox) findViewById(R.id.MuteCheckBox);
        mMuteCheckBox.setOnClickListener(mMuteButtonClickListener);

        mMuteSpeakerButton = (FloatingActionButton) findViewById(R.id.MuteSpeakerBtn);
        mVideoCaptureButton = (FloatingActionButton) findViewById(R.id.VideoCaptureBtn);
        mAudioCaptureButton = (FloatingActionButton) findViewById(R.id.AudioCaptureBtn);
        mVideoPublishButton = (FloatingActionButton) findViewById(R.id.VideoPublishBtn);
        mAudioPublishButton = (FloatingActionButton) findViewById(R.id.AudioPublishBtn);

        /**
         * Step 2: config camera & microphone settings
         */
        CameraStreamingSetting.CAMERA_FACING_ID facingId = chooseCameraFacingId();
        mCurrentCamFacingIndex = facingId.ordinal();

        CameraStreamingSetting cameraStreamingSetting = new CameraStreamingSetting();
        cameraStreamingSetting.setCameraFacingId(facingId)
                .setContinuousFocusModeEnabled(true)
                .setRecordingHint(false)
                .setResetTouchFocusDelayInMs(3000)
                .setFocusMode(CameraStreamingSetting.FOCUS_MODE_CONTINUOUS_PICTURE)
                .setCameraPrvSizeLevel(CameraStreamingSetting.PREVIEW_SIZE_LEVEL.MEDIUM)
                .setCameraPrvSizeRatio(CameraStreamingSetting.PREVIEW_SIZE_RATIO.RATIO_16_9);

        MicrophoneStreamingSetting microphoneStreamingSetting = new MicrophoneStreamingSetting();
        microphoneStreamingSetting.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);

        if (isBeautyEnabled) {
            cameraStreamingSetting.setBuiltInFaceBeautyEnabled(true); // Using sdk built in face beauty algorithm
            cameraStreamingSetting.setFaceBeautySetting(new CameraStreamingSetting.FaceBeautySetting(0.8f, 0.8f, 0.6f)); // sdk built in face beauty settings
            cameraStreamingSetting.setVideoFilter(CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_BEAUTY); // set the beauty on/off
        }

        /**
         * Step 3: create streaming manager and set listeners
         */
        AVCodecType codecType = isSwCodec ? AVCodecType.SW_VIDEO_WITH_SW_AUDIO_CODEC : AVCodecType.HW_VIDEO_YUV_AS_INPUT_WITH_HW_AUDIO_CODEC;
        mRTCConferenceManager = new RTCConferenceManager(getApplicationContext(), mCameraPreviewFrameView, codecType);
        mRTCConferenceManager.setConferenceStateListener(mRTCStreamingStateChangedListener);
        mRTCConferenceManager.setRemoteWindowEventListener(mRTCRemoteWindowEventListener);
        mRTCConferenceManager.setUserEventListener(mRTCUserEventListener);
        mRTCConferenceManager.setDebugLoggingEnabled(isDebugModeEnabled);
        mRTCConferenceManager.setMediaSubscribeCallback(mRTCMediaSubscribeCallback);

        if (mIsAudioLevelCallbackEnabled) {
            mRTCConferenceManager.setAudioLevelCallback(mRTCAudioLevelCallback);
        }

        /**
         * Step 4: set conference options
         */
        RTCConferenceOptions options = new RTCConferenceOptions();
        options.setVideoEncodingSizeRatio(RTCConferenceOptions.VIDEO_ENCODING_SIZE_RATIO.RATIO_16_9);
        options.setVideoEncodingSizeLevel(RTCConferenceOptions.VIDEO_ENCODING_SIZE_HEIGHT_480);
        options.setVideoBitrateRange(800 * 1024, 1024 * 1024);
        options.setVideoEncodingFps(15);
        options.setHWCodecEnabled(!isSwCodec);
        if (isStatsEnabled) {
            options.setStreamStatsInterval(500);
            mRTCConferenceManager.setRTCStreamStatsCallback(mRTCStreamStatsCallback);
        }
        mRTCConferenceManager.setConferenceOptions(options);

        /**
         * Step 5: create the remote windows, must add enough windows for remote users
         */
        RTCVideoWindow windowA = new RTCVideoWindow(findViewById(R.id.RemoteWindowA), (RTCSurfaceView) findViewById(R.id.RemoteGLSurfaceViewA));
        RTCVideoWindow windowB = new RTCVideoWindow(findViewById(R.id.RemoteWindowB), (RTCSurfaceView) findViewById(R.id.RemoteGLSurfaceViewB));

        /**
         * Step 6: add the remote windows
         */
        mRTCConferenceManager.addRemoteWindow(windowA);
        mRTCConferenceManager.addRemoteWindow(windowB);

        mRTCVideoWindowA = windowA;
        mRTCVideoWindowB = windowB;

        /**
         * Step 7: do prepare
         */
        mRTCConferenceManager.prepare(cameraStreamingSetting, microphoneStreamingSetting);

        mProgressDialog = new ProgressDialog(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRTCConferenceManager.startVideoCapture();
        mIsVideoCaptureStarted = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRTCConferenceManager.stopVideoCapture();
        mIsVideoCaptureStarted = false;
        mIsInReadyState = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /**
         * Step 8: You must call destroy to release some resources when activity destroyed
         */
        mRTCConferenceManager.destroy();
    }

    public void onClickExit(View v) {
        finish();
    }

    private boolean startConference() {
        if (!QiniuAppServer.isNetworkAvailable(this)) {
            Toast.makeText(ConferenceActivity.this, "network is unavailable!!!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (mRTCConferenceManager.isConferenceStarted()) {
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

        mUserID = QiniuAppServer.getTestUserId(this);
        mRTCConferenceManager.startConference(mUserID, mRoomName, roomToken, new RTCStartConferenceCallback() {
            @Override
            public void onStartConferenceSuccess() {
                dismissProgressDialog();
                showToast(getString(R.string.start_conference), Toast.LENGTH_SHORT);
                updateControlButtonText();
                mRTCConferenceManager.setAudioLevelMonitorEnabled(mIsAudioLevelCallbackEnabled);
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
        if (!mRTCConferenceManager.isConferenceStarted()) {
            return true;
        }
        mRTCConferenceManager.stopConference();
        showToast(getString(R.string.stop_conference), Toast.LENGTH_SHORT);
        updateControlButtonText();
        return true;
    }

    public void onClickRemoteWindowA(View v) {
        if (!mIsPreviewOnTop) {
            mRTCConferenceManager.switchRenderView(mRTCVideoWindowA.getRTCSurfaceView(), mCameraPreviewFrameView);
            mIsPreviewOnTop = true;
            mIsWindowAOnBottom = true;
        }
    }

    public void onClickRemoteWindowB(View v) {
        if (!mIsPreviewOnTop) {
            mRTCConferenceManager.switchRenderView(mRTCVideoWindowB.getRTCSurfaceView(), mCameraPreviewFrameView);
            mIsPreviewOnTop = true;
            mIsWindowAOnBottom = false;
        }
    }

    public void onClickConference(View v) {
        if (!mRTCConferenceManager.isConferenceStarted()) {
            startConference();
        } else {
            stopConference();
        }
    }

    public void onClickKickoutUserA(View v) {
        mRTCConferenceManager.kickoutUser(R.id.RemoteGLSurfaceViewA);
    }

    public void onClickKickoutUserB(View v) {
        mRTCConferenceManager.kickoutUser(R.id.RemoteGLSurfaceViewB);
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
        mRTCConferenceManager.switchCamera(facingId);
    }

    public void onClickCaptureFrame(View v) {
        mRTCConferenceManager.captureFrame(new RTCFrameCapturedCallback() {
            @Override
            public void onFrameCaptureSuccess(Bitmap bitmap) {
                String filepath = Environment.getExternalStorageDirectory() + "/captured.jpg";
                saveBitmapToSDCard(filepath, bitmap);
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + filepath)));
                showToast("截帧成功, 存放在 " + filepath, Toast.LENGTH_SHORT);
            }

            @Override
            public void onFrameCaptureFailed(int errorCode) {
                showToast("截帧失败，错误码：" + errorCode, Toast.LENGTH_SHORT);
            }
        });
    }

    private void updateControlButtonText() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mRTCConferenceManager.isConferenceStarted()) {
                    mControlButton.setText(getString(R.string.stop_conference));
                } else {
                    mControlButton.setText(getString(R.string.start_conference));
                }
            }
        });
    }

    private View.OnClickListener mMuteButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mMuteCheckBox.isChecked()) {
                mRTCConferenceManager.mute(RTCAudioSource.MIC);
            } else {
                mRTCConferenceManager.unMute(RTCAudioSource.MIC);
            }
        }
    };

    public void onClickVideoCapture(View v) {
        if (mIsVideoCaptureStarted) {
            mRTCConferenceManager.stopVideoCapture();
            mIsVideoCaptureStarted = false;
            mIsInReadyState = false;
            mVideoCaptureButton.setTitle("采集视频");
        } else {
            mRTCConferenceManager.startVideoCapture();
            mIsVideoCaptureStarted = true;
            mVideoCaptureButton.setTitle("停止视频采集");
        }
    }

    public void onClickAudioCapture(View v) {
        if (mIsAudioCaptureStarted) {
            mRTCConferenceManager.stopAudioCapture();
            mIsAudioCaptureStarted = false;
            mAudioCaptureButton.setTitle("采集音频");
        } else {
            mRTCConferenceManager.startAudioCapture();
            mIsAudioCaptureStarted = true;
            mAudioCaptureButton.setTitle("停止音频采集");
        }
    }

    public void onClickMuteSpeaker(View v) {
        if (!mRTCConferenceManager.isConferenceStarted()) {
            showToast(getString(R.string.not_join_room), Toast.LENGTH_SHORT);
            return;
        }
        if (mIsSpeakerMuted) {
            mRTCConferenceManager.unMute(RTCAudioSource.SPEAKER);
            mMuteSpeakerButton.setTitle(getResources().getString(R.string.button_mute_speaker));
        } else {
            mRTCConferenceManager.mute(RTCAudioSource.SPEAKER);
            mMuteSpeakerButton.setTitle(getResources().getString(R.string.button_unmute_speaker));
        }
        mIsSpeakerMuted = !mIsSpeakerMuted;
    }

    public void onClickVideoPublish(View v) {
        if (!mRTCConferenceManager.isConferenceStarted()) {
            showToast(getString(R.string.not_join_room), Toast.LENGTH_SHORT);
            return;
        }
        if (mIsVideoPublishStarted) {
            mRTCConferenceManager.unpublishLocalVideo();
            mIsVideoPublishStarted = false;
            mVideoPublishButton.setTitle("发布视频");
        } else{
            mRTCConferenceManager.publishLocalVideo();
            mIsVideoPublishStarted = true;
            mVideoPublishButton.setTitle("取消视频发布");
        }
    }

    public void onClickAudioPublish(View v) {
        if (!mRTCConferenceManager.isConferenceStarted()) {
            showToast(getString(R.string.not_join_room), Toast.LENGTH_SHORT);
            return;
        }
        if (mIsAudioPublishStarted) {
            mRTCConferenceManager.unpublishLocalAudio();
            mIsAudioPublishStarted = false;
            mAudioPublishButton.setTitle("发布音频");
        } else{
            mRTCConferenceManager.publishLocalAudio();
            mIsAudioPublishStarted = true;
            mAudioPublishButton.setTitle("取消音频发布");
        }
    }

    private RTCConferenceStateChangedListener mRTCStreamingStateChangedListener = new RTCConferenceStateChangedListener() {
        @Override
        public void onConferenceStateChanged(RTCConferenceState state, int extra) {
            switch (state) {
                case READY:
                    mIsInReadyState = true;
                    mMaxZoom = mRTCConferenceManager.getMaxZoom();
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
                    break;
            }
        }
    };

    private RTCUserEventListener mRTCUserEventListener = new RTCUserEventListener() {
        @Override
        public void onUserJoinConference(String remoteUserId) {
            Log.i(TAG, "onUserJoinConference: " + remoteUserId);
            if (mUserID.equals(remoteUserId)) {
                Log.i(TAG, "it`s me!");
            }
        }

        @Override
        public void onUserLeaveConference(String remoteUserId) {
            Log.i(TAG, "onUserLeaveConference: " + remoteUserId);
            if (mUserID == remoteUserId) {
                return;
            }
        }
    };

    private RTCRemoteWindowEventListener mRTCRemoteWindowEventListener = new RTCRemoteWindowEventListener() {
        @Override
        public void onRemoteWindowAttached(RTCVideoWindow window, String remoteUserId) {
            Log.i(TAG, "onRemoteWindowAttached: " + remoteUserId);
        }

        @Override
        public void onRemoteWindowDetached(RTCVideoWindow window, String remoteUserId) {
            Log.i(TAG, "onRemoteWindowDetached: " + remoteUserId);
        }

        @Override
        public void onFirstRemoteFrameArrived(final String remoteUserId) {
            Log.i(TAG, "onFirstRemoteFrameArrived: " + remoteUserId);
        }
    };

    private RTCMediaSubscribeCallback mRTCMediaSubscribeCallback = new RTCMediaSubscribeCallback() {
        @Override
        public boolean isSubscribeVideoStream(String fromUserId) {
            Log.i(TAG, "remote video published: " + fromUserId);
            /**
             * decided whether subscribe the video stream
             * return true -- do subscribe, return false will ignore the video stream
             */
            return true;
        }
    };

    private RTCAudioLevelCallback mRTCAudioLevelCallback = new RTCAudioLevelCallback() {
        @Override
        public void onAudioLevelChanged(String userId, int level) {
            Log.i(TAG, "onAudioLevelChanged: userId = " + userId + " level = " + level);
        }
    };

    private RTCStreamStatsCallback mRTCStreamStatsCallback = new RTCStreamStatsCallback() {
        @Override
        public void onStreamStatsChanged(String userId, int statsType, int value) {
            Log.i(TAG, "userId = " + userId + "statsType = " + statsType + " value = " + value);
        }
    };

    private CameraPreviewFrameView.Listener mCameraPreviewListener = new CameraPreviewFrameView.Listener() {
         @Override
         public boolean onSingleTapUp(MotionEvent e) {
             if (mIsPreviewOnTop) {
                 RTCVideoWindow window = mIsWindowAOnBottom ? mRTCVideoWindowA : mRTCVideoWindowB;
                 mRTCConferenceManager.switchRenderView(mCameraPreviewFrameView, window.getRTCSurfaceView());
                 mIsPreviewOnTop = false;
                 mIsWindowAOnBottom = false;
                 return true;
             }
             Log.i(TAG, "onSingleTapUp X:" + e.getX() + ",Y:" + e.getY());
             if (mIsInReadyState) {
                 setFocusAreaIndicator();
                 mRTCConferenceManager.doSingleTapUp((int) e.getX(), (int) e.getY());
                 return true;
             }
             return false;
         }

        @Override
         public boolean onZoomValueChanged(float factor) {
            if (mIsInReadyState && mRTCConferenceManager.isZoomSupported()) {
                mCurrentZoom = (int) (mMaxZoom * factor);
                mCurrentZoom = Math.min(mCurrentZoom, mMaxZoom);
                mCurrentZoom = Math.max(0, mCurrentZoom);
                Log.d(TAG, "zoom ongoing, scale: " + mCurrentZoom + ",factor:" + factor + ",maxZoom:" + mMaxZoom);
                mRTCConferenceManager.setZoomValue(mCurrentZoom);
            }
            return false;
        }
     };

    protected void setFocusAreaIndicator() {
        if (mRotateLayout == null) {
            mRotateLayout = (RotateLayout) findViewById(R.id.focus_indicator_rotate_layout);
            mRTCConferenceManager.setFocusAreaIndicator(mRotateLayout,
                    mRotateLayout.findViewById(R.id.focus_indicator));
        }
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

    private void dismissProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.dismiss();
            }
        });
    }

    private void showToast(final String text, final int duration) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ConferenceActivity.this, text, duration).show();
            }
        });
    }

    private static boolean saveBitmapToSDCard(String filepath, Bitmap bitmap) {
        try {
            FileOutputStream fos = new FileOutputStream(filepath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}