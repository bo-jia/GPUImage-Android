package com.gpuimage.appdemo.fragment.testcase;

import android.Manifest;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.gpuimage.GPUImageFilter;
import com.gpuimage.GSize;
import com.gpuimage.appdemo.R;
import com.gpuimage.appdemo.base.BaseFragment;
import com.gpuimage.appdemo.camera.view.CameraView;
import com.gpuimage.appdemo.model.AppDirectoryHelper;
import com.gpuimage.appdemo.model.ItemDescription;
import com.gpuimage.appdemo.utils.LogUtil;
import com.gpuimage.outputs.GPUImageMovieWriter;
import com.gpuimage.outputs.GPUImageView;
import com.gpuimage.sources.GPUImageVideoCamera;
import com.qmuiteam.qmui.widget.QMUITopBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import permissions.dispatcher.NeedsPermission;

/**
 * @ClassName
 * @Description
 * @Author danny
 * @Date 2017/12/20 11:26
 */
public class TestCameraReaderFragment extends BaseFragment {
    protected static final String TAG = TestCameraReaderFragment.class.getSimpleName();
    @BindView(R.id.topbar) QMUITopBar mTopBar;
    @BindView(R.id.camera) protected CameraView mCameraView;
    @BindView(R.id.take_picture) protected Button mTakePicBtn;
    @BindView(R.id.start_recording) protected Button mStartRecBtn;
    @BindView(R.id.stop_recording) protected Button mStopRecBtn;

    protected GPUImageView mGPUImageView;
    private GPUImageVideoCamera mGPUImageVideoCamera;

    private GPUImageMovieWriter mMovieWriter;
    @Override
    protected View onCreateView() {
        View root = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_test_gpuimage_camera_reader, null);
        ButterKnife.bind(this, root);
        initTopBar();
        mTakePicBtn.setText("open camera");
        mStartRecBtn.setText("start record");
        mStopRecBtn.setText("stop record");
        onCreateViewCompleted();
        LogUtil.v(TAG, "onCreateView");

        return root;
    }

    @NeedsPermission({Manifest.permission.CAMERA})
    public void onCreateViewCompleted() {
        mGPUImageView = mCameraView.mGPUImageView;
        mGPUImageVideoCamera = GPUImageVideoCamera.getInstance();
        mGPUImageVideoCamera.resetInputSize(new GSize(1440,1080));

        String outputPath = AppDirectoryHelper.getImageCachePath() + "/out.mp4";
        mMovieWriter = new GPUImageMovieWriter(1280, 720, outputPath);

        GPUImageFilter filter = new GPUImageFilter();
        mGPUImageVideoCamera.addTarget(filter);

        filter.addTarget(mGPUImageView);
        filter.addTarget(mMovieWriter);
    }

    @Override
    public void onResume() {
        super.onResume();
       // mCameraView.start();
    }

    @Override
    public void onPause() {
        mCameraView.stop();
        super.onPause();
    }


    @OnClick(R.id.take_picture )
    public void openCamera(){
         mCameraView.start();
    }

    @OnClick(R.id.start_recording )
    public void startRecording(){
        mMovieWriter.startRecording();
    }

    @OnClick(R.id.stop_recording )
    public void stopRecording(){
        mMovieWriter.finishRecording();
    }

    public static ItemDescription mItemDescription = new ItemDescription(TestCameraReaderFragment.class,
            "Camera:使用gpuimageview", R.mipmap.icon_tabbar_lab);

    protected ItemDescription getQDItemDescription() {
        return mItemDescription;
    }

    private void initTopBar() {
        mTopBar.addLeftBackImageButton().setOnClickListener(v -> popBackStack());
        mTopBar.setTitle(getQDItemDescription().getName());
    }

}