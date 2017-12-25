package com.gpuimage.appdemo.fragment.testcase;

import com.gpuimage.GLog;
import com.gpuimage.GPUImageFilter;
import com.gpuimage.appdemo.R;
import com.gpuimage.appdemo.fragment.BaseSingleGPUImageViewFragment;
import com.gpuimage.appdemo.model.AppDirectoryHelper;
import com.gpuimage.appdemo.model.ItemDescription;
import com.gpuimage.appdemo.utils.CommonUtil;
import com.gpuimage.appdemo.utils.LogUtil;
import com.gpuimage.mediautils.GMediaPlayer;
import com.gpuimage.mediautils.GMediaVideoReader;
import com.gpuimage.outputs.GPUImageMovieWriter;
import com.gpuimage.sources.GPUImageMovie;

/**
 * @ClassName
 * @Description
 * @Author danny
 * @Date 2017/12/20 15:15
 */
public class TestMovieWriterFragment extends BaseSingleGPUImageViewFragment {
    protected static final String TAG = TestMovieWriterFragment.class.getSimpleName();

    private GPUImageMovie mMovie;
    private GPUImageMovieWriter mMovieWriter;

    public static ItemDescription mItemDescription = new ItemDescription(TestMovieWriterFragment.class,
            "TestMovieWriter", R.mipmap.icon_tabbar_lab);

    @Override
    protected ItemDescription getQDItemDescription() {
        return mItemDescription;
    }

    @Override
    protected void onCreateViewCompleted() {
        testCode();
    }

    private void testCode() {
        mMovie = new GPUImageMovie();
        final GMediaPlayer player = new GMediaPlayer();
        GMediaPlayer.defaultConfig(player, mMovie);

        GPUImageFilter filter = new GPUImageFilter();
        mMovie.addTarget(filter);
        filter.addTarget(mGPUImageView);

        String path = AppDirectoryHelper.getImageCachePath() + "/final_video.mp4";
        if (!CommonUtil.copyAssetsResToSD("out_480.mp4", path)) {
            LogUtil.e(TAG, "copyAssetsResToSD error");
            return;
        }

        final GMediaVideoReader videoReader = new GMediaVideoReader();
        videoReader.loadMP4(path);

        String outputPath = AppDirectoryHelper.getImageCachePath() + "/out.mp4";
        mMovieWriter = new GPUImageMovieWriter(videoReader.getFrameWidth(), videoReader.getFrameHeight(), outputPath);
        mMovie.addTarget(mMovieWriter);

        mTestBtn.setOnClickListener(view -> new Thread(() -> {
            mMovieWriter.startRecording();
            videoReader.start();
            while (videoReader.readFrame()) {
                if (videoReader.getTimestamp() < 0) {
                    break;
                }
                GLog.v("read frame " + videoReader.getTimestamp());
                mMovie.processMovieFrame(videoReader.getNV12Data(), videoReader.getFrameWidth(), videoReader.getFrameHeight(), videoReader.getTimestamp());
            }
            mMovieWriter.finishRecording();
        }).run());
    }
}
