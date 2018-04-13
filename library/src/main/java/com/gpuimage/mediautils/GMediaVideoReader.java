package com.gpuimage.mediautils;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;


import com.gpuimage.cvutils.CVImageUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import com.gpuimage.GLog;

/**
 * Created by j on 17/11/2017.
 */

public class GMediaVideoReader {
    private final String TAG = "GMediaVideoReader";

    private final int DEFAULT_TIMEOUT_US = 10000;

    private MediaCodec mMediaCodec;
    private MediaExtractor mMediaExtractor;
    private MediaFormat mMediaFormat;

    ByteBuffer[] mInputBuffers;
    ByteBuffer[] mOutputBuffers;

    private double mDuration;
    private byte mData[], mNV12[];
    private int mFrameWidth, mFrameHeight;
    private double mTimestamp;

    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    public boolean loadMP4(String path) {
        GLog.i("load mp4: " + path);
        mMediaExtractor = new MediaExtractor();
        try {
            mMediaExtractor.setDataSource(path);
        } catch (IOException e) {
            GLog.e("invalid mp4 path: " + path);
            e.printStackTrace();
            return false;
        }

        int videoTrackIndex = getVideoTrack(mMediaExtractor);
        if ( videoTrackIndex < 0 ) {
            GLog.e("no video track found!");
            return false;
        }

        mMediaExtractor.selectTrack(videoTrackIndex);

        mMediaFormat = mMediaExtractor.getTrackFormat(videoTrackIndex);
        mDuration = mMediaFormat.getLong(MediaFormat.KEY_DURATION);
        mFrameWidth = mMediaFormat.getInteger(MediaFormat.KEY_WIDTH);
        mFrameHeight = mMediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
        GLog.i("media format " + mMediaFormat);
        String mime = mMediaFormat.getString(MediaFormat.KEY_MIME);
        try {
            mMediaCodec = MediaCodec.createDecoderByType(mime);
        } catch (IOException e) {
            GLog.e("create decoder error, mime: " + mime);
            e.printStackTrace();
            return false;
        }

        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        mMediaCodec.configure(mMediaFormat, null, null, 0);

        return true;
    }

    private int getVideoTrack(MediaExtractor extractor) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; ++i) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                GLog.i("video track " + i + " (" + mime + "): " + format);
                return i;
            }
        }
        return -1;
    }

    public boolean readFrame() {

        while (true) {
            int inputIndex = mMediaCodec.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
            if (inputIndex >= 0) {
                // fill inputBuffers[inputBufferIndex] with valid data
                ByteBuffer inputBuffer = mInputBuffers[inputIndex];

                int sampleSize = mMediaExtractor.readSampleData(inputBuffer, 0);

                if (mMediaExtractor.advance() && sampleSize > 0) {
                    mMediaCodec.queueInputBuffer(inputIndex, 0, sampleSize, mMediaExtractor.getSampleTime(), 0);
                } else {
                    GLog.i("InputBuffer BUFFER_FLAG_END_OF_STREAM");
                    mMediaCodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    return false;
                }
            }

            int outputStatus = mMediaCodec.dequeueOutputBuffer(mInfo, DEFAULT_TIMEOUT_US);
            switch (outputStatus) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED: {
                    mOutputBuffers = mMediaCodec.getOutputBuffers();
                } break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED: {
                    mMediaFormat = mMediaCodec.getOutputFormat();
                } break;
                case MediaCodec.INFO_TRY_AGAIN_LATER: {

                } break;
                default:
                {
                    if (mInfo.size == 0) {
                        GLog.i("size of decoded frame is 0");
                    }
                    int outputIndex = outputStatus;
                    ByteBuffer buffer = mOutputBuffers[outputIndex];
                    if (mData == null || mData.length != mInfo.size) {
                        mData = new byte[mInfo.size];
                    }
                    mTimestamp = mMediaExtractor.getSampleTime();
                    buffer.get(mData);
                    buffer.clear();
                    convertToNV12();
                    mMediaCodec.releaseOutputBuffer(outputIndex, false);
                } break;
            }
            if (outputStatus >= 0) {
                break;
            }
        }
        return true;
    }

    public void convertToNV12() {
        int colorFormat = mMediaFormat.getInteger(MediaFormat.KEY_COLOR_FORMAT);

        int frameWidth  = mMediaFormat.getInteger(MediaFormat.KEY_WIDTH);
        int frameHeight = mMediaFormat.getInteger(MediaFormat.KEY_HEIGHT);

        int stride = frameWidth, sliceHeight = frameHeight, cropLeft = 0, cropRight = frameWidth-1, cropTop = 0, cropBottom = frameHeight-1;
        if (mMediaFormat.containsKey("stride")) {
            stride = mMediaFormat.getInteger("stride");
        }
        if (mMediaFormat.containsKey("slice-height")) {
            sliceHeight = mMediaFormat.getInteger("slice-height");
        }

        if (mMediaFormat.containsKey("crop-left")) {
            cropLeft = mMediaFormat.getInteger("crop-left");
        }

        if (mMediaFormat.containsKey("crop-right")) {
            cropRight = mMediaFormat.getInteger("crop-right");
        }

        if (mMediaFormat.containsKey("crop-top")) {
            cropTop = mMediaFormat.getInteger("crop-top");
        }

        if (mMediaFormat.containsKey("crop-bottom")) {
            cropBottom = mMediaFormat.getInteger("crop-bottom");
        }

//        Log.d(TAG, "color format:" + colorFormat + ", width:" + frameWidth + " height:" + frameHeight);
//        Log.d(TAG, "stride:" + stride + " slice height:" + sliceHeight + " crop left:" + cropLeft + " right:" + cropRight + " top:" + cropTop + " bottom:" + cropBottom);

        int rw = cropRight - cropLeft + 1;
        int rh = cropBottom - cropTop + 1;

        if (mNV12 == null || mNV12.length != rw * rh * 3 / 2) {
            mNV12 = new byte[rw*rh*3/2];
        }
        int crop[] = new int[]{cropLeft, cropTop, cropRight, cropBottom};
        CVImageUtils.CompactNV12Ela(mData, frameWidth, frameHeight, crop, stride, sliceHeight, mNV12);

        mFrameWidth  = rw;
        mFrameHeight = rh;
    }

    public void stop() {
        mMediaCodec.stop();
    }

    public void start() {
        mMediaCodec.start();
        mInputBuffers = mMediaCodec.getInputBuffers();
        mOutputBuffers = mMediaCodec.getOutputBuffers();
    }

    public void close() {
        mMediaCodec.stop();
        mMediaCodec.release();
        mMediaExtractor.release();
    }

    public int getFrameWidth() {
        return mFrameWidth;
    }

    public int getFrameHeight() {
        return mFrameHeight;
    }

    public byte[] getNV12Data() {
        return mNV12;
    }

    public double getTimestamp() { return mTimestamp / 1e3; }

    public Bitmap getFrame() {

        Bitmap show = Bitmap.createBitmap(mFrameWidth, mFrameHeight, Bitmap.Config.ARGB_8888);
        byte[] rgba = new byte[mFrameWidth * mFrameHeight * 4];
        CVImageUtils.NV122RGBA(mNV12, mFrameWidth, mFrameHeight, rgba);
        show.copyPixelsFromBuffer(ByteBuffer.wrap(rgba));

        return show;
    }
}
