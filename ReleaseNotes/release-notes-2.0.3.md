# PLDroidRTCStreaming Release Notes for 2.0.3

本次更新:

## 版本

- 发布了 pldroid-rtc-streaming-2.0.3.jar
- 更新了 libpldroid_rtc_streaming.so

## 缺陷

- 修复数据上报，集合分辨率不正常的问题
- 修复个别机型连麦时间戳不正常的问题
- 修复频繁进出房间时的 eglContext2 Failed 的问题
- 修复小米 max2 连麦推流的回声问题
- 修复在预览界面动态切换横竖屏后连麦花屏的问题
- 修复个别场景下音量回调不生效的问题
- 修复偶现的收不到 VIDEO_ON 回调的问题
- 修复连麦下停止推流再重新推流，远端小窗口消失的问题

## 更新注意事项

- 从 v2.0.1 版本开始，增加 libpldroid_streaming_puic.so 库
- libpldroid_streaming_core.so 依赖于 libpldroid_streaming_puic.so，无论是否启用 QUIC 推流，都需要包含 libpldroid_streaming_puic.so 库
- 构造函数中包含 AspectFrameLayout 参数的方法已被弃用，后续版本会删除，故不推荐使用
