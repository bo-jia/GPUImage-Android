package com.gpuimage.mediautils;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import com.gpuimage.GLog;
import com.gpuimage.cvutils.CVImageUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import static android.media.MediaExtractor.SEEK_TO_CLOSEST_SYNC;

/**
 * Created by j on 17/4/2018.
 */

public class GMediaMovieReader {

    public enum Type {
        Video,
        Audio
    }

    private MediaExtractor mMediaExtractor;
    private MediaFormat mMediaFormat;
    private int mTrackIndex;
    private long mTimestamp;
    private int mSampleFlags;
    private Type mType;

    public boolean loadMP4(String path, Type type) {
        GLog.i("load mp4: " + path);
        mType = type;
        mMediaExtractor = new MediaExtractor();
        try {
            mMediaExtractor.setDataSource(path);
        } catch (IOException e) {
            GLog.e("invalid mp4 path: " + path);
            e.printStackTrace();
            return false;
        }

        int trackCount = mMediaExtractor.getTrackCount();
        String mimeType = (type == Type.Video ? "video/" : "audio/");
        mTrackIndex = -1;
        for (int i = 0; i < trackCount; ++i) {
            MediaFormat format = mMediaExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(mimeType)) {
                GLog.i(mimeType + " track " + i + " " + format);
                mTrackIndex = i;
                mMediaFormat = format;
                break;
            }
        }
        if (mTrackIndex < 0) {
            GLog.i("Did not find specified track!");
            return false;
        }

        mMediaExtractor.selectTrack(mTrackIndex);
        return true;
    }

    public int readNextFrame(ByteBuffer buffer) {
        int size = mMediaExtractor.readSampleData(buffer, 0);
        mTimestamp = mMediaExtractor.getSampleTime();
        mSampleFlags = mMediaExtractor.getSampleFlags();
        if (mMediaExtractor.advance()) {
            return size;
        } else {
            return -1;
        }
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

    public void stop() {
        if (mMediaExtractor != null) {
            mMediaExtractor.seekTo(0, SEEK_TO_CLOSEST_SYNC);
        }
    }

    public MediaFormat getMediaFormat() {
        return mMediaFormat;
    }

    public Type getMediaType() {
        return mType;
    }
}
