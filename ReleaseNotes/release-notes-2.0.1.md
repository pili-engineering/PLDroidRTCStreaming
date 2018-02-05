# PLDroidRTCStreaming Release Notes for 2.0.1

本次更新:

## 版本

- 发布了 pldroid-rtc-streaming-2.0.1.jar
- 新增了 libpldroid_streaming_puic.so
- 更新了 libpldroid_rtc_streaming.so
- 更新了 libpldroid_mmprocessing.so
- 更新了 libpldroid_streaming_core.so

## 功能

- 新增录制时动态水印功能
- 新增 QUIC 推流功能
- 新增房间号对 “-” 的支持

## 缺陷

- 修复金立 M7 黑屏问题
- 修复纯音频推流 pause 后无法 resume 问题
- 修复弱网下 pause 小概率 ANR 问题
- 修复推流 NALU 长度溢出问题

## 更新注意事项

- 从 v2.0.1 版本开始，增加 libpldroid_streaming_puic.so 库
- libpldroid_streaming_core.so 依赖于 libpldroid_streaming_puic.so，无论是否启用 QUIC 推流，都需要包含 libpldroid_streaming_puic.so 库
- 构造函数中包含 AspectFrameLayout 参数的方法已被弃用，后续版本会删除，故不推荐使用
