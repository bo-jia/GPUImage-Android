package com.gpuimage.appdemo.fragment;

import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.gpuimage.GPUImageFilter;
import com.gpuimage.appdemo.R;
import com.gpuimage.appdemo.base.BaseFragment;
import com.gpuimage.appdemo.model.QDItemDescription;
import com.gpuimage.mediautils.GMediaPlayer;
import com.gpuimage.outputs.GPUImageMovieWriter;
import com.gpuimage.outputs.GPUImageView;
import com.gpuimage.sources.GPUImageMovie;
import com.qmuiteam.qmui.widget.QMUITopBar;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @ClassName
 * @Description
 * @Author danny
 * @Date 2017/12/20 11:26
 */
public class TestMovieReaderFragment extends BaseFragment {

    public static String haha = "adf";
    @BindView(R.id.topbar) QMUITopBar mTopBar;
    @BindView(R.id.bt_test) Button bt;
    @BindView(R.id.iv_test) GPUImageView glSurfaceView;

    private GPUImageMovie mMovie;
    private GPUImageMovieWriter mMovieWriter;

    public static QDItemDescription mQDItemDescription = new QDItemDescription(TestMovieReaderFragment.class,
            "TestMovieReaderFragment", R.mipmap.icon_tabbar_lab);

    @Override
    protected View onCreateView() {
        View root = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_single_gpuimageview, null);
        ButterKnife.bind(this, root);
        initTopBar();

        testCode();
        return root;
    }

    private void testCode() {
        mMovie = new GPUImageMovie();
//        mMovie.addTarget(glSurfaceView);
        final GMediaPlayer player = new GMediaPlayer();
        GMediaPlayer.defaultConfig(player, mMovie);

        GPUImageFilter filter = new GPUImageFilter();
        mMovie.addTarget(filter);
        filter.addTarget(glSurfaceView);


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
        bt.setOnClickListener(new View.OnClickListener() {
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

    private void initTopBar() {
        mTopBar.addLeftBackImageButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popBackStack();
            }
        });
        mTopBar.setTitle(TestMovieReaderFragment.class.getSimpleName());
    }
}