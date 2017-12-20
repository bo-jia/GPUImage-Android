package com.gpuimage.appdemo.fragment.testcase;

import android.os.Environment;
import android.view.View;

import com.gpuimage.GPUImageFilter;
import com.gpuimage.appdemo.R;
import com.gpuimage.appdemo.fragment.BaseSingleGPUImageViewFragment;
import com.gpuimage.appdemo.model.ItemDescription;
import com.gpuimage.mediautils.GMediaPlayer;
import com.gpuimage.outputs.GPUImageMovieWriter;
import com.gpuimage.sources.GPUImageMovie;

/**
 * @ClassName
 * @Description
 * @Author danny
 * @Date 2017/12/20 15:15
 */
public class TestMovieReaderFragment extends BaseSingleGPUImageViewFragment {
    private GPUImageMovie mMovie;
    private GPUImageMovieWriter mMovieWriter;

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
//        mMovie.addTarget(glSurfaceView);
        final GMediaPlayer player = new GMediaPlayer();
        GMediaPlayer.defaultConfig(player, mMovie);

        GPUImageFilter filter = new GPUImageFilter();
        mMovie.addTarget(filter);
        filter.addTarget(mGPUImageView);


        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test.avi";
        /*
        InputStream inputStream = null;
        try {
            inputStream = getApplicationContext().getAssets().open("Megamind.avi");
            writeToFile(inputStream, path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final GMediaVideoReader videoReader = new GMediaVideoReader();
        videoReader.loadMP4(path);

        String outputPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/gpuimage/out.mp4";
        mMovieWriter = new GPUImageMovieWriter(videoReader.getFrameWidth(), videoReader.getFrameHeight(), outputPath);
        mMovie.addTarget(mMovieWriter);
*/
        mTestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/final_video.mp4";
                player.loadMP4(path);
                player.start();
                player.play();
/*
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mMovieWriter.startRecording();
                        videoReader.start();
                        while (videoReader.readFrame()) {
                            if (videoReader.getTimestamp() < 0) break;
                            GLog.v("read frame " + videoReader.getTimestamp());
//                            try {
                                mMovie.processMovieFrame(videoReader.getNV12Data(), videoReader.getFrameWidth(), videoReader.getFrameHeight(), videoReader.getTimestamp());
//                                Thread.sleep(50);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }

                        }
                        mMovieWriter.finishRecording();
                    }
                }).run();
*/


/*
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.test);
                if (test)
                    iv_test.setImageBitmap(bitmap);
                else {
                    byte[] data = new byte[bitmap.getWidth()*bitmap.getHeight()*4];
                    bitmap.copyPixelsToBuffer(ByteBuffer.wrap(data));
                    OceanUtils.enhance(data, bitmap.getWidth(), bitmap.getHeight(), data);
                    bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(data));
                    iv_test.setImageBitmap(bitmap);
                }
                test = !test;
//                iv_test.setImageResource(R.drawable.test);
*/
            }
        });
    }
}
