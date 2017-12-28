package com.gpuimage.appdemo.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.gpuimage.appdemo.R;
import com.gpuimage.appdemo.base.BaseFragment;
import com.gpuimage.appdemo.model.ItemDescription;
import com.gpuimage.outputs.GPUImageView;
import com.qmuiteam.qmui.widget.QMUITopBar;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @ClassName
 * @Description http://www.sample-videos.com/
 * @Author danny
 * @Date 2017/12/20 11:26
 */
public abstract class BaseSingleGPUImageViewFragment extends BaseFragment {
    @BindView(R.id.topbar) QMUITopBar mTopBar;
    @BindView(R.id.bt_test) protected Button mTestBtn;
    @BindView(R.id.iv_test) protected GPUImageView mGPUImageView;

    @Override
    protected View onCreateView() {
        View root = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_single_gpuimageview, null);
        ButterKnife.bind(this, root);
        initTopBar();
        onCreateViewCompleted();
        return root;
    }

    protected abstract ItemDescription getQDItemDescription();
    protected abstract void onCreateViewCompleted();



    private void initTopBar() {
        mTopBar.addLeftBackImageButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popBackStack();
            }
        });
        mTopBar.setTitle(getQDItemDescription().getName());
    }
}