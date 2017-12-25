package com.gpuimage.appdemo;


import com.gpuimage.appdemo.base.BaseFragment;
import com.gpuimage.appdemo.fragment.testcase.TestCameraReaderFragment;
import com.gpuimage.appdemo.fragment.testcase.TestCameraV1ReaderFragment;
import com.gpuimage.appdemo.fragment.testcase.TestMovieReaderFragment;
import com.gpuimage.appdemo.fragment.testcase.TestMovieWriterFragment;
import com.gpuimage.appdemo.model.ItemDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author cginechen
 * @date 2016-10-21
 */
public class HomeDataManager {
    private static HomeDataManager _sInstance;
    private HashMap<Class<? extends BaseFragment>, ItemDescription> mComponentsNames;
    private HashMap<Class<? extends BaseFragment>, ItemDescription> mUtilNames;
    private HashMap<Class<? extends BaseFragment>, ItemDescription> mLabNames;

    public HomeDataManager() {
        initComponentsDesc();
        initUtilDesc();
        initLabDesc();
    }

    public static HomeDataManager getInstance() {
        if (_sInstance == null) {
            _sInstance = new HomeDataManager();
        }
        return _sInstance;
    }

    /**
     * Components
     */
    private void initComponentsDesc() {
        mComponentsNames = new HashMap<>();
        mComponentsNames.put(TestMovieReaderFragment.class, TestMovieReaderFragment.mItemDescription);
        mComponentsNames.put(TestMovieWriterFragment.class, TestMovieWriterFragment.mItemDescription);
        mComponentsNames.put(TestCameraReaderFragment.class, TestCameraReaderFragment.mItemDescription);
        mComponentsNames.put(TestCameraV1ReaderFragment.class, TestCameraV1ReaderFragment.mItemDescription);
    }

    /**
     * Helper
     */
    private void initUtilDesc() {
        mUtilNames = new HashMap<>();
        mUtilNames.put(TestMovieReaderFragment.class, TestMovieReaderFragment.mItemDescription);
    }

    /**
     * Lab
     */
    private void initLabDesc() {
        mLabNames = new HashMap<>();
        mLabNames.put(TestMovieReaderFragment.class, TestMovieReaderFragment.mItemDescription);
    }

    public ItemDescription getDescription(Class<? extends BaseFragment> cls) {
        ItemDescription itemDescription = null;
        if ((itemDescription = mComponentsNames.get(cls)) != null) {
            return itemDescription;
        }
        if ((itemDescription = mLabNames.get(cls)) != null) {
            return itemDescription;
        }
        if ((itemDescription = mUtilNames.get(cls)) != null) {
            return itemDescription;
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
        List<ItemDescription> list = new ArrayList<>();
        for (Map.Entry entry : mComponentsNames.entrySet()) {
            list.add((ItemDescription) entry.getValue());
        }
        return list;
    }

    public List<ItemDescription> getUtilDescriptions() {
        List<ItemDescription> list = new ArrayList<>();
        for (Map.Entry entry : mUtilNames.entrySet()) {
            list.add((ItemDescription) entry.getValue());
        }
        return list;
    }

    public List<ItemDescription> getLabDescriptions() {
        List<ItemDescription> list = new ArrayList<>();
        for (Map.Entry entry : mLabNames.entrySet()) {
            list.add((ItemDescription) entry.getValue());
        }
        return list;
    }
}
