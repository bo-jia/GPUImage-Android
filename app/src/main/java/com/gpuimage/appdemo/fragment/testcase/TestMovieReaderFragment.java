package com.gpuimage.appdemo.fragment.testcase;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.gpuimage.GPUImageFilter;
import com.gpuimage.GPUImagePicture;
import com.gpuimage.appdemo.R;
import com.gpuimage.appdemo.fragment.BaseSingleGPUImageViewFragment;
import com.gpuimage.appdemo.model.AppDirectoryHelper;
import com.gpuimage.appdemo.model.ItemDescription;
import com.gpuimage.appdemo.utils.BitmapUtil;
import com.gpuimage.appdemo.utils.CommonUtil;
import com.gpuimage.appdemo.utils.LogUtil;
import com.gpuimage.cvutils.CVImageUtils;
import com.gpuimage.cvutils.CVPicture;
import com.gpuimage.mediautils.GMediaPlayer;
import com.gpuimage.sources.GPUImageMovie;

import java.io.File;

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
        File file = new File("/mnt/sdcard/gpuimage/log.txt");
        if (file.exists()) {
            file.delete();
        }
        testCode();
    }

    private boolean play = false;
    GMediaPlayer player;
    private void testCode() {

        player = new GMediaPlayer();

        final String path = AppDirectoryHelper.getImageCachePath() + "/final_video.mp4";
        if (!CommonUtil.copyAssetsResToSD("10.mp4", path)) {
            LogUtil.e("TestMovieReaderFragment", "copyAssetsResToSD error");
            return;
        }

        // 打开效果
        player.setLooping(true);

        player.loadMP4(path);
        player.start();
        GPUImageMovie movie = new GPUImageMovie();
        player.addPlayerListener(new GMediaPlayer.PlayerCallback() {
            @Override
            public void onNewFrame(byte[] nv12, int width, int height, double timestamp) {
                movie.processMovieFrame(nv12, width, height, timestamp);
            }

            @Override
            public void onEnd() {}
        });
        movie.addTarget(mGPUImageView);

        mTestBtn.setOnClickListener(view -> {


            if (play) {
                player.pause();
            } else {
                player.play();

            }
            play = !play;
//            player.pause();
//            String outputPath = AppDirectoryHelper.getImageCachePath() + "/out.mp4";
//            RColorFactory.saveFilteredMovie(path, outputPath, null);
        });



/*
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
*/
    }

    @Override
    public void onPause() {
        if (player != null) {
            player.stop();
            player.release();
        }
        super.onPause();
    }
}
