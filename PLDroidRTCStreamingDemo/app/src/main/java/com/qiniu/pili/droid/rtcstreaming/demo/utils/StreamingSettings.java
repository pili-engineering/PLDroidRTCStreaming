package com.qiniu.pili.droid.rtcstreaming.demo.utils;

import com.qiniu.pili.droid.streaming.StreamingProfile;

public class StreamingSettings {
    public static final String[] PREVIEW_SIZE_RATIO_TIPS_ARRAY = {
            "4:3",
            "16:9"
    };

    public static final String[] PREVIEW_SIZE_LEVEL_TIPS_ARRAY = {
            "SMALL",
            "MEDIUM",
            "LARGE",
    };

    public static final String[] ENCODING_SIZE_RATIO_TIPS_ARRAY = {
            "4:3",
            "16:9"
    };

    public static final String[] ENCODING_SIZE_LEVEL_TIPS_ARRAY = {
            "240P",
            "480P",
            "544P",
            "720P",
            "1088P",
    };

    public static final String[] ENCODING_TIPS_ARRAY = {
            "15fps 800kbps",
            "15fps 1200kbps",
            "24fps 800kbps",
            "24fps 1200kbps",
            "30fps 800kbps",
            "30fps 1200kbps",
    };

    public static final String[] VIDEO_QUALITY_PROFILES = {
            "HIGH",
            "MAIN",
            "BASELINE"
    };

    public static final StreamingProfile.H264Profile[] VIDEO_QUALITY_PROFILES_MAPPING = {
            StreamingProfile.H264Profile.HIGH,
            StreamingProfile.H264Profile.MAIN,
            StreamingProfile.H264Profile.BASELINE
    };

    public static final String[] YUV_FILTER_MODE = {
            "None",
            "Linear",
            "Bilinear",
            "Box"
    };

    public static final StreamingProfile.YuvFilterMode[] YUV_FILTER_MODE_MAPPING = {
            StreamingProfile.YuvFilterMode.None,
            StreamingProfile.YuvFilterMode.Linear,
            StreamingProfile.YuvFilterMode.Bilinear,
            StreamingProfile.YuvFilterMode.Box
    };
}
