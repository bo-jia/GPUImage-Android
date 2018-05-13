package com.gpuimage.mediautils;

/**
 * Created by j on 18/4/2018.
 */
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import com.gpuimage.GSize;
import com.gpuimage.cvutils.CVImageUtils;
import com.gpuimage.extern.GReportUtils;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

public class GMediaUtils {

    private final static int OMX_QCOM_COLOR_FormatYUV420PackedSemiPlanar32m = 0x7FA30C04; // 2141391876

    // planar YUV 4:2:0
    public static boolean isYV12(int colorFormat) {
        switch (colorFormat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:{
                return true;
            }
            default: {
                return false;
            }
        }
    }

    // semi planar YUV 4:2:0
    public static boolean isNV12(int colorFormat) {
        switch (colorFormat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
            case OMX_QCOM_COLOR_FormatYUV420PackedSemiPlanar32m:
            {
                return true;
            }
            default: {
                return false;
            }
        }
    }

    private static Vector<Integer> FormatList = new Vector<>();

    public static byte[] convertToNV12(byte[] data, byte[] nv12, MediaFormat format, GSize outFrameSize) {
        int colorFormat = format.getInteger(MediaFormat.KEY_COLOR_FORMAT);

        int frameWidth  = format.getInteger(MediaFormat.KEY_WIDTH);
        int frameHeight = format.getInteger(MediaFormat.KEY_HEIGHT);

        int stride = frameWidth, sliceHeight = frameHeight, cropLeft = 0, cropRight = frameWidth-1, cropTop = 0, cropBottom = frameHeight-1;
        if (format.containsKey("stride")) {
            stride = format.getInteger("stride");
        }
        if (format.containsKey("slice-height")) {
            sliceHeight = format.getInteger("slice-height");
        }

        if (format.containsKey("crop-left")) {
            cropLeft = format.getInteger("crop-left");
        }

        if (format.containsKey("crop-right")) {
            cropRight = format.getInteger("crop-right");
        }

        if (format.containsKey("crop-top")) {
            cropTop = format.getInteger("crop-top");
        }

        if (format.containsKey("crop-bottom")) {
            cropBottom = format.getInteger("crop-bottom");
        }
        int rw = cropRight - cropLeft + 1;
        int rh = cropBottom - cropTop + 1;

        if (nv12 == null || nv12.length != rw * rh * 3 / 2) {
            nv12 = new byte[rw*rh*3/2];
        }
        int crop[] = new int[]{cropLeft, cropTop, cropRight, cropBottom};

        if (isNV12(colorFormat)) {
        } else if (isYV12(colorFormat)) {
            CVImageUtils.ConvertToSemiPlanar(data, stride, sliceHeight);
        } else {
            if (!FormatList.contains(colorFormat)) {
                GReportUtils.report(null, "" + colorFormat, GReportUtils.Type.UnhandledColorFormat);
                FormatList.add(colorFormat);
            }
        }
        CVImageUtils.CompactNV12Ela(data, frameWidth, frameHeight, crop, stride, sliceHeight, nv12);

        outFrameSize.width  = rw;
        outFrameSize.height = rh;

        return nv12;
    }

    public static MediaFormat generateVideoTrackFormat(String path) {
        File videoFile = new File(path);
        if (!videoFile.exists()) {
            return null;
        }
        long fileLength = videoFile.length();

        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        MediaFormat videoFormat = null;
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; ++i) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                extractor.selectTrack(i);
                videoFormat = format;
                break;
            }
        }
        if (videoFormat == null) {
            return null;
        }

        int width  = videoFormat.getInteger(MediaFormat.KEY_WIDTH);
        int height = videoFormat.getInteger(MediaFormat.KEY_HEIGHT);

        long duration = 0;
        while (true) {
            long timestamp = extractor.getSampleTime();
            boolean hasNext = extractor.advance();
            if (timestamp < 0 && !hasNext) {
                break;
            }
            duration = timestamp;
        }
        extractor.release();

        MediaFormat outFormat = MediaFormat.createVideoFormat("video/avc", width, height);

        outFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

        if (videoFormat.containsKey(MediaFormat.KEY_BIT_RATE)) {
            outFormat.setInteger(MediaFormat.KEY_BIT_RATE, videoFormat.getInteger(MediaFormat.KEY_BIT_RATE));
        } else {
            int bitRate = (int) (fileLength * 8 / (duration / 1e6));
            outFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        }
        if (videoFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
            outFormat.setInteger(MediaFormat.KEY_FRAME_RATE, videoFormat.getInteger(MediaFormat.KEY_FRAME_RATE));
        } else {
            outFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        }
        if (videoFormat.containsKey(MediaFormat.KEY_I_FRAME_INTERVAL)) {
            outFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, videoFormat.getInteger(MediaFormat.KEY_I_FRAME_INTERVAL));
        } else {
            outFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
        }

        return outFormat;
    }
}
