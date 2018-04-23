# PLDroidRTCStreaming Release Notes for 2.0.2

本次更新:

## 版本

- 发布了 pldroid-rtc-streaming-2.0.2.jar
- 更新了 libpldroid_rtc_streaming.so
- 更新了 libpldroid_mmprocessing.so

## 功能

- 新增手动配置曝光度的接口

## 缺陷

- 修复直播过程中退后台，再回到前台，无法继续推流的问题
- 修复个别机型使用蓝牙连麦断开再连接会外放的问题
- 修复连麦者被踢之后调用 stopConference 无效的问题
- 修复连麦下使用 StreamingProfile 内置分辨率配置推流编码分辨率花屏的问题

## 优化

- 优化镜像逻辑

## 更新注意事项

- 从 v2.0.1 版本开始，增加 libpldroid_streaming_puic.so 库
- libpldroid_streaming_core.so 依赖于 libpldroid_streaming_puic.so，无论是否启用 QUIC 推流，都需要包含 libpldroid_streaming_puic.so 库
- 构造函数中包含 AspectFrameLayout 参数的方法已被弃用，后续版本会删除，故不推荐使用
