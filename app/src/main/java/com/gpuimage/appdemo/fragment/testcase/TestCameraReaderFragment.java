package com.gpuimage.appdemo.fragment.testcase;

import android.Manifest;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.gpuimage.appdemo.R;
import com.gpuimage.appdemo.base.BaseFragment;
import com.gpuimage.appdemo.camera.view.CameraView;
import com.gpuimage.appdemo.model.ItemDescription;
import com.qmuiteam.qmui.widget.QMUITopBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import permissions.dispatcher.NeedsPermission;

/**
 * @ClassName
 * @Description
 * @Author danny
 * @Date 2017/12/20 11:26
 */
public class TestCameraReaderFragment extends BaseFragment {
    @BindView(R.id.topbar) QMUITopBar mTopBar;
    @BindView(R.id.camera) protected CameraView mCameraView;
    @BindView(R.id.take_picture) protected Button mTakePicBtn;

    public static ItemDescription mItemDescription = new ItemDescription(TestCameraReaderFragment.class,
            "TestCameraReader", R.mipmap.icon_tabbar_lab);

    protected ItemDescription getQDItemDescription() {
        return mItemDescription;
    }

    private void initTopBar() {
        mTopBar.addLeftBackImageButton().setOnClickListener(v -> popBackStack());
        mTopBar.setTitle(getQDItemDescription().getName());
    }

    @Override
    protected View onCreateView() {
        View root = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_test_camera_reader, null);
        ButterKnife.bind(this, root);
        initTopBar();
        onCreateViewCompleted();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        mCameraView.start();
    }

    @Override
    public void onPause() {
        mCameraView.stop();
        super.onPause();
    }


    @NeedsPermission({Manifest.permission.CAMERA})
    public void onCreateViewCompleted() {
    }
}