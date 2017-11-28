# 1.x.x -> 2.x.x 版本迁移文档

## 说明

- 本次更新，全面升级连麦内核，可提供更好的连麦体验。

- **注意：** 1.x.x 版本与 2.x.x 版本不兼容，新老版本无法互相连麦，升级请详细阅读文档及更改说明

## 版本

- 发布了 pldroid-rtc-streaming-2.0.0.jar
- 更新了 libpldroid_rtc_streaming.so

## 更改说明

具体接口使用方式详见 docs 目录下的 PLDroidRTCStreaming 文档。

### 新增接口说明

- 新增 RTCConferenceOptions 配置连麦横竖屏的接口

```java
/**
 * 设置连麦画面的 orientation
 *
 * @param orientation the orientation
 */
public RTCConferenceOptions setVideoEncodingOrientation(VIDEO_ENCODING_ORIENTATION orientation)
```

### 变更接口说明

- 变更初始化接口

在原有接口的基础上新增首选连麦连接服务器的区域码的配置参数（必须），同时建议整个进程运行过程中只执行一次初始化和反初始化，初始化后不会在后台占用 CPU 资源。
接口更改具体如下:

```java
/**
  * 初始化引擎
  * 该类的所有其他接口必须在本方法调用成功后才能使用
  *
  * @param context the context Android 的上下文句柄
  * @param serverRegionId 连麦服务器的区域，详细区域请参考 RTCServerRegion
  * @return 错误码，0 代表成功，其他数值为初始化失败
  */
public static int init(Context context, int serverRegionId);

/**
  * 初始化引擎
  * 该类的所有其他接口必须在本方法调用成功后才能使用
  *
  * @param context the context Android 的上下文句柄
  * @param serverRegionId 连麦服务器的区域
  * @param logPath 日志保存路径，默认为 null
  * @return 错误码，0 代表成功，其他数值为初始化失败
  */
public static int init(Context context, int serverRegionId, String logPath);
```

区域配置参数详见官方文档

- 变更连麦数据统计回调的接口

原有的方式已弃用，不再通过 setStreamStatsEnabled(true) 的方式开启回调并手动获取，而是通过新增 RTCConferenceOptions 配置如下接口来配置统计时间间隔，并根据时间间隔触发回调

```java
/**
 * 开启数据统计功能，每隔 interval ms 会回调一次数据，数据为该时间段内的统计结果
 *
 * @param interval 统计时间间隔（单位：毫秒）默认为0，为0时不统计
 */
public RTCConferenceOptions setStreamStatsInterval(int interval)

/**
 * 注册数据统计的监听回调，该接口适用于 RTCMediaStreamingManager 和 RTCConferenceManager
 */
public final void setRTCStreamStatsCallback(RTCStreamStatsCallback callback);
```

开启数据统计回调后，回调信息如下：

```java
public interface RTCStreamStatsCallback {
    // 音频码率
    int RTC_STATS_AUDIO_BITRATE = 1;
    // 视频码率
    int RTC_STATS_VIDEO_BITRATE = 2;
    // 视频帧率
    int RTC_STATS_VIDEO_FPS = 3;
    // 音频丢包率，千分比
    int RTC_STATS_AUDIO_PACKET_LOSS_RATE = 4;
    // 视频丢包率，千分比
    int RTC_STATS_VIDEO_PACKET_LOSS_RATE = 5;

    void onStreamStatsChanged(String userId, int statsType, int value);
}
```

- 变更连麦音量信息统计的回调接口

```java
public interface RTCAudioLevelCallback {
    /**
     * 开启音量回调后触发.
     *
     * @param userId userId
     * @param level 音量级别
     */
    void onAudioLevelChanged(String userId, int level);
}
```

- 变更主播端自定义连麦窗口合流位置的接口

```java
/**
 * 设置自定义窗口的合流位置
 *
 * @param x 窗口的 x 坐标
 * @param y 窗口的 y 坐标
 * @param width 窗口宽度
 * @param height 窗口高度
 */
public void setLocalWindowPosition(int x, int y, int width, int height);
```

- 变更连麦小窗口的显示控件

连麦小窗口的显示控件从老版本的 GLSurfaceView 变更为 RTCSurfaceView

