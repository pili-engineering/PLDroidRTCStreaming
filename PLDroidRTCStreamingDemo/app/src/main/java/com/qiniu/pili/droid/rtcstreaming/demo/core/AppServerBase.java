package com.qiniu.pili.droid.rtcstreaming.demo.core;

/**
 * 该接口用于处理连麦的服务端流程，如需实现自己的服务器业务，需自行实现该接口
 */
public interface AppServerBase {
    /**
     * 登录
     *
     * @param userName 登录用户名
     * @param password 登录密码
     * @return 成功或失败
     */
    boolean login(String userName, String password);

    /**
     * 请求推流地址
     *
     * @param roomName 房间号
     * @return 推流地址
     */
    String requestPublishAddress(String roomName);

    /**
     * 请求播放地址
     *
     * @param roomName 房间号
     * @return 播放地址
     */
    String requestPlayURL(String roomName);

    /**
     * 请求连麦 token
     *
     * @param userId 加入房间的 userId，可以和登录的 userName 不一样
     * @param roomName 房间号
     * @return 连麦 token
     */
    String requestRoomToken(String userId, String roomName);
}
