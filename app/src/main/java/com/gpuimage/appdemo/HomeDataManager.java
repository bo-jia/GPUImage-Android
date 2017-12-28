package com.gpuimage.appdemo;


import com.gpuimage.appdemo.base.BaseFragment;
import com.gpuimage.appdemo.fragment.testcase.TestCameraReaderFragment;
import com.gpuimage.appdemo.fragment.testcase.TestCameraV1ReaderFragment;
import com.gpuimage.appdemo.fragment.testcase.TestMovieReaderFragment;
import com.gpuimage.appdemo.fragment.testcase.TestMovieWriterFragment;
import com.gpuimage.appdemo.fragment.testcase.TestOffscreenCameraFragment;
import com.gpuimage.appdemo.model.ItemDescription;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cginechen
 * @date 2016-10-21
 */
public class HomeDataManager {
    private static HomeDataManager sInstance;
    private ArrayList<ItemDescription> mComponentsNames;
    private ArrayList<ItemDescription> mUtilNames;
    private ArrayList<ItemDescription> mLabNames;

    private HomeDataManager() {
        initComponentsDesc();
        initUtilDesc();
        initLabDesc();
    }

    public static HomeDataManager getInstance() {
        if (sInstance == null) {
            sInstance = new HomeDataManager();
        }
        return sInstance;
    }

    /**
     * Components
     */
    private void initComponentsDesc() {
        mComponentsNames = new ArrayList<>();
        mComponentsNames.add(TestMovieReaderFragment.mItemDescription);
        mComponentsNames.add(TestMovieWriterFragment.mItemDescription);
        mComponentsNames.add(TestCameraReaderFragment.mItemDescription);
        mComponentsNames.add(TestCameraV1ReaderFragment.mItemDescription);
        mComponentsNames.add(TestOffscreenCameraFragment.mItemDescription);
    }

    /**
     * Helper
     */
    private void initUtilDesc() {
        mUtilNames = new ArrayList<>();
        mUtilNames.add(TestMovieReaderFragment.mItemDescription);
    }

    /**
     * Lab
     */
    private void initLabDesc() {
        mLabNames = new ArrayList<>();
        mLabNames.add(TestMovieReaderFragment.mItemDescription);
    }

    private ItemDescription getDescription(Class<? extends BaseFragment> cls) {
        for (ItemDescription itemDescription: mComponentsNames) {
            if (itemDescription.getDemoClass() == cls) {
                return itemDescription;
            }
        }

        for (ItemDescription itemDescription: mLabNames) {
            if (itemDescription.getDemoClass() == cls) {
                return itemDescription;
            }
        }

        for (ItemDescription itemDescription: mUtilNames) {
            if (itemDescription.getDemoClass() == cls) {
                return itemDescription;
            }
        }
        return null;
    }

    public String getName(Class<? extends BaseFragment> cls) {
        ItemDescription itemDescription = getDescription(cls);
        if (itemDescription == null) {
            return null;
        }
        return itemDescription.getName();
    }

    public List<ItemDescription> getComponentsDescriptions() {
        ArrayList<ItemDescription> list = new ArrayList<>();
        list.addAll(mComponentsNames);
        return list;
    }

    public List<ItemDescription> getUtilDescriptions() {
        ArrayList<ItemDescription> list = new ArrayList<>();
        list.addAll(mUtilNames);
        return list;
    }

    public List<ItemDescription> getLabDescriptions() {
        ArrayList<ItemDescription> list = new ArrayList<>();
        list.addAll(mLabNames);
        return list;
    }
}
