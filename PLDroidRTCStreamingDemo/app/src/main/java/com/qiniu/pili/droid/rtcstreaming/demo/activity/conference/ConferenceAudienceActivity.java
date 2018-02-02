package com.qiniu.pili.droid.rtcstreaming.demo.activity.conference;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.qiniu.pili.droid.rtcstreaming.RTCAudioLevelCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceManager;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceOptions;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceState;
import com.qiniu.pili.droid.rtcstreaming.RTCConferenceStateChangedListener;
import com.qiniu.pili.droid.rtcstreaming.RTCStartConferenceCallback;
import com.qiniu.pili.droid.rtcstreaming.RTCSurfaceView;
import com.qiniu.pili.droid.rtcstreaming.RTCVideoWindow;
import com.qiniu.pili.droid.rtcstreaming.demo.R;
import com.qiniu.pili.droid.rtcstreaming.demo.core.QiniuAppServer;

public class ConferenceAudienceActivity extends Activity {
    private static final String TAG = "ConferenceAudienceActivity";

    private RTCConferenceManager mRTCConferenceManager;
    private String mRoomName;
    private String mUserID;
    private ProgressDialog mProgressDialog;

    private RTCVideoWindow mMainRTCVideoWindow;
    private RTCVideoWindow mRTCVideoWindowA;
    private RTCVideoWindow mRTCVideoWindowB;

    private boolean mIsAudioLevelCallbackEnabled = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_conference_audience);

        mRoomName = getIntent().getStringExtra("roomName");
        mIsAudioLevelCallbackEnabled = getIntent().getBooleanExtra("audioLevelCallback", false);

        mRTCConferenceManager = new RTCConferenceManager(getApplicationContext());
        mRTCConferenceManager.setConferenceStateListener(mRTCStreamingStateChangedListener);
        mRTCConferenceManager.setAudioLevelCallback(mRTCAudioLevelCallback);

        RTCConferenceOptions options = new RTCConferenceOptions();
        mRTCConferenceManager.setConferenceOptions(options);

        mRTCVideoWindowA = new RTCVideoWindow(findViewById(R.id.small_windowA), (RTCSurfaceView) findViewById(R.id.small_gl_surface_view_A));
        mRTCVideoWindowB = new RTCVideoWindow(findViewById(R.id.small_windowB), (RTCSurfaceView) findViewById(R.id.small_gl_surface_view_B));
        mMainRTCVideoWindow = new RTCVideoWindow(findViewById(R.id.main_window), (RTCSurfaceView) findViewById(R.id.main_gl_surface_view));
        mMainRTCVideoWindow.setZOrderOnTop(false);

        mRTCConferenceManager.addRemoteWindow(mMainRTCVideoWindow);
        mRTCConferenceManager.addRemoteWindow(mRTCVideoWindowA);
        mRTCConferenceManager.addRemoteWindow(mRTCVideoWindowB);

        mRTCConferenceManager.prepare(null);
        mProgressDialog = new ProgressDialog(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startConference();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopConference();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRTCConferenceManager.destroy();
    }

    private boolean startConference() {
        if (!QiniuAppServer.isNetworkAvailable(this)) {
            Toast.makeText(ConferenceAudienceActivity.this, "network is unavailable!!!", Toast.LENGTH_SHORT).show();
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
        return true;
    }

    public void onClickSmallWindowA(View v) {
        FrameLayout window = (FrameLayout) v;
        if (window.getChildAt(0).getId() == mRTCVideoWindowA.getRTCSurfaceView().getId()) {
            mRTCConferenceManager.switchRenderView(mRTCVideoWindowA.getRTCSurfaceView(), mMainRTCVideoWindow.getRTCSurfaceView());
        } else {
            mRTCConferenceManager.switchRenderView(mMainRTCVideoWindow.getRTCSurfaceView(), mRTCVideoWindowA.getRTCSurfaceView());
        }
    }

    public void onClickSmallWindowB(View v) {
        FrameLayout window = (FrameLayout) v;
        if (window.getChildAt(0).getId() == mRTCVideoWindowB.getRTCSurfaceView().getId()) {
            mRTCConferenceManager.switchRenderView(mRTCVideoWindowB.getRTCSurfaceView(), mMainRTCVideoWindow.getRTCSurfaceView());
        } else {
            mRTCConferenceManager.switchRenderView(mMainRTCVideoWindow.getRTCSurfaceView(), mRTCVideoWindowB.getRTCSurfaceView());
        }
    }

    private RTCConferenceStateChangedListener mRTCStreamingStateChangedListener = new RTCConferenceStateChangedListener() {
        @Override
        public void onConferenceStateChanged(RTCConferenceState state, int extra) {
            switch (state) {
                case READY:
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
                case USER_JOINED_AGAIN:
                    showToast(getString(R.string.user_join_other_where), Toast.LENGTH_SHORT);
                    finish();
                    break;
                case USER_KICKOUT_BY_HOST:
                    showToast(getString(R.string.user_kickout_by_host), Toast.LENGTH_SHORT);
                    finish();
                    break;
                default:
                    break;
            }
        }
    };

    private RTCAudioLevelCallback mRTCAudioLevelCallback = new RTCAudioLevelCallback() {
        @Override
        public void onAudioLevelChanged(String userId, int level) {
            Log.i(TAG, "onAudioLevelChanged: userId = " + userId + " level = " + level);
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ConferenceAudienceActivity.this, text, duration).show();
            }
        });
    }
}
