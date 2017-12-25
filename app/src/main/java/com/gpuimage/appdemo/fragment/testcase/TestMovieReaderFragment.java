package com.gpuimage.appdemo.fragment.testcase;

import android.os.Environment;

import com.gpuimage.GPUImageFilter;
import com.gpuimage.appdemo.R;
import com.gpuimage.appdemo.fragment.BaseSingleGPUImageViewFragment;
import com.gpuimage.appdemo.model.AppDirectoryHelper;
import com.gpuimage.appdemo.model.ItemDescription;
import com.gpuimage.appdemo.utils.CommonUtil;
import com.gpuimage.appdemo.utils.LogUtil;
import com.gpuimage.mediautils.GMediaPlayer;
import com.gpuimage.sources.GPUImageMovie;

/**
 * @ClassName
 * @Description
 * @Author danny
 * @Date 2017/12/20 15:15
 */
public class TestMovieReaderFragment extends BaseSingleGPUImageViewFragment {
    private GPUImageMovie mMovie;

    public static ItemDescription mItemDescription = new ItemDescription(TestMovieReaderFragment.class,
            "TestMovieReader", R.mipmap.icon_tabbar_lab);

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

        final String path = AppDirectoryHelper.getImageCachePath() + "/final_video.mp4";
        if (!CommonUtil.copyAssetsResToSD("out_480.mp4", path)) {
            LogUtil.e("TestMovieReaderFragment", "copyAssetsResToSD error");
            return;
        }

        mTestBtn.setOnClickListener(view -> {
            player.loadMP4(path);
            player.start();
            player.play();
        });
    }
}