```xml
<FrameLayout
      android:id="@+id/RemoteWindowA"
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:visibility="invisible"
      android:clickable="true"
      android:onClick="onClickRemoteWindowA" >
      <com.qiniu.pili.droid.rtcstreaming.RTCSurfaceView
            android:id="@+id/RemoteGLSurfaceViewA"
            android:layout_width="120dp"
            android:layout_height="160dp"
            android:visibility="invisible"/>
</FrameLayout>
```

- 变更 mute 的逻辑

mute 逻辑变更为使用枚举类型来实现，具体使用如下：

```java
/**
 * 静音
 *
 * @param audioSource 静音的目标设备（麦克风、扬声器、合流数据）
 */
public void mute(RTCAudioSource audioSource);

/**
 * 取消静音
 *
 * @param audioSource 取消静音的目标设备（麦克风、扬声器、合流数据）
 */
public void unMute(RTCAudioSource audioSource);
```

RTCAudioSource 的具体内容如下：

```java
public enum RTCAudioSource {
    // 麦克风
    MIC,
    // 扬声器
    SPEAKER,
    // 合流数据
    MIXAUDIO
}
```

- 变更连麦重连机制

去除 RTCConferenceOptions 中对重连超时时间和重连次数的配置，连麦断线后，SDK 内部会自动重连，断线和重连的状态消息会通过 `RTCConferenceStateChangedListener` 进行回调，`RECONNECTING` 表示与服务器断线了，正在重连；`RECONNECTED` 表示与服务器连接成功；目前的机制是重连中没有主动调用 LeaveRoom 的话，会重试最多500次

### 删除接口说明

- 删除 RTCConferenceOptions 中配置重连超时时间的接口

```java
public RTCConferenceOptions setConnectTimeout(int milliseconds)
```

- 删除 RTCConferenceOptions 中配置重连次数的接口

```java
public RTCConferenceOptions setReconnectTimes(int reconnectTimes)
```

## 更新注意事项

- 初始化和反初始化建议在整个 app 开始运行和最终销毁时调用，调用一次即可，初始化后不会在后台占用 CPU 资源

- **注意：** 通过 setPreferredVideoEncodingSize 方法设置推流尺寸的时候，要根据推流的尺寸进行相应的配置。比如：横屏推流，则传入（width, height）；竖屏推流，则需传入（height, width）。

- 变更了错误码，新的错误码定义如下：

| 错误码| 描述                          |
| ---- | ---------------------------- |
| 0    | SUCCESS                      |
| -2   | ERROR_INVALID_ALG            |
| -3   | ERROR_ALREADY_INITIALIZED    |
| -4   | ERROR_NOT_INITIALIZED        |
| -7   | ERROR_WRONG_STATE            |
| -11  | ERROR_ROOM_TOKEN             |
| -100 | ERROR_OUT_OF_MEMORY          |
| -101 | ERROR_ENGINE_START_FAILED    |
| -102 | ERROR_ENGINE_STOP_FAILED     |
| -103 | ERROR_ILLEGAL_SDK            |
| -104 | ERROR_SERVER_INVALID         |
| -105 | ERROR_NETWORK_ERROR          |
| -106 | ERROR_SERVER_INNER_ERROR     |
| -1000| ERROR_UNEXPECTED             |
| 2001 | ERROR_UNKNOWN                |
| 2002 | ERROR_NOT_JOIN_ROOM          |
| 2003 | ERROR_CAMERA_NOT_READY       |
| 2004 | ERROR_AUTH_DNSLOOKUP_FAILED  |
| 2005 | ERROR_AUTH_CONNECT_FAILED    |
| 2006 | ERROR_AUTH_HTTP_BAD_REQUEST  |
| 2007 | ERROR_AUTH_HTTP_UNAUTHORIZED |
| 2008 | ERROR_AUTH_HTTP_NOT_FOUND    |
| 2009 | ERROR_MALFORMEDURL_EXCEPTION |
| 2010 | ERROR_JSON_EXCEPTION         |
| 2011 | ERROR_IO_EXCEPTION           |
| 2012 | ERROR_SERVER_TIMEOUT         |