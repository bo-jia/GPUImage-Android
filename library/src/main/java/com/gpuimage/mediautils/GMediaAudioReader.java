package com.gpuimage.mediautils;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import com.gpuimage.GLog;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by j on 17/4/2018.
 */

public class GMediaAudioReader {

    private MediaExtractor mMediaExtractor;
    private MediaFormat mMediaFormat;
    private int mTrackIndex;
    private long mTimestamp;
    private int mSampleFlags;

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

        int trackCount = mMediaExtractor.getTrackCount();
        mTrackIndex = -1;
        for (int i = 0; i < trackCount; ++i) {
            MediaFormat format = mMediaExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                GLog.i("audio track " + i + " (" + mime + "): " + format);
                mTrackIndex = i;
                mMediaFormat = format;
                break;
            }
        }
        if (mTrackIndex < 0) {
            GLog.i("Did not find audio track!");
            return false;
        }

        mMediaExtractor.selectTrack(mTrackIndex);
        return true;
    }

    public int readNextFrame(ByteBuffer buffer) {
        int size = mMediaExtractor.readSampleData(buffer, 0);
        mTimestamp = mMediaExtractor.getSampleTime();
        mSampleFlags = mMediaExtractor.getSampleFlags();
        if (size > 0) {
            mMediaExtractor.advance();
        }
        return size;
    }

    public long timestamp() {
        return mTimestamp;
    }

    public int flags() {
        return mSampleFlags;
    }

    public void release() {
        if (mMediaExtractor != null) {
            mMediaExtractor.release();
            mMediaExtractor = null;
        }
    }

    public MediaFormat getMediaFormat() {
        return mMediaFormat;
    }
}
