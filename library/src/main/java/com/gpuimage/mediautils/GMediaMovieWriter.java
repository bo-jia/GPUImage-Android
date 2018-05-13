package com.gpuimage.mediautils;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Environment;
import android.view.Surface;
import com.gpuimage.GLog;
import com.gpuimage.GSize;
import com.gpuimage.extern.GReportUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import static android.content.ContentValues.TAG;

/**
 * Created by j on 16/12/2017.
 */

public class GMediaMovieWriter {

    // TODO: these ought to be configurable as well

    public static final int MAX_SAMPLE_SIZE = 256 * 1024;

    private Surface mInputSurface;
    private MediaMuxer mMuxer;
    private MediaCodec mEncoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private int mVideoTrackIndex = -1;
    private int mAudioTrackIndex = -1;
    private boolean mMuxerStarted;

    public GSize videoSize = GSize.newZero();

    private String mVideoMimeType = "video/avc";

    public GMediaMovieWriter(int width, int height, File outputFile, HashMap<String, Object> outputSettings) throws IOException {
        /*
        mBufferInfo = new MediaCodec.BufferInfo();

        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = mEncoder.createInputSurface();
        mEncoder.start();

        // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
        // because our MediaFormat doesn't have the Magic Goodies.  These can only be
        // obtained from the encoder after it has started processing data.
        //
        // We're not actually interested in multiplexing audio.  We just want to convert
        // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
        mMuxer = new MediaMuxer(outputFile.toString(),
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        mTrackIndex = -1;
        mMuxerStarted = false;
        */

    }

    public GMediaMovieWriter(String path, Vector<MediaFormat> mediaInfo) throws IOException {

        mMuxer = new MediaMuxer(path,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        MediaFormat videoFormat = null;
        for (MediaFormat format: mediaInfo) {
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                GLog.i("video track " + format);
                videoSize.width  = format.getInteger(MediaFormat.KEY_WIDTH);
                videoSize.height = format.getInteger(MediaFormat.KEY_HEIGHT);
                videoFormat = format;
            } else if (mime.startsWith("audio/")) {
                GLog.i("audio track " + format);
                try {
                    mAudioTrackIndex = mMuxer.addTrack(format);
                } catch (Exception e) {
                    mAudioTrackIndex = -1;
                    GReportUtils.report(e, "" + format, GReportUtils.Type.ExceptionOfAudioTrack);
                }
            }
        }

        mBufferInfo = new MediaCodec.BufferInfo();

        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        mEncoder = MediaCodec.createEncoderByType(mVideoMimeType);
        mEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = mEncoder.createInputSurface();
        mEncoder.start();

        mMuxerStarted = false;
    }

    /**
     * Returns the encoder's input surface.
     */
    public Surface getInputSurface() {
        return mInputSurface;
    }

    /**
     * Releases encoder resources.
     */
    public void release() {
        GLog.i("release encoder objects");
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mMuxer != null) {
            // TODO: stop() throws an exception if you haven't fed it any data.  Keep track
            //       of frames submitted, and don't call stop() if we haven't written anything.
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
    }

    /**
     * Extracts all pending data from the encoder and forwards it to the muxer.
     * <p>
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     * <p>
     * We're just using the muxer to get a .mp4 file (instead of a raw H.264 stream).  We're
     * not recording audio.
     */
    public void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;
        GLog.v("drainEncoder(" + endOfStream + ")");

        if (endOfStream) {
            GLog.v("sending EOS to encoder");
            mEncoder.signalEndOfInputStream();
        }

        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
        while (true) {
            int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break;      // out of while
                } else {
                    GLog.v("no output available, spinning to await EOS");
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mEncoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = mEncoder.getOutputFormat();
                GLog.i("encoder output format changed: " + newFormat);

                // now that we have the Magic Goodies, start the muxer
                mVideoTrackIndex = mMuxer.addTrack(newFormat);
                mMuxer.start();
                mMuxerStarted = true;
            } else if (encoderStatus < 0) {
                GLog.w("unexpected result from encoder.dequeueOutputBuffer: " +
                        encoderStatus);
                // let's ignore it
            } else {
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    GLog.i("ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }

                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                    writeVideoSampleData(encodedData, mBufferInfo);
                    GLog.v("sent " + mBufferInfo.size + " bytes to muxer, ts=" +
                                mBufferInfo.presentationTimeUs);
                }

                mEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        GLog.e("reached end of stream unexpectedly");
                    } else {
                        GLog.i("reached end of stream");
                    }
                    break;      // out of while
                }
            }
        }
    }

    public synchronized void writeVideoSampleData(ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
        if (mMuxer != null && mVideoTrackIndex >= 0) {
            mMuxer.writeSampleData(mVideoTrackIndex, byteBuf, bufferInfo);
        }
    }

    public synchronized void writeAudioSampleData(ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
        if (mMuxer != null && mAudioTrackIndex >= 0) {
            mMuxer.writeSampleData(mAudioTrackIndex, byteBuf, bufferInfo);
        }
    }
}
