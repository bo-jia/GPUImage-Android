package com.gpuimage.mediautils;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaPlayer;

import com.gpuimage.GLog;
import com.gpuimage.GSize;
import com.gpuimage.cvutils.CVImageUtils;
import com.gpuimage.extern.GReportUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaExtractor.SEEK_TO_CLOSEST_SYNC;

/**
 * Created by j on 17/4/2018.
 */

public class GMediaVideoDecoder {
    private final int DEFAULT_TIMEOUT_US = 10000;

    private MediaCodec mMediaCodec;
    private GMediaMovieReader mReader;

    private ByteBuffer[] mInputBuffers;
    private ByteBuffer[] mOutputBuffers;

    private MediaFormat mMediaFormat;
    private MediaFormat mCodecMediaFormat;

    private double mDuration = 0;

    private byte mData[], mNV12[];

    private boolean mInputDone, mOutputDone;
    private GSize mFrameSize = new GSize();

    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    public GMediaVideoDecoder(GMediaMovieReader reader) throws IOException {
        assert (reader != null && reader.getMediaType() == GMediaMovieReader.Type.Video);

        mReader = reader;
        mMediaFormat = reader.getMediaFormat();


        mDuration = mMediaFormat.getLong(MediaFormat.KEY_DURATION);
        mFrameSize.width  = mMediaFormat.getInteger(MediaFormat.KEY_WIDTH);
        mFrameSize.height = mMediaFormat.getInteger(MediaFormat.KEY_HEIGHT);

        try {
            mMediaCodec = MediaCodec.createDecoderByType(mMediaFormat.getString(MediaFormat.KEY_MIME));
        } catch (IOException e) {
            GReportUtils.report(e, "" + mMediaFormat, GReportUtils.Type.ExceptionOfCreateDecoder);
            e.printStackTrace();
        }
    }

    public boolean decodeNextFrame() {
        while (!mOutputDone) {
            if (!mInputDone) {
                int inputIndex = mMediaCodec.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
                if (inputIndex >= 0) {
                    ByteBuffer inputBuffer = mInputBuffers[inputIndex];
                    long timestamp = mReader.timestamp();
                    int flags = mReader.flags();
                    int sampleSize = mReader.readNextFrame(inputBuffer);
                    if (sampleSize > 0) {
                        mMediaCodec.queueInputBuffer(inputIndex, 0, sampleSize, timestamp, flags);
                    } else {
                        GLog.i("InputBuffer BUFFER_FLAG_END_OF_STREAM");
                        mMediaCodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        mInputDone = true;
                    }
                }
            }

            int outputStatus = mMediaCodec.dequeueOutputBuffer(mInfo, DEFAULT_TIMEOUT_US);
            switch (outputStatus) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED: {
                    mOutputBuffers = mMediaCodec.getOutputBuffers();
                } break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED: {
                    mCodecMediaFormat = mMediaCodec.getOutputFormat();
                } break;
                case MediaCodec.INFO_TRY_AGAIN_LATER: {
                    GLog.i("no output from decoder available!");
                } break;
                default:
                {
                    if ((mInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        mOutputDone = true;
                        break;
                    }
                    if (mInfo.size == 0) {
                        GLog.i("size of decoded frame is 0");
                    }
                    int outputIndex = outputStatus;
                    ByteBuffer buffer = mOutputBuffers[outputIndex];
                    if (mData == null || mData.length != mInfo.size) {
                        mData = new byte[mInfo.size];
                    }
                    buffer.get(mData);
                    buffer.clear();
                    convertToNV12();
                    mMediaCodec.releaseOutputBuffer(outputIndex, false);
                } break;
            }
            if (outputStatus >= 0) {
                break;
            } else {
                GLog.w("unexpected result from decoder.dequeueOutputBuffer: " + outputStatus);
            }
        }
        return !mOutputDone;
    }

    public void convertToNV12() {
        mNV12 = GMediaUtils.convertToNV12(mData, mNV12, mCodecMediaFormat, mFrameSize);
    }

    public GSize getSize() {
        return new GSize(mFrameSize);
    }

    public double timestampMs() {
        return mInfo.presentationTimeUs / 1.0e3;
    }

    public byte[] getFrameNV12Data() {
        return mNV12;
    }

    public Bitmap getFrameBitmap() {

        Bitmap bmp = Bitmap.createBitmap(mFrameSize.width, mFrameSize.height, Bitmap.Config.ARGB_8888);
        byte[] rgba = new byte[mFrameSize.width * mFrameSize.height * 4];
        CVImageUtils.NV122RGBA(mNV12, mFrameSize.width, mFrameSize.height, rgba);
        bmp.copyPixelsFromBuffer(ByteBuffer.wrap(rgba));

        return bmp;
    }


    public void stop() {
        mInputDone = false;
        mOutputDone = false;
        mMediaCodec.stop();

        mReader.stop();
    }

    public void start() {
        try {
            mMediaCodec.configure(mMediaFormat, null, null, 0);
        } catch (Exception e) {
            GReportUtils.report(e, "" + mMediaFormat, GReportUtils.Type.ExceptionOfConfigureCodec);
            e.printStackTrace();
        }
        mMediaCodec.start();

        mInputBuffers  = mMediaCodec.getInputBuffers();
        mOutputBuffers = mMediaCodec.getOutputBuffers();

        mInputDone = false;
        mOutputDone = false;
    }

    public void release() {
        mMediaCodec.release();
        mReader.release();
    }

    public double getDuration() {
        return mDuration;
    }
}
